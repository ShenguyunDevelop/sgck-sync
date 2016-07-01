package com.sgck.sync.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;

import com.sgck.common.log.DSLogger;
import com.sgck.sync.config.DefaultSyncServiceConfig;
import com.sgck.sync.config.SyncServiceConfig;

public class DefaultLocalServerInfoProvider implements LocalServerInfoProvider {
	private String workDir = null;
	private ServerInfo serverInfo = null;
	
	@Autowired
	private SyncServiceConfig config;
	
	/**
	 * 
	 * @param workDir  用于存放上下级同步信息的目录，如：/admin/sync/
	 */
	public DefaultLocalServerInfoProvider(String workDir){
		this.workDir = workDir;
	}
	
	public DefaultLocalServerInfoProvider(){
		
	}
	
	public synchronized ServerInfo getLocalServerInfo() {
		if(serverInfo != null){
			return serverInfo;
		}else{
			if(workDir == null || workDir.isEmpty()){
				if(config != null){
					workDir = config.getSyncWorkDir();
				}
			}
			
			if(workDir == null || workDir.isEmpty()){
				workDir = DefaultSyncServiceConfig.DEFAULT_WOKR_DIR;
			}
			
			workDir += "/config";
			String configPath = workDir + "/localServerInfo.ini";
			
			//首先尝试从文件中读取本地服务器信息
			File file = new File(configPath);  
			Properties pro = new Properties();
			if(!file .exists()){
				try {    
					serverInfo =  new ServerInfo();
					serverInfo.setDesc("auto-generated local server info");
					serverInfo.setServerName("sgck-server");
					
					pro.put("serverId", serverInfo.getServerId());
					pro.put("serverName", serverInfo.getServerName());
					pro.put("desc", serverInfo.getDesc());
					
					if(!file.getParentFile().exists() || !file.getParentFile().isDirectory()){
						if(!file.getParentFile().mkdirs()) {
			                DSLogger.error("getLocalServerInfo: cann't create dir: " + file.getParent());
			                return serverInfo;
			            }
					}
					
			        file.createNewFile();
			        
			        FileOutputStream oFile = new FileOutputStream(file);
			        pro.store(oFile, "local server info");
			        oFile.close();
			    } catch (IOException e) {    
			        DSLogger.error("getLocalServerInfo:",e);
			    }    
			}else{
				try {
					FileInputStream in = new FileInputStream(file);
					pro.load(in);
					in.close();
					
					String serverId = pro.getProperty("serverId");
					if(serverId != null){ //有效的配置文件
						serverInfo =  new ServerInfo();
						serverInfo.setServerId(serverId);
						serverInfo.setDesc(pro.getProperty("desc"));
						serverInfo.setIp(pro.getProperty("ip"));
						serverInfo.setServerName(pro.getProperty("serverName"));
						serverInfo.setUrl(pro.getProperty("url"));
					}
					
				} catch (Exception e) {
					DSLogger.error("getLocalServerInfo:",e);
				}finally{
					if(serverInfo == null){
						serverInfo =  new ServerInfo();
						serverInfo.setDesc("auto-generated local server info");
						serverInfo.setServerName("sgck-server");
						
						pro.clear();
						pro.put("serverId", serverInfo.getServerId());
						pro.put("serverName", serverInfo.getServerName());
						pro.put("desc", serverInfo.getDesc());
						
						try {
							FileOutputStream oFile = new FileOutputStream(file);
							pro.store(oFile, "local server info");
							oFile.close();
						} catch (FileNotFoundException e) {
							DSLogger.error("getLocalServerInfo[set default localServerInfo when got broken localServerInfo]:",e);
						} catch (IOException e) {
							DSLogger.error("getLocalServerInfo[set default localServerInfo when got broken localServerInfo]:",e);
						}
					}
				}
			}
		}
		
		return serverInfo;
	}
	
	public void setLocalServerName(String serverName){
		if(serverName == null || serverName.isEmpty()){
			return;
		}
		
		getLocalServerInfo();
		
		if(serverInfo.getServerName().equals(serverName)){
			return;
		}
		
		serverInfo.setServerName(serverName);
		File file = new File(workDir + "/localServerInfo.ini");  
		if(!file .exists()){
			DSLogger.error("setLocalServerName: localServerInfo.ini isn't existed,this should not be happened");
			 try {
				if (!file.getParentFile().exists()
						|| !file.getParentFile().isDirectory()) {
					if (!file.getParentFile().mkdirs()) {
						DSLogger.error("setLocalServerName: cann't create dir: "
								+ file.getParent());
					}
				}
				file.createNewFile();
			} catch (IOException e) {
				DSLogger.error("setLocalServerName: localServerInfo.ini isn't existed,this should not be happened",e);
				return;
			}
		}
		
		Properties pro = new Properties();
		try {
			pro.put("serverId", serverInfo.getServerId());
			if(serverInfo.getServerName() != null){
				pro.put("serverName", serverInfo.getServerName());
			}
			if(serverInfo.getDesc() != null){
				pro.put("desc", serverInfo.getDesc());
			}
			if(serverInfo.getIp() != null){
				pro.put("ip",serverInfo.getIp());
			}
			if(serverInfo.getUrl() != null){
				pro.put("url", serverInfo.getUrl());
			}
	        FileOutputStream oFile = new FileOutputStream(file);
	        pro.store(oFile, "local server info");
	        oFile.close();
	    } catch (IOException e) {   
	        DSLogger.error("setLocalServerName:",e);
	    }    
	}
	
	public static void main(String[] args) {
		String configPath = "D:/test/sync" + "/localServerInfo.ini";
		File file = new File(configPath);  
		
		Properties pro = new Properties();
		if(!file .exists()){
			if(!file.getParentFile().exists() || !file.getParentFile().isDirectory()){
				if(!file.getParentFile().mkdirs()) {
	                return;
	            }
			}
			
	        try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		pro.put("serverName", "yiocio");
		try {
			FileOutputStream oFile = new FileOutputStream(file);
			pro.store(oFile, "local server info");
			oFile.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
