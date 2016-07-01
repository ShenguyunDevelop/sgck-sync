package com.sgck.sync.stub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.SyncTask;
import com.sgck.sync.config.SyncServiceConfig;
import com.sgck.sync.exception.AddLowerInvokeTaskException;
import com.sgck.sync.exception.AddLowerSyncTaskException;
import com.sgck.sync.exception.StartServerException;
import com.sgck.sync.lower.LowerServer;
import com.sgck.sync.support.ServerInfo;
import com.sgck.sync.support.InvokeCallback;

import flex.messaging.io.amf.ASObject;

/**
 * 默认的下级服务器代理类
 * @author yuan
 * 2015-9-12下午6:43:26
 */
public class DefaultLowerServerStub implements LowerServerStub{
	@Autowired
	private SyncServiceConfig config;
	private Map<String,LowerServer> lowerServerMap = new ConcurrentHashMap<String, LowerServer>(16);
	
	public void init(){
		
	}
	
	public LowerServer getLowerServer(String serverId,String subSystemName){
		LowerServer lowerServer = null;
		String key = subSystemName + "_" + serverId;
		if((lowerServer = lowerServerMap.get(key)) == null){
			synchronized (lowerServerMap) {
				if((lowerServer = lowerServerMap.get(key)) == null){
					lowerServer = new LowerServer(key);
					lowerServer.setSyncConfig(config);
					try {
						lowerServer.start();
						lowerServerMap.put(key, lowerServer);
					} catch (StartServerException e) {
						return null;
					}
				}
			}
		}
		return lowerServer;
	}
	
	public LowerServer removeLowerServer(String serverId,String subSystemName){
		String key = subSystemName + "_" + serverId;
		return lowerServerMap.remove(key);
	}
	
	/** 向下级服务器发送一个任务，需要知道任务处理结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 因为指定了回调函数，故不能用sync函数替代，sync函数不支持回调。
	 * @param lowerServerId 下级服务器唯一编码 
	 * @param invokeRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @param callback 回调类，用于接收任务处理结果，可以为空
	 */
	@Override
	public void invoke(String lowerServerId,String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddLowerInvokeTaskException{
		LowerServer lowerServer = getLowerServer(lowerServerId,subSystemName);
		if(lowerServer != null){
			lowerServer.addInvokeTask(new InvokeTask(invokeRPCObj,callback));
		}else{
			throw new AddLowerInvokeTaskException("can't find lowerserver by " + lowerServerId + ":" + subSystemName);
		}
	}

	/** 向下级服务器发送一个任务，不关心任务处理结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 如果需要保证任务被执行（即使服务器重启），请用sync函数
	 * @param lowerServerId 下级服务器唯一编码
	 * @param invokeRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 */
	@Override
	public void invoke(String lowerServerId,String subSystemName, ASObject invokeRPCObj)
			throws AddLowerInvokeTaskException {
		invoke(lowerServerId,invokeRPCObj,null);
	}
	
	/**
	 * 向下级服务器发送一个同步任务，此任务对象会存磁盘，所以可以保证任务执行（即使服务器重启）；
	 * 因为此任务对象不占用内存空间，所以此任务队列可以不做作大小限制；
	 * 但为防止意外情况发生，暂定此任务队列的最大长度为10000，超过此大小，会抛出 AddLowerSyncTaskException异常.
	 * 同步任务不能有回调函数，因为此类型的任务要存磁盘，而回调类不应该序列化到磁盘中
	 * 如果需要使用回调函数，请使用invoke函数，此函数有两个不足是：
	 * 1、任务存在内存中，服务器重启后，任务会丢失
	 * 2、任务队列的大小有限（考虑到内存占用），可以在config的lowerTaskListSize字段中指定。默认是100
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @throws AddLowerSyncTaskException
	 */
	@Override
	public void sync(String lowerServerId,String subSystemName, ASObject syncRPCObj)
			throws AddLowerSyncTaskException {
		LowerServer lowerServer = getLowerServer(lowerServerId,subSystemName);
		if(lowerServer != null){
			lowerServer.addSyncTask(new SyncTask(syncRPCObj));
		}else{
			throw new AddLowerSyncTaskException("can't find lowerserver by " + lowerServerId + ":" + subSystemName);
		}
	}
	
	@Override
	public void invoke(String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddLowerInvokeTaskException{
//		Collection<LowerServer> serverList = lowerServerMap.values();
//		for(LowerServer lowerServer : serverList){
//			lowerServer.addInvokeTask(new InvokeTask(invokeRPCObj,callback));
//		}
		Set<String> serverKeys = lowerServerMap.keySet();
		LowerServer lowerServer = null;
		for(String serverKey : serverKeys){
			if(serverKey.startsWith(subSystemName)){
				if((lowerServer = lowerServerMap.get(serverKey)) != null){
					lowerServer.addInvokeTask(new InvokeTask(invokeRPCObj,callback));
				}
			}
		}
	}
	
	@Override
	public void invoke(String subSystemName,ASObject invokeRPCObj) throws AddLowerInvokeTaskException{
		invoke(subSystemName,invokeRPCObj,null);
	}
	
	@Override
	public void sync(String subSystemName,ASObject syncRPCObj) throws AddLowerSyncTaskException{
		/*Collection<LowerServer> serverList = lowerServerMap.values();
		for(LowerServer lowerServer : serverList){
			lowerServer.addSyncTask(new SyncTask(syncRPCObj));
		}*/
		Set<String> serverKeys = lowerServerMap.keySet();
		LowerServer lowerServer = null;
		for(String serverKey : serverKeys){
			if(serverKey.startsWith(subSystemName)){
				if((lowerServer = lowerServerMap.get(serverKey)) != null){
					lowerServer.addSyncTask(new SyncTask(syncRPCObj));
				}
			}
		}
	}
	
	public List<ServerInfo> getLowerServerInfo(String subSystemName) {
		List<ServerInfo> serverInfoList = new ArrayList<ServerInfo>();
		for (String key : lowerServerMap.keySet()) {
			if (!key.startsWith(subSystemName)) {
				continue;
			}
			LowerServer lowerServer = lowerServerMap.get(key);
			serverInfoList.add(lowerServer.getServerInfo());
		}

		return serverInfoList;
	}
}
