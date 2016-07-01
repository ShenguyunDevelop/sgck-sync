package com.sgck.sync;

import flex.messaging.io.amf.ASObject;

/**
 * 上下级服务器 同步任务对象
 * 此任务对象可以持久化到文件系统，
 * 此任务可以保证被执行，即使服务器重启
 * @author yuan
 * 2015-9-12下午6:34:03
 */
public class SyncTask{
	private ASObject rpcObj;
	
	private TaskStatus status = TaskStatus.PENDING;
	/*
	 * taskId是一个自增序列，其是Task对象的主键
	 */
	private long taskId = 0;
	
	public SyncTask(ASObject rpcObj){
		this.rpcObj = rpcObj;
	}
	
	public SyncTask(ASObject rpcObj,long taskId){
		this.rpcObj = rpcObj;
		this.taskId = taskId;
	}
	
	public SyncTask(){
		
	}
	
	public long getTaskId() {
		return taskId;
	}
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}
	public ASObject getRpcObj() {
		return rpcObj;
	}
	public void setRpcObj(ASObject rpcObj) {
		this.rpcObj = rpcObj;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}
}
