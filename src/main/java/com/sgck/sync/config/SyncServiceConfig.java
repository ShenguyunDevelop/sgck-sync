package com.sgck.sync.config;

import com.sgck.sync.support.LocalServerInfoProvider;
import com.sgck.sync.support.UpperServerInfoListProvider;

public interface SyncServiceConfig {
	/**
	 * 上下级通讯的时间间隔，以秒为单位,默认为 3 秒钟
	 * @return
	 */
	public int getSyncHeartBeatInterval();
	
	/**
	 * 本地服务器信息提供者，可以为空，
	 * 如果为空，使用默认的 {@literal DefaultLocalServerInfoProvider} 
	 * @return
	 */
	public LocalServerInfoProvider getLocalServerInfoProvider();
	
	/**
	 * 上级服务器信息提供者，不能为空
	 * @return
	 */
	@Deprecated
	public UpperServerInfoListProvider getUpperServerInfoProvider();
	
	/**
	 * 用于存放上下级同步信息的目录，默认为/admin/sync/
	 * @return
	 */
	public String getSyncWorkDir();
	
	
	/**
	 * 上级服务器任务发送队列的大小，超过此限制，将抛出 AddUpperSyncTaskException 异常。
	 * @return
	 */
	public int getUpperTaskListSize();
	
	
	/**
	 * 下级服务器任务下发队列的大小，超过此限制，将抛出 AddLowerSyncTaskException 异常。
	 * @return
	 */
	public int getLowerTaskListSize();
}
