package com.sgck.sync;

/**
 * 
 * @author yuan
 * 2015-9-10下午1:40:45
 */
public class InvokeTaskResult {
	public static int TASK_RESULT_OK = 0;
	public static int TASK_RESULT_ERROR = 1;
	
	private long taskId;
	/*
	 * 任务处理结果，
	 * 0：任务处理成功；
	 * 非0：任务处理失败
	 */
	private int code;
	
	/*
	 * 任务处理结果
	 * 任务处理成功时为 任务处理结果
	 * 任务处理失败时为错误描述(String)
	 */
	private Object result;

	/**
	 * @return the taskId
	 */
	public long getTaskId() {
		return taskId;
	}

	/**
	 * @param taskId the taskId to set
	 */
	public void setTaskId(long taskId) {
		this.taskId = taskId;
	}

	/**
	 * @return the code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(int code) {
		this.code = code;
	}

	/**
	 * @return the result
	 */
	public Object getResult() {
		return result;
	}

	/**
	 * @param result the result to set
	 */
	public void setResult(Object result) {
		this.result = result;
	}
}
