package com.sgck.sync.lower;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.atomic.AtomicLong;

import com.sgck.common.log.DSLogger;
import com.sgck.common.utils.FileOperateUtils;
import com.sgck.core.cache.CacheService;
import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.sync.AMFSerializer;
import com.sgck.sync.MapDBManager;
import com.sgck.sync.Server;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.InvokeTaskResult;
import com.sgck.sync.SyncTask;
import com.sgck.sync.TaskStatus;
import com.sgck.sync.config.DefaultSyncServiceConfig;
import com.sgck.sync.config.SyncServiceConfig;
import com.sgck.sync.exception.AddLowerInvokeTaskException;
import com.sgck.sync.exception.AddLowerSyncTaskException;
import com.sgck.sync.exception.StartServerException;
import com.sgck.sync.handler.IncomingRequestHandler;
import com.sgck.sync.support.ServerInfo;
import com.sgck.sync.support.InvokeCallback;

/**
 * @author yuan
 * 2015-9-1下午2:04:37
 */
public class LowerServer implements Server{
	private ServerInfo serverInfo = null;
	/*
	 * 下级服务器的连接时间
	 */
	private long connTime;
	private long LastCommTime;
	
	private SortedMap<Long,InvokeTask> invokeTaskList;
	private SortedMap<Long,SyncTask> syncTaskList;
	
	private int syncTaskListSize = 100;
//	private CacheService<InvokeTask> taskCache;
	
	private SyncServiceConfig syncConfig;
	
	private String serverId;
	private String workPath = null;
	
	private AtomicLong taskIdGenerator = null;
	private MapDBManager dbManager = null;
	
	/**
	 * 
	 * @param syncTaskListSize 同步任务队列的大小
	 */
	public LowerServer(String serverId){
		this.serverId = serverId;
//		this.taskCache = new CacheService<InvokeTask>(5*60); // 5分钟后会自动过期
	}

	@Override
	public void start() throws StartServerException{
		if(syncConfig == null){
			throw new StartServerException("Failed to start UpperServer,syncConfig is null?!");
		}
		
		this.syncTaskListSize = syncConfig.getLowerTaskListSize();
		/**
		 * 当下级服务器不在线时，上级最多允许堆积500个同步任务对象。
		 */
		if(syncTaskListSize <= 0 ){
			this.syncTaskListSize = 100;
		}
		if(syncTaskListSize > 500){
			syncTaskListSize = 500;
		}
		
		// 创建远程调用任务队列
		this.invokeTaskList = Collections.synchronizedSortedMap(new TreeMap<Long,InvokeTask>());
		
		// 创建上下级同步任务队列
		try {
			workPath = syncConfig.getSyncWorkDir();
			if(workPath == null || workPath.isEmpty()){
				workPath = DefaultSyncServiceConfig.DEFAULT_WOKR_DIR;
			}
			workPath += File.separator;
			
			String dbFilePath = workPath + "/data/lower/" + serverId + "/tasklist";
			dbManager = new MapDBManager(dbFilePath);
			this.syncTaskList = dbManager.getTreeMap("syncTaskSortedMap",new AMFSerializer<SyncTask>());
		} catch (IOException e) {
			DSLogger.error("failed to getSyncTaskList: ",e);
			throw new StartServerException("Failed to create syncTaskSortedMap,",e);
		}
		
		if(!this.syncTaskList.isEmpty()){
			taskIdGenerator = new AtomicLong(syncTaskList.lastKey());
		}else{
			taskIdGenerator = new AtomicLong();
		}
		
		this.connTime = System.currentTimeMillis();
	}

	@Override
	public void terminate() {
		if(dbManager != null){
			dbManager.commit();
			dbManager.close();
		}
	}
	
	@Override
	public void drop(){
		this.terminate();
		FileOperateUtils.deleteDirectory(workPath + "/data/lower/" + serverId);
	}
	
	public ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}
	
	public long getConnTime() {
		return connTime;
	}

	public void setConnTime(long connTime) {
		this.connTime = connTime;
	}

	public void refreshLastCommTime(){
		this.LastCommTime = System.currentTimeMillis();
	}
	
	/**
	 * 为指定下级添加一个远程调用任务，当下级服务器的任务队列满时，会抛 {@code AddLowerSyncTaskException} 异常，
	 * 适用于不关心任务处理结果的场合
	 * @param syncTask 异步task对象
	 * @throws AddLowerInvokeTaskException
	 */
	public void addInvokeTask(InvokeTask invokeTask) throws AddLowerInvokeTaskException{
		try {
			if(invokeTaskList.size() > syncTaskListSize){
				throw new AddLowerInvokeTaskException("invokeTask is full");
			}
			invokeTask.setTaskId(taskIdGenerator.incrementAndGet());
			invokeTaskList.put(invokeTask.getTaskId(),invokeTask);
		} catch (Exception e) {
			throw new AddLowerInvokeTaskException("invokeTask is full or something is wrong",e);
		}
	}
	
	
	/**
	 * 从下级服务器任务队列中取出任务对象。防止一次取出过多造成异常情况发生，
	 * 暂定每次最多返回10条任务
	 * @return
	 */
	public List<SyncTask> peekInvokeTask(){
		refreshLastCommTime();
		
		List<SyncTask> taskList = new ArrayList<SyncTask>();

		/*synchronized(invokeTaskList){
			Collection<InvokeTask> invokeTasks = invokeTaskList.values();
			for (InvokeTask task : invokeTasks) {
				if (task.getStatus() != TaskStatus.PENDING) {
					continue;
				}
				taskList.add(task.clone());
				task.setStatus(TaskStatus.PROCESSING);
				if (taskList.size() > 10) {
					break;
				}
			}
		}*/
		
		Collection<InvokeTask> invokeTasks = invokeTaskList.values();
		for (InvokeTask task : invokeTasks) {
			taskList.add(task.clone());
			if (taskList.size() > 10) {
				break;
			}
		}
		
		/*
		 * 下级服务器的信息没有汇报上来，当 上级服务器重启 后可能会出现这种情况。
		 * 此时，需要通知下级上报其服务器信息
		 * 
		 * 此处没有加锁，可能会上传多次，仅限于上级服务器刚启动的时候，影响可以忽略。
		 */
		if(serverInfo == null){
			InvokeTask getLocalServerInfoTask = new InvokeTask(
					InvokeObjectWrapper.packInvokeObject(IncomingRequestHandler.class.getSimpleName(), "GetLocalServerInfo"),
					new InvokeCallback() {
						public void onOK(Object result) {
							if(result instanceof ServerInfo){
								setServerInfo((ServerInfo)result);
							}else{
								DSLogger.error("failed to get server info from lower server,illegal format?!");
							}
						}
						public void onError(Object error) {
							DSLogger.error("failed to get server info from lower server," + error);
						}
					});
			getLocalServerInfoTask.setTaskId(taskIdGenerator.incrementAndGet());
			taskList.add(getLocalServerInfoTask);
			invokeTaskList.put(getLocalServerInfoTask.getTaskId(), getLocalServerInfoTask);
		}
		
		/*
		 * 将需要获得任务处理结果的异步task保存在缓存中（最多保留5分钟）
		 */
//		if(!taskList.isEmpty()){
//			for(InvokeTask task : taskList){
//				if(task.getCallback() != null){
//					taskCache.put(task.getTaskId(), task);
//				}
//			}
//		}
		
		return taskList;
	}
	
	public void updateInvokeTask(InvokeTaskResult result){
		if(result == null){
			return;
		}
		
//		InvokeTask task = taskCache.getIfPresent(result.getTaskId());
		InvokeTask task = invokeTaskList.get(result.getTaskId());
		InvokeCallback callback = null;
		if(task != null && (callback = task.getCallback()) != null){
			if(result.getCode() == InvokeTaskResult.TASK_RESULT_OK){ // 任务处理成功
				callback.onOK(result.getResult());
			}else{ // 任务处理失败
				callback.onError(result.getResult());
			}
		}
		invokeTaskList.remove(result.getTaskId());
	}

	/**
	 * 为指定下级添加一个同步任务，当下级服务器当前的的任务数超过10000时，会抛 {@code AddLowerSyncTaskException} 异常，
	 * 适用于不关心任务处理结果的场合
	 * @param syncTask 异步task对象
	 * @throws AddLowerInvokeTaskException
	 */
	public void addSyncTask(SyncTask syncTask) throws AddLowerSyncTaskException{
		try {
			if(syncTaskList.size() > 10000){
				throw new AddLowerSyncTaskException("invokeTask is full");
			}
			syncTask.setTaskId(taskIdGenerator.incrementAndGet());
			syncTaskList.put(syncTask.getTaskId(),syncTask);
			dbManager.commit();
		} catch (Exception e) {
			throw new AddLowerSyncTaskException("invokeTask is full or something is wrong",e);
		}
	}
	
	/**
	 * 从下级服务器任务队列中取出任务对象。防止一次取出过多造成异常情况发生，
	 * 暂定每次最多返回10条任务
	 * @return
	 */
	public List<SyncTask> peekSyncTask(){
		refreshLastCommTime();
		
		List<SyncTask> taskList = new ArrayList<SyncTask>();
		/*synchronized (syncTaskList) {
			Collection<SyncTask> syncTasks = syncTaskList.values();
			for (SyncTask task : syncTasks) {
				if (task.getStatus() != TaskStatus.PENDING) {
					continue;
				}
				
				taskList.add(task);
				task.setStatus(TaskStatus.PROCESSING);
				
				if (taskList.size() > 10) {
					break;
				}
			}
		}*/
		
		Collection<SyncTask> syncTasks = syncTaskList.values();
		for (SyncTask task : syncTasks) {
			taskList.add(task);
			if (taskList.size() > 10) {
				break;
			}
		}
		
		return taskList;
	}
	
	public void updateSyncTask(InvokeTaskResult result){
		if(result == null){
			return;
		}
		syncTaskList.remove(result.getTaskId());
		dbManager.commit();
	}
	
	public SyncServiceConfig getSyncConfig() {
		return syncConfig;
	}

	public void setSyncConfig(SyncServiceConfig syncConfig) {
		this.syncConfig = syncConfig;
	}
}
