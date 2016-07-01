package com.sgck.sync.upper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.context.ApplicationContext;

import com.sgck.common.log.DSLogger;
import com.sgck.common.utils.FileOperateUtils;
import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.core.rpc.client.Stub;
import com.sgck.core.rpc.server.InvokeFeedback;
import com.sgck.sync.AMFSerializer;
import com.sgck.sync.MapDBManager;
import com.sgck.sync.Server;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.SyncTask;
import com.sgck.sync.config.DefaultSyncServiceConfig;
import com.sgck.sync.config.SyncServiceConfig;
import com.sgck.sync.exception.StartServerException;
import com.sgck.sync.handler.IncomingRequestHandler;
import com.sgck.sync.support.ServerInfo;

import flex.messaging.io.amf.ASObject;

/**
 * @author yuan
 * 2015-9-1下午2:04:43
 */
public class UpperServer implements Server{
	private ServerInfo serverInfo;
	private SyncServiceConfig syncConfig;
	
	private int invokeTaskListSize = 100;
	private SortedMap<Long,InvokeTask> invokeTaskList;
	private SortedMap<Long,SyncTask> syncTaskList;
	
	private String localServerId = null;
	private ApplicationContext applicationContext;
	
	private MapDBManager dbManager;
	private AtomicLong taskIdGenerator = null;
	
	private UpperServerPushThread pushSyncThread;
	private UpperServerPushThread pushInovkeThread;
	private UpperServerPullThread pullSyncThread;
	private UpperServerPullThread pullInovkeThread;
	
	private String serverId = null;
	private String workPath = null;
	
	private String subSystemName = null;
	
	public UpperServer(){
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void start() throws StartServerException{
		if(syncConfig == null){
			throw new StartServerException("Failed to start UpperServer,syncConfig is null?!");
		}
		
		this.invokeTaskListSize = syncConfig.getUpperTaskListSize();
		this.localServerId = syncConfig.getLocalServerInfoProvider().getLocalServerInfo().getServerId();
		
		if(this.localServerId == null){
			throw new StartServerException("Failed to start UpperServer,localServerId is null?!");
		}
		
		if(this.serverInfo == null){
			throw new StartServerException("Failed to start UpperServer,serverInfo is null?!");
		}
		
		if(this.serverId == null){
			throw new StartServerException("Failed to start UpperServer,serverId is not seted ?!");
		}
		
		/**
		 * 当上级服务器down掉的时候，下级最多允许堆积5000个同步任务对象。
		 */
		if(invokeTaskListSize <= 0 || invokeTaskListSize > 5000){
			this.invokeTaskListSize = 100;
		}
		
		this.invokeTaskList = Collections.synchronizedSortedMap(new TreeMap<Long,InvokeTask>());
		
		try {
			workPath = syncConfig.getSyncWorkDir();
			if(workPath == null || workPath.isEmpty()){
				workPath = DefaultSyncServiceConfig.DEFAULT_WOKR_DIR;
			}
			workPath += File.separator;
			
			String dbFilePath = workPath + "/data/upper/" + serverId + "/tasklist";
			dbManager = new MapDBManager(dbFilePath);
			this.syncTaskList = dbManager.getTreeMap("syncTaskSortedMap",new AMFSerializer<SyncTask>());
		} catch (IOException e) {
			throw new StartServerException("Failed to start UpperServer,can't create db file for syncTaskList!",e);
		}
		
		if(!this.syncTaskList.isEmpty()){
			taskIdGenerator = new AtomicLong(syncTaskList.lastKey());
		}else{
			taskIdGenerator = new AtomicLong();
		}
		
		/*
		 * 启动工作线程
		 */
		this.pushSyncThread = new UpperServerPushThread(this,UpperServerSyncThread.SYNC_TASK);
		this.pushInovkeThread = new UpperServerPushThread(this,UpperServerSyncThread.INVOKE_TASK);
		this.pushSyncThread.start();
		this.pushInovkeThread.start();
		
		this.pullSyncThread = new UpperServerPullThread(this,UpperServerSyncThread.SYNC_TASK);
		this.pullInovkeThread = new UpperServerPullThread(this,UpperServerSyncThread.INVOKE_TASK);
		this.pullSyncThread.start();
		this.pullInovkeThread.start();
	}
	
	@Override
	public void terminate(){
		this.pushSyncThread.stop();
		this.pushInovkeThread.stop();
		this.pullSyncThread.stop();
		this.pullInovkeThread.stop();
		if(this.dbManager != null){
			this.dbManager.commit();
			this.dbManager.close();
		}
	}

	@Override
	public void drop(){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					pushSyncThread.shutdown(true);
					pushInovkeThread.shutdown(true);
					pullSyncThread.shutdown(true);
					pullInovkeThread.shutdown(true);
					if(dbManager != null){
						dbManager.commit();
						dbManager.close();
					}
				} catch (Exception e) {
				}
				
				FileOperateUtils.deleteDirectory(workPath + "/data/upper/" + serverId);
				
				ASObject clientInfo = new ASObject();
				clientInfo.put("serverId", localServerId);
				clientInfo.put("subSystemName",subSystemName);
				Stub rpc = new Stub(getServerInfo().getUrl(),clientInfo);
				try {
					rpc.CallDirectEx(IncomingRequestHandler.class.getSimpleName(), "DropLower", new Object[]{});
				} catch (Exception e) {
				}
			}
		}).start();
	}
	
	public ServerInfo getServerInfo() {
		return serverInfo;
	}

	public void setServerInfo(ServerInfo serverInfo) {
		this.serverInfo = serverInfo;
		this.serverId = serverInfo.getServerId();
	}

	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	public SyncServiceConfig getSyncConfig() {
		return syncConfig;
	}

	public void setSyncConfig(SyncServiceConfig syncConfig) {
		this.syncConfig = syncConfig;
	}

	public String getServerId() {
		return serverId;
	}

	/**
	 * 添加远程调用任务
	 * @param invokeTask
	 * @return
	 */
	public boolean addInvokeTask(InvokeTask invokeTask){
		if(invokeTaskList.size() > invokeTaskListSize){
			return false;
		}
		
		invokeTask.setTaskId(taskIdGenerator.incrementAndGet());
		invokeTaskList.put(invokeTask.getTaskId(),invokeTask);
		return true;
	}
	
	public InvokeTask popInvokeTask(){
		if(invokeTaskList.isEmpty()){
			return null;
		}
		
		return invokeTaskList.remove(invokeTaskList.firstKey());
	}
	
	public InvokeTask peekInvokeTask(){
		if(invokeTaskList.isEmpty()){
			return null;
		}
		return invokeTaskList.get(invokeTaskList.firstKey());
	}
	
	
	/**
	 * 添加远程调用任务
	 * @param syncTask
	 * @return
	 */
	public boolean addSyncTask(SyncTask syncTask){
		if(syncTaskList.size() > 10000){
			return false;
		}
		
		syncTask.setTaskId(taskIdGenerator.incrementAndGet());
		syncTaskList.put(syncTask.getTaskId(),syncTask);
		dbManager.commit();
		return true;
	}
	
	public SyncTask popSyncTask(){
		if(syncTaskList.isEmpty()){
			return null;
		}
		
		SyncTask task = syncTaskList.remove(syncTaskList.firstKey());
		dbManager.commit();
		return task;
	}
	
	public SyncTask peekSyncTask(){
		if(syncTaskList.isEmpty()){
			return null;
		}
		return syncTaskList.get(syncTaskList.firstKey());
	}

	public String getSubSystemName() {
		return subSystemName;
	}

	public void setSubSystemName(String subSystemName) {
		this.subSystemName = subSystemName;
	}
}
