package com.sgck.sync.upper;

import java.util.ArrayList;

import com.sgck.common.log.DSLogger;
import com.sgck.core.rpc.server.InvokeError;
import com.sgck.core.rpc.server.InvokeFeedback;
import com.sgck.core.rpc.server.InvokeResult;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.SyncTask;
import com.sgck.sync.support.InvokeCallback;

import flex.messaging.io.amf.ASObject;

public class UpperServerPushThread extends UpperServerSyncThread {
	public UpperServerPushThread(UpperServer upperServer,String taskPullType) {
		super(upperServer,taskPullType);
	}
	
	public void run() {
		DSLogger.info(this.getServiceName() + " service started");

		/*
		 * 线程开始工作前需要做一些初始化工作
		 */
		init();
		
		while (!this.isStoped()) {
			try {
				if(taskType.equals(INVOKE_TASK)){
					doInvoke();
				}else{
					doSync();
				}
			} catch (Exception e) {
				DSLogger.error(this.getServiceName() + "[" + taskType + "]", e);
				try {Thread.sleep(1000);
				} catch (InterruptedException e1) {}
			} finally{
				resetClientInfo();
			}
		}

		DSLogger.info(this.getServiceName() + "[" + taskType + "] service end");

	}
	
	private void doInvoke(){
		InvokeTask rpcObjWrapper = upperServer.peekInvokeTask();
		if(rpcObjWrapper == null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return;
		}
		
		ASObject rpcObj = rpcObjWrapper.getRpcObj();
		InvokeCallback callback = rpcObjWrapper.getCallback();
		
		String domain = (String)rpcObj.get("domain");
		String func = (String)rpcObj.get("foo");
		ArrayList paramList = (ArrayList)rpcObj.get("params");
		InvokeFeedback result = null;
		try {
			result = (InvokeFeedback)rpc.CallDirectEx(domain, func, paramList != null ? paramList.toArray() : new Object[0]);
		} catch (Exception e) {
			if(callback != null){
				callback.onError(e.getMessage());
			}
			// 如果是网络异常导致调用失败的话，应该稍等一会儿重试此任务
			DSLogger.error(this.getServiceName() + "[" + taskType + "]",e);
			// 一秒钟之后重试
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
			return; 
		}
		
		if(result != null && callback != null){
			if (result.getCode() == 0){
				callback.onOK(((InvokeResult)result).getRet());
			}else{
				callback.onError(((InvokeError)result).getWhat());
			}
		}
		
		upperServer.popInvokeTask();
	}
	
	private void doSync(){
		SyncTask rpcObjWrapper = upperServer.peekSyncTask();
		if(rpcObjWrapper == null){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			return;
		}
		
		ASObject rpcObj = rpcObjWrapper.getRpcObj();
		String domain = (String)rpcObj.get("domain");
		String func = (String)rpcObj.get("foo");
		ArrayList paramList = (ArrayList)rpcObj.get("params");
		InvokeFeedback result = null;
		try {
			result = (InvokeFeedback)rpc.CallDirectEx(domain, func, paramList != null ? paramList.toArray() : new Object[0]);
		} catch (Exception e) {
			// 如果是网络异常导致调用失败的话，应该稍等一会儿重试此任务
			DSLogger.error(this.getServiceName() + "[" + taskType + "]",e);
			
			// 一秒钟之后重试
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
			return;
		}
		
		upperServer.popSyncTask();
	}
	
	@Override
	public String getServiceName() {
		return this.getClass().getSimpleName();
	}
}