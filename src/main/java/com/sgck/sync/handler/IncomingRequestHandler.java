package com.sgck.sync.handler;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.sgck.common.log.DSLogger;
import com.sgck.core.rpc.server.RecvHandler;
import com.sgck.sync.InvokeTaskResult;
import com.sgck.sync.SyncTask;
import com.sgck.sync.config.SyncServiceConfig;
import com.sgck.sync.lower.LowerServer;
import com.sgck.sync.stub.DefaultLowerServerStub;
import com.sgck.sync.support.ServerInfo;

/**
 * 此类用于接收下级服务器的远程调用请求
 * @author yuan
 * 2015-9-12下午6:41:01
 */
public class IncomingRequestHandler {
	@Autowired
	private SyncServiceConfig config;
	
	@Autowired(required=false)
	private DefaultLowerServerStub lowerServerStub;
	
	public ServerInfo GetLocalServerInfo(){
		return config.getLocalServerInfoProvider().getLocalServerInfo();
	}
	
	/**
	 * 下级定时获取上级下发给它的任务列表，此接口调用也起到了心跳包的作用
	 * @param serverId
	 * @return
	 */
	public List<SyncTask> GetSyncTaskList(){
		if(lowerServerStub == null){
			DSLogger.error("GetSyncTaskList fatal error: lowerServerStub is null");
			return null;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("GetSyncTaskList fatal error: can't find serverId from clientInfo");
			return null;
		}
		
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("GetSyncTaskList fatal error: can't find subSystemName from clientInfo");
			return null;
		}
		
		LowerServer lowerServer = lowerServerStub.getLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			return lowerServer.peekSyncTask();
		}else{
			return null;
		}
	}
	
	public void UpdateSyncTask(InvokeTaskResult result){
		if(lowerServerStub == null){
			DSLogger.error("UpdateSyncTask fatal error: lowerServerStub is null");
			return;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("UpdateSyncTask fatal error: can't find serverId from clientInfo");
			return;
		}
		
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("GetSyncTaskList fatal error: can't find subSystemName from clientInfo");
			return;
		}
		
		LowerServer lowerServer = lowerServerStub.getLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			lowerServer.updateSyncTask(result);
		}
	}
	
	/**
	 * 下级定时获取上级下发给它的任务列表，此接口调用也起到了心跳包的作用
	 * @param serverId
	 * @return
	 */
	public List<SyncTask> GetInvokeTaskList(){
		if(lowerServerStub == null){
			DSLogger.error("GetInvokeTaskList fatal error: lowerServerStub is null");
			return null;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("GetInvokeTaskList fatal error: can't find serverId from clientInfo");
			return null;
		}
		
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("GetSyncTaskList fatal error: can't find subSystemName from clientInfo");
			return null;
		}
		
		LowerServer lowerServer = lowerServerStub.getLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			return lowerServer.peekInvokeTask();
		}else{
			return null;
		}
	}
	
	public void UpdateInvokeTask(InvokeTaskResult result){
		if(lowerServerStub == null){
			DSLogger.error("UpdateInvokeTask fatal error: lowerServerStub is null");
			return;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("UpdateInvokeTask fatal error: can't find serverId from clientInfo");
			return;
		}
		
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("UpdateInvokeTask fatal error: subSystemName is null");
			return;
		}
		
		LowerServer lowerServer = lowerServerStub.getLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			lowerServer.updateInvokeTask(result);
		}
	}
	
	public void UploadServerInfo(ServerInfo sinfo){
		if(lowerServerStub == null){
			DSLogger.error("UploadServerInfo fatal error: lowerServerStub is null");
			return;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("UploadServerInfo fatal error: can't find serverId from clientInfo");
			return;
		}
		
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("GetSyncTaskList fatal error: can't find subSystemName from clientInfo");
			return;
		}
		
		LowerServer lowerServer = lowerServerStub.getLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			lowerServer.setServerInfo(sinfo);
		}
	}
	
	public void DropLower(){
		if(lowerServerStub == null){
			DSLogger.error("DropLower fatal error: lowerServerStub is null");
			return;
		}
		
		String serverId = (String)RecvHandler.getClientAttribute("serverId");
		if(serverId == null){
			DSLogger.error("DropLower fatal error: can't find serverId from clientInfo");
			return;
		}
		String subSystemName = (String)RecvHandler.getClientAttribute("subSystemName");
		if(subSystemName == null){
			DSLogger.error("DropLower fatal error: subSystemName is null");
			return;
		}
		
		LowerServer lowerServer = lowerServerStub.removeLowerServer(serverId,subSystemName);
		if(lowerServer != null){
			lowerServer.drop();
		}
	}
}
