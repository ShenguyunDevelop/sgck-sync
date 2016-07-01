package com.sgck.sync.config;

import org.springframework.beans.factory.annotation.Autowired;

import com.sgck.sync.support.LocalServerInfoProvider;
import com.sgck.sync.support.UpperServerInfoListProvider;

/**
 * 同步服务配置类，一般使用此默认对象即可。
 * 要使上下级同步服务生效，必须配置此类。
 * @author yuan
 * 2015-9-12下午6:35:10
 */
public class DefaultSyncServiceConfig implements SyncServiceConfig {
	
	public static String DEFAULT_WOKR_DIR = "/admin/sync/";
	
	/*
	 * 上级服务器信息提供者，必须提供
	 */
	private UpperServerInfoListProvider upperServerInfoProvider;
	
	/*
	 * 本机服务器信息提供者，不是必需，如果没有提供，系统会自动生成本级服务器的信息（主要是本机服务器的唯一编码）
	 */
	private LocalServerInfoProvider localServerInfoProvider;
	
	/*
	 * 上下级服务器的心跳包间隔，以秒为单位
	 * 默认为3秒钟
	 */
	private int syncHeartBeatInterval = 3; 

	/*
	 * 同步服务的工作目录，用于存放同步服务需要的配置，需要缓存的数据等
	 */
	private String syncWorkDir = "/admin/sync";
	
	/*
	 * 上级服务器任务发送队列的大小,默认为 100 
	 */
	private int upperTaskListSize;
	
	/*
	 * 下级服务器任务下发队列的大小,默认为 100
	 */
	private int lowerTaskListSize;
	
	@Autowired
	private LocalServerInfoProvider defaultLocalServerInfoProvider;
	
	@Deprecated
	public UpperServerInfoListProvider getUpperServerInfoProvider() {
		return upperServerInfoProvider;
	}

	@Deprecated
	public void setUpperServerInfoProvider(
			UpperServerInfoListProvider upperServerInfoProvider) {
		this.upperServerInfoProvider = upperServerInfoProvider;
	}

	public LocalServerInfoProvider getLocalServerInfoProvider() {
		if(localServerInfoProvider == null){
			return defaultLocalServerInfoProvider;
		}else{
			return localServerInfoProvider;
		}
	}

	public void setLocalServerInfoProvider(
			LocalServerInfoProvider localServerInfoProvider) {
		this.localServerInfoProvider = localServerInfoProvider;
	}

	public int getSyncHeartBeatInterval() {
		return syncHeartBeatInterval;
	}

	public void setSyncHeartBeatInterval(int syncHeartBeatInterval) {
		this.syncHeartBeatInterval = syncHeartBeatInterval;
	}

	public String getSyncWorkDir() {
		return syncWorkDir;
	}

	public void setSyncWorkDir(String syncWorkDir) {
		this.syncWorkDir = syncWorkDir;
	}

	public int getUpperTaskListSize() {
		return upperTaskListSize;
	}

	public void setUpperTaskListSize(int upperTaskListSize) {
		this.upperTaskListSize = upperTaskListSize;
	}

	public int getLowerTaskListSize() {
		return lowerTaskListSize;
	}

	public void setLowerTaskListSize(int lowerTaskListSize) {
		this.lowerTaskListSize = lowerTaskListSize;
	}
}
