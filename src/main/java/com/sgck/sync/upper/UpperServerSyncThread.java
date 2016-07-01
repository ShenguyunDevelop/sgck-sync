package com.sgck.sync.upper;

import java.beans.Introspector;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;

import com.sgck.common.ServiceThread;
import com.sgck.common.log.DSLogger;
import com.sgck.common.utils.DateUtil;
import com.sgck.core.exception.DSException;
import com.sgck.core.platform.SubSystemGather;
import com.sgck.core.platform.SubSystemInitilizer;
import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.core.rpc.client.Stub;
import com.sgck.core.rpc.server.InvokeError;
import com.sgck.core.rpc.server.InvokeFeedback;
import com.sgck.core.rpc.server.InvokeResult;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.InvokeTaskResult;
import com.sgck.sync.SyncTask;
import com.sgck.sync.handler.AmfInvokor;
import com.sgck.sync.handler.IncomingRequestHandler;
import com.sgck.sync.support.ServerInfo;
import com.sgck.sync.support.InvokeCallback;

import flex.messaging.io.amf.ASObject;

/**
 * 
 * @author yuan 2015-9-2上午8:51:12
 */
public abstract class UpperServerSyncThread extends ServiceThread {
	public static String SYNC_TASK = "sync";
	public static String INVOKE_TASK = "invoke";
	
	private ASObject clientInfo; // 远程调用专用
	
	protected String upperDomainName = IncomingRequestHandler.class.getSimpleName();
	protected ServerInfo localServerInfo = null;
	protected UpperServer upperServer;
	protected Stub rpc;
	protected String taskType = INVOKE_TASK;
	
	protected SubSystemGather subSystemGather;
	protected String subSystemName;
	
	public UpperServerSyncThread(UpperServer upperServer,String taskPullType) {
		this.upperServer = upperServer;
		this.taskType = taskPullType;
	}

	public void init(){
		try {
			subSystemGather = upperServer.getApplicationContext().getBean(SubSystemGather.class);
			subSystemName = upperServer.getSubSystemName();
			localServerInfo = upperServer.getSyncConfig().getLocalServerInfoProvider().getLocalServerInfo();
			
			clientInfo = new ASObject();
			clientInfo.put("serverId", localServerInfo.getServerId());
			clientInfo.put("subSystemName",subSystemName);
//			ApplicationContext applicationContext = upperServer.getApplicationContext();
//			if(applicationContext != null){
//				Map<String,SubSystemInitilizer> beans = applicationContext.getBeansOfType(SubSystemInitilizer.class);
//				if(!beans.isEmpty()){
//					clientInfo.put("subSystemName",beans.values().toArray(new SubSystemInitilizer[0])[0].getSystemName());
//				}
//			}
			
			// 下级服务器第一次启动时，需要向上级服务器报告其服务器信息
			if(taskType.equals(INVOKE_TASK)){
				upperServer.addInvokeTask(new InvokeTask(
						InvokeObjectWrapper.packInvokeObject(upperDomainName, "UploadServerInfo",localServerInfo)));
			}
			
			rpc = new Stub(upperServer.getServerInfo().getUrl(),clientInfo);
		} catch (Exception e) {
			DSLogger.error("init ",e);
		}
	}
	
	protected void resetClientInfo(){
		if(rpc != null){
			rpc.SetClientInfo(clientInfo);
		}
	}
}
