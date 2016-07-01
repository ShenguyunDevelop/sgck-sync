package com.sgck.sync.upper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;

import com.sgck.common.log.DSLogger;
import com.sgck.common.utils.DateUtil;
import com.sgck.core.platform.SubSystemGather;
import com.sgck.core.rpc.server.InvokeError;
import com.sgck.core.rpc.server.InvokeFeedback;
import com.sgck.core.rpc.server.InvokeResult;
import com.sgck.sync.InvokeTaskResult;
import com.sgck.sync.SyncTask;
import com.sgck.sync.handler.AmfInvokor;

/**
 * 
 * @author yuan 2015-9-2上午8:51:12
 */
public class UpperServerPullThread extends UpperServerSyncThread {
	private int heartBeatInterval = 3;
	// 上次与上级服务器通讯的时间点
	private long lastPullTimepoint = 0;
	
	private String getTaskListFuncName;
	private String updateTaskFuncName;
	
	private Map<String,AmfInvokor> subSystemInvokeMap = new ConcurrentHashMap<String,AmfInvokor>();
	
	public UpperServerPullThread(UpperServer upperServer,String taskPullType) {
		super(upperServer,taskPullType);
	}

	public void init(){
		super.init();
		try {
			heartBeatInterval = upperServer.getSyncConfig().getSyncHeartBeatInterval();
			if(heartBeatInterval == 0){
				heartBeatInterval = 3;
			}
			heartBeatInterval *= 1000; // 转换为毫秒
			
			getTaskListFuncName = taskType.equals(INVOKE_TASK) ? "GetInvokeTaskList" : "GetSyncTaskList";
			updateTaskFuncName = taskType.equals(INVOKE_TASK) ? "UpdateInvokeTask" : "UpdateSyncTask";
		} catch (Exception e) {
			DSLogger.error(this.getServiceName() + "[" + INVOKE_TASK + "] init ",e);
		}
	}
	
	public void run() {
		DSLogger.info(this.getServiceName() + " service started");
		
		/*
		 * 线程开始工作前需要做一些初始化工作
		 */
		init();
		
//		Set<String> subSystemsSet = subSystemGather != null ? subSystemGather.getSubSystemNames() : null;
//		List<String> subSystemsList = null;
//		if(subSystemsSet != null){
//			subSystemsList = Arrays.asList(subSystemsSet.toArray(new String[0]));
//		}
		 
		while (!this.isStoped()) {
			try {
				InvokeFeedback result = null;
				List<SyncTask> rpcTaskList = null;
				long now =  DateUtil.getCurrentDate().getTime();
				if((lastPullTimepoint != 0) && (now - lastPullTimepoint < heartBeatInterval)){
					Thread.sleep(heartBeatInterval - (now - lastPullTimepoint));
				}
				
				lastPullTimepoint = now;
				
				try {
					result = (InvokeFeedback)rpc.CallDirectEx(upperDomainName, getTaskListFuncName, new Object[]{});
				} catch (Exception e) {
					DSLogger.error(this.getServiceName() + "[" + taskType + "]",e);
				}
				
				if(result != null){
					int code = (Integer) result.getCode();
					if (code == 0){
						rpcTaskList = (List<SyncTask>)((InvokeResult)result).getRet();
					}else{
						DSLogger.error(this.getServiceName() + "[" + taskType + "] failed to getSyncTaskList "+ ((InvokeError)result).getWhat());
					}
				}
				
				if(rpcTaskList != null && !rpcTaskList.isEmpty()){
					handleRequestFromUpperServer(rpcTaskList);
				}
			} catch (InterruptedException e) {
				DSLogger.error(this.getServiceName() + "[" + taskType + "] ", e);
				try {Thread.sleep(1000);
				} catch (InterruptedException e1) {}
			} catch (Exception e) {
				DSLogger.error(this.getServiceName() + "[" + taskType + "] ", e);
				try {Thread.sleep(1000);
				} catch (InterruptedException e1) {}
			} finally{
				resetClientInfo();
			}
		}
		
		DSLogger.info(this.getServiceName() + "[" + taskType + "] service end");
	}

	private void handleRequestFromUpperServer(List<SyncTask> taskList){
		if (taskList == null || taskList.isEmpty()) {
			return;
		}

		AmfInvokor amfInvokor = subSystemInvokeMap.get(subSystemName);
		if (amfInvokor == null) {
			Object context = subSystemGather.getSubSystemContext(subSystemName);
			if (context == null) {
				DSLogger.error("handleRequestFromUpperServer: can't find applicationContext for "
						+ subSystemName);
				return;
			}
			amfInvokor = new AmfInvokor((ApplicationContext) context);
			subSystemInvokeMap.put(subSystemName, amfInvokor);
		}

		List<InvokeTaskResult> invokeResultList = new ArrayList<InvokeTaskResult>();
		for (SyncTask task : taskList) {
			try {
				invokeResultList.add(amfInvokor.handleRequest(task));
			} catch (Exception e) {
				DSLogger.error(
						"handleRequestFromUpperServer: amfInvokor error ", e);
			}
		}

		if (invokeResultList.isEmpty()) {
			return;
		}

		// 将远程调用的结果反馈给上级服务器
		try {
			rpc.ClearCalls();
			for (InvokeTaskResult invokeResult : invokeResultList) {
				rpc.AddCall(upperDomainName, updateTaskFuncName, null,
						new Object[] { invokeResult});
			}
			rpc.Commit();
		} catch (Exception e) {
			DSLogger.error(
					"handleRequestFromUpperServer: batch feedback error ", e);
		} finally {
			resetClientInfo();
		}
	}
	
	@Override
	public String getServiceName() {
		return this.getClass().getSimpleName();
	}
}
