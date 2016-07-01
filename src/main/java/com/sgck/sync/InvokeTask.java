package com.sgck.sync;

import java.util.UUID;

import com.sgck.sync.support.InvokeCallback;

import flex.messaging.io.amf.ASObject;

/**
 * 上下级服务器 远程调用任务对象
 * 此业务对象不能不能持久化
 * @author yuan
 * 2015-9-12下午6:34:03
 */
public class InvokeTask extends SyncTask{
	private InvokeCallback callback;
	public InvokeTask(ASObject rpcObj,InvokeCallback callback){
		super(rpcObj);
		this.callback = callback;
	}
	
	public InvokeTask(ASObject rpcObj){
		super(rpcObj);
	}
	
	public InvokeTask(){
		
	}
	
	public InvokeCallback getCallback() {
		return callback;
	}
	
	public SyncTask clone(){
		SyncTask clonedObj = new SyncTask();
		clonedObj.setRpcObj(getRpcObj());
		clonedObj.setTaskId(getTaskId());
		return clonedObj;
	}
}
