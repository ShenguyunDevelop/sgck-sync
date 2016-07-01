package com.sgck.sync.stub;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.sgck.common.log.DSLogger;
import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.SyncTask;
import com.sgck.sync.config.SyncServiceConfig;
import com.sgck.sync.exception.AddUpperInvokeTaskException;
import com.sgck.sync.exception.AddUpperServerException;
import com.sgck.sync.exception.AddUpperSyncTaskException;
import com.sgck.sync.exception.StartServerException;
import com.sgck.sync.support.InvokeCallback;
import com.sgck.sync.support.ServerInfo;
import com.sgck.sync.support.UpperServerInfoListProvider;
import com.sgck.sync.upper.UpperServer;

import flex.messaging.io.amf.ASObject;

/**
 * 默认的上级服务代理类
 * @author yuan
 * 2015-9-12下午6:42:41
 */
public class DefaultUpperServerStub implements ApplicationContextAware,UpperServerStub{
	private Map<String,UpperServer> upperServerMap = new ConcurrentHashMap<String,UpperServer>();
	private ApplicationContext applicationContext;
	
	@Autowired
	private SyncServiceConfig config;
//	private boolean isReady = false;
	
	public void init(){
//		if(config == null){
//			return;
//		}
//		
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				UpperServerInfoListProvider upperServerInfoProvider = config.getUpperServerInfoProvider();
//				if(upperServerInfoProvider != null){
//					List<ServerInfo> servInfoList = upperServerInfoProvider.getUpperServerInfoList();
//					if(servInfoList != null){
//						for(ServerInfo serverInfo : servInfoList){
//							UpperServer upper = new UpperServer();
//							upper.setServerInfo(serverInfo);
//							upper.setApplicationContext(applicationContext);
//							upper.setSyncConfig(config);
//							try {
//								upper.start();
//							} catch (StartServerException e) {
//								DSLogger.error("failed to start upper: ",e);
//								continue;
//							}
//							upperServerMap.put(upper.getServerId(), upper);
//						}
//					}
//				}
//				isReady = true;
//			}
//		}).start();
	}
	
	/**
	 * 添加一个上级服务器
	 * @param sinfo  上级服务器信息，必须提供serverId和url
	 */
	@Override
	public void addUpperServer(ServerInfo sinfo,String subSystemName) throws AddUpperServerException{
		String serverId = sinfo.getServerId();
		if(!serverId.startsWith(subSystemName)){
			serverId = subSystemName + "_" + serverId;
			sinfo.setServerId(serverId);
		}
		
		if(upperServerMap.containsKey(sinfo.getServerId())){
			throw new AddUpperServerException("duplicated upper server: " + sinfo.getServerId());
		}
		
		UpperServer upper = new UpperServer();
		upper.setServerInfo(sinfo);
		upper.setApplicationContext(applicationContext);
		upper.setSyncConfig(config);
		upper.setSubSystemName(subSystemName);
		try {
			upper.start();
		} catch (StartServerException e) {
			throw new AddUpperServerException("failed to start upper",e);
		}
		
		upperServerMap.put(upper.getServerId(), upper);
	}
	
	/**
	 * 为一个业务子系统批量添加多个上级服务器
	 * @param sinfoList	上级服务器信息列表
	 * @param subSystemName 业务子系统名称
	 * @throws AddUpperServerException
	 */
	@Override
	public void addUpperServer(List<ServerInfo> sinfoList,String subSystemName) throws AddUpperServerException{
		List<String> brokenUpperIds = new ArrayList<String>();
		
		for(ServerInfo sinfo : sinfoList){
			String serverId = sinfo.getServerId();
			if(!serverId.startsWith(subSystemName)){
				serverId = subSystemName + "_" + serverId;
				sinfo.setServerId(serverId);
			}
			
			if(upperServerMap.containsKey(sinfo.getServerId())){
				brokenUpperIds.add(sinfo.getServerId()+ "[duplicated]");
				continue;
			}
			
			UpperServer upper = new UpperServer();
			upper.setServerInfo(sinfo);
			upper.setApplicationContext(applicationContext);
			upper.setSyncConfig(config);
			try {
				upper.start();
			} catch (StartServerException e) {
				brokenUpperIds.add(sinfo.getServerId()+ "[starterror]");
				continue;
			}
			
			upperServerMap.put(upper.getServerId(), upper);
		}
	}
	
	
	/**
	 * 向上级服务器发起一个远程调用请求
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 因为指定了回调函数，故不能用sync函数替代，sync函数不支持回调。
	 * @param invokeRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @param callback  回调类，用于任务处理结果
	 * @throws AddUpperInvokeTaskException
	 */
	@Override
	public void invoke(String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddUpperInvokeTaskException{
		if(!isReady()){
			throw new AddUpperInvokeTaskException("upperServerStub isn't ready now?!");
		}
		
		boolean haveMatchedSubSystem = false;
		
		List<String> brokenUpperUrls = new ArrayList<String>();
		for(UpperServer upper : upperServerMap.values()){
			if(upper.getServerId().startsWith(subSystemName)){
				if(!upper.addInvokeTask(new InvokeTask(invokeRPCObj,callback))){
					brokenUpperUrls.add(upper.getServerInfo().getUrl());
				}
				haveMatchedSubSystem = true;
			}
		}
		
		if(!haveMatchedSubSystem){
			//throw new AddUpperInvokeTaskException("no subsystem[" + subSystemName + "],did you not invoke addUpperServer for it?");
		}
		
		if(!brokenUpperUrls.isEmpty()){
			AddUpperInvokeTaskException exception = new AddUpperInvokeTaskException();
			exception.setBrokenUpperUrls(brokenUpperUrls);
			throw exception;
		}
	}

	/**
	 * 向上级服务器发起一个远程调用请求，不关心调用结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 如果需要保证任务被执行（即使服务器重启），请用sync函数
	 * @param invokeRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @throws AddUpperInvokeTaskException
	 */
	@Override
	public void invoke(String subSystemName,ASObject invokeRPCObj) throws AddUpperInvokeTaskException {
		invoke(subSystemName,invokeRPCObj,null);
	}
	
	/**
	 * 向下级服务器发送一个同步任务，此任务对象会存磁盘，所以可以保证任务执行（即使服务器重启）；
	 * 因为此任务对象不占用内存空间，所以此任务队列可以不做作大小限制；
	 * 但为防止意外情况发生，暂定此任务队列的最大长度为10000，超过此大小，会抛出 AddLowerSyncTaskException异常.
	 * 同步任务不能有回调函数，因为此类型的任务要存磁盘，而回调类不应该序列化到磁盘中
	 * 如果需要使用回调函数，请使用invoke函数，此函数有两个不足是：
	 * 1、任务存在内存中，服务器重启后，任务会丢失
	 * 2、任务队列的大小有限（考虑到内存占用），可以在config的upperTaskListSize字段中指定。默认是100
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @throws AddUpperSyncTaskException
	 */
	@Override
	public void sync(String subSystemName,ASObject syncRPCObj) throws AddUpperSyncTaskException {
		if(!isReady()){
			throw new AddUpperSyncTaskException("upperServerStub isn't ready now?!");
		}
		
		boolean haveMatchedSubSystem = false;
		
		List<String> brokenUpperUrls = new ArrayList<String>();
		for(UpperServer upper : upperServerMap.values() ){
			if(upper.getServerId().startsWith(subSystemName)){
				if(!upper.addSyncTask(new SyncTask(syncRPCObj))){
					brokenUpperUrls.add(upper.getServerInfo().getUrl());
				}
				haveMatchedSubSystem = true;
			}
		}
		
		if(!haveMatchedSubSystem){
			//throw new AddUpperSyncTaskException("no subsystem[" + subSystemName + "],did you not invoke addUpperServer for it?");
		}
		
		if(!brokenUpperUrls.isEmpty()){
			AddUpperSyncTaskException exception = new AddUpperSyncTaskException();
			exception.setBrokenUpperUrls(brokenUpperUrls);
			throw exception;
		}
	}
	
	@Override
	@Deprecated
	public void invoke(String upperServerId,String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddUpperInvokeTaskException{
		if(!isReady()){
			throw new AddUpperInvokeTaskException("upperServerStub isn't ready now?!");
		}
		
		if(!upperServerId.startsWith(subSystemName)){
			upperServerId = subSystemName + "_" + upperServerId;
		}
		
		UpperServer upper = upperServerMap.get(upperServerId);
		if(upper == null){
			throw new AddUpperInvokeTaskException("given upper server[" + upperServerId + "] isn't existed");
		}
		
		if(!upper.addInvokeTask(new InvokeTask(invokeRPCObj,callback))){
			AddUpperInvokeTaskException exception = new AddUpperInvokeTaskException();
			List<String> brokenUpperUrls = new ArrayList<String>();
			brokenUpperUrls.add(upper.getServerInfo().getUrl());
			exception.setBrokenUpperUrls(brokenUpperUrls);
			throw exception;
		}
	}
	
	@Override
	@Deprecated
	public void invoke(String upperServerId,String subSystemName,ASObject invokeRPCObj) throws AddUpperInvokeTaskException{
		invoke(upperServerId,subSystemName,invokeRPCObj,null);
	}
	
	@Override
	@Deprecated
	public void sync(String upperServerId,String subSystemName,ASObject syncRPCObj) throws AddUpperSyncTaskException{
		if(!isReady()){
			throw new AddUpperSyncTaskException("upperServerStub isn't ready now?!");
		}
		
		if(!upperServerId.startsWith(subSystemName)){
			upperServerId = subSystemName + "_" + upperServerId;
		}
		
		UpperServer upper = upperServerMap.get(upperServerId);
		if(upper == null){
			throw new AddUpperSyncTaskException("given upper server[" + upperServerId + "] isn't existed");
		}
		
		if(!upper.addSyncTask(new SyncTask(syncRPCObj))){
			AddUpperSyncTaskException exception = new AddUpperSyncTaskException();
			List<String> brokenUpperUrls = new ArrayList<String>();
			brokenUpperUrls.add(upper.getServerInfo().getUrl());
			exception.setBrokenUpperUrls(brokenUpperUrls);
			throw exception;
		}
	}
	
	/**
	 * 与指定的上级服务器断开，用于本机服务器同上级服务器断开
	 * @param upperServerId 上级服务器id
	 */
	@Override
	public void removeUpperServer(String upperServerId,String subSystemName) {
		UpperServer droppedServer = upperServerMap.remove(subSystemName + "_" + upperServerId);
		if(droppedServer != null){
			droppedServer.drop();
		}
	}
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public boolean isReady() {
		return true;
	}
}
