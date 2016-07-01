package com.sgck.sync.stub;

import java.util.List;

import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.sync.exception.AddLowerInvokeTaskException;
import com.sgck.sync.exception.AddLowerSyncTaskException;
import com.sgck.sync.support.InvokeCallback;
import com.sgck.sync.support.ServerInfo;

import flex.messaging.io.amf.ASObject;

public interface LowerServerStub {
	/** 
	 * 向下级服务器发送一个任务，需要知道任务处理结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 因为指定了回调函数，故不能用sync函数替代，sync函数不支持回调。
	 * @param lowerServerId 下级服务器唯一编码 
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @param callback 回调类，用于接收任务处理结果，可以为空
	 */
	public void invoke(String lowerServerId,String subSystemName,ASObject syncRPCObj,InvokeCallback callback) throws AddLowerInvokeTaskException;
	
	/** 
	 * 向下级服务器发送一个任务，不关心任务处理结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 如果需要保证任务被执行（即使服务器重启），请用sync函数
	 * @param lowerServerId 下级服务器唯一编码
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 */
	public void invoke(String lowerServerId,String subSystemName,ASObject syncRPCObj) throws AddLowerInvokeTaskException;
	
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
	public void sync(String lowerServerId,String subSystemName,ASObject syncRPCObj) throws AddLowerSyncTaskException;
	
	public void invoke(String subSystemName,ASObject syncRPCObj,InvokeCallback callback) throws AddLowerInvokeTaskException;
	public void invoke(String subSystemName,ASObject syncRPCObj) throws AddLowerInvokeTaskException;
	public void sync(String subSystemName,ASObject syncRPCObj) throws AddLowerSyncTaskException;
	
	/**
	 * 获取目前连接至本级服务器的下级服务器信息列表
	 * @return
	 */
	public List<ServerInfo> getLowerServerInfo(String subSystem);
}
