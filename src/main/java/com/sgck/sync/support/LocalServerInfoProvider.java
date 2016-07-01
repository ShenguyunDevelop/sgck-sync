package com.sgck.sync.support;


/**
 * 用于提供本机服务器的信息
 * @author yuan
 * 2015-9-1下午2:02:59
 */
public interface LocalServerInfoProvider {
	public ServerInfo getLocalServerInfo();
	public void setLocalServerName(String serverName);
}
