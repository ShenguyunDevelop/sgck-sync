package com.sgck.sync.support;

import java.util.UUID;

public class ServerInfo {
	private String serverId;
	private String serverName;
	private String ip;
	private String desc;
	private String url;
	
	public ServerInfo(){
		serverId = UUID.randomUUID().toString();
	}
	public String getServerId() {
		return serverId;
	}
	/*public void setServerId(String serverId) {
		this.serverId = serverId;
	}*/
	public String getServerName() {
		return serverName;
	}
	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @param serverId the serverId to set
	 */
	public void setServerId(String serverId) {
		this.serverId = serverId;
	}
}
