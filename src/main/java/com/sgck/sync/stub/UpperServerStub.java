package com.sgck.sync.stub;

import java.util.List;

import com.sgck.core.rpc.client.InvokeObjectWrapper;
import com.sgck.sync.exception.AddUpperInvokeTaskException;
import com.sgck.sync.exception.AddUpperServerException;
import com.sgck.sync.exception.AddUpperSyncTaskException;
import com.sgck.sync.support.InvokeCallback;
import com.sgck.sync.support.ServerInfo;

import flex.messaging.io.amf.ASObject;

public interface UpperServerStub {
	/**
	 * 向所有上级服务器发起一个远程调用请求
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 因为指定了回调函数，故不能用sync函数替代，sync函数不支持回调。
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @param callback  回调类，用于任务处理结果
	 * @throws AddUpperInvokeTaskException
	 */
	public void invoke(String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddUpperInvokeTaskException;
	
	/**
	 * 向所有上级服务器发起一个远程调用请求，不关心调用结果
	 * 此类型的任务对象存在内存中，服务器重启会丢失。
	 * 如果需要保证任务被执行（即使服务器重启），请用sync函数
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @throws AddUpperInvokeTaskException
	 */
	public void invoke(String subSystemName,ASObject invokeRPCObj) throws AddUpperInvokeTaskException;
	
	/**
	 * 向所有上级服务器发送一个同步任务，此任务对象会存磁盘，所以可以保证任务执行（即使服务器重启）；
	 * 因为此任务对象不占用内存空间，所以此任务队列可以不做作大小限制；
	 * 但为防止意外情况发生，暂定此任务队列的最大长度为10000，超过此大小，会抛出 AddLowerSyncTaskException异常.
	 * 同步任务不能有回调函数，因为此类型的任务要存磁盘，而回调类不应该序列化到磁盘中
	 * 如果需要使用回调函数，请使用invoke函数，此函数有两个不足是：
	 * 1、任务存在内存中，服务器重启后，任务会丢失
	 * 2、任务队列的大小有限（考虑到内存占用），可以在config的upperTaskListSize字段中指定。默认是100
	 * @param syncRPCObj ASObject类型的远程调用对象，可以用{@link InvokeObjectWrapper.packInvokeObject} 打包生成
	 * @throws AddUpperSyncTaskException
	 */
	public void sync(String subSystemName,ASObject syncRPCObj) throws AddUpperSyncTaskException;
	
	/**
	 * 用法同上三个函数，唯一的区别是此三个函数是向指定的上级服务器发起远程调用，
	 * 上面三个函数是向所有的上级服务发送远程调用。
	 * @param upperServerId
	 * @param invokeRPCObj
	 * @param callback
	 * @throws AddUpperInvokeTaskException
	 */
	@Deprecated
	public void invoke(String upperServerId,String subSystemName,ASObject invokeRPCObj,InvokeCallback callback) throws AddUpperInvokeTaskException;
	@Deprecated
	public void invoke(String upperServerId,String subSystemName,ASObject invokeRPCObj) throws AddUpperInvokeTaskException;
	@Deprecated
	public void sync(String upperServerId,String subSystemName,ASObject syncRPCObj) throws AddUpperSyncTaskException;
	
	public boolean isReady();
	
	/**
	 * 与指定的上级服务器断开，用于本机服务器同上级服务器断开
	 * @param upperServerId 上级服务器id
	 */
	public void removeUpperServer(String upperServerId,String subSystemName);
	
	/**
	 * 为一个业务子系统添加一个上级服务器
	 * @param sinfo  上级服务器信息，必须提供serverId和url
	 * @param subSystemName 业务子系统名称
	 */
	public void addUpperServer(ServerInfo sinfo,String subSystemName) throws AddUpperServerException;
	
	/**
	 * 为一个业务子系统批量添加多个上级服务器
	 * @param sinfo	上级服务器信息列表
	 * @param subSystemName 业务子系统名称
	 * @throws AddUpperServerException
	 */
	public void addUpperServer(List<ServerInfo> sinfoList,String subSystemName) throws AddUpperServerException;
}
