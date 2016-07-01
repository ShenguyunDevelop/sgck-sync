package com.sgck.sync;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

/**
 * 基于文件系统的map,set,queue的管理类
 * @author yuan
 * 2015-9-16上午8:50:42
 */
public class MapDBManager{
	private String dbFileName;
	private transient DB db = null;

	public MapDBManager(String dbFileName) {
		this.dbFileName = dbFileName;
	}

	public BlockingQueue getQueue(String name) throws IOException{
		return getDB().getQueue(name);
	}

	public Set getHashSet(String name) throws IOException{
		return getDB().hashSet(name);
	}
	
	public NavigableSet getTreeSet(String name) throws IOException{
		return getDB().treeSet(name);
	}
	
	/**
	 * 获取具有排序功能的map
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public ConcurrentNavigableMap getTreeMap(String name) throws IOException{
		return getDB().treeMap(name);
	}
	
	public ConcurrentNavigableMap getTreeMap(String name,Serializer valueSerializer) throws IOException{
		DB db = getDB();
		return db.treeMap(name,db.getDefaultSerializer(),valueSerializer);
	}
	
	
	/**
	 * 获取没有排序功能的map
	 * @param name
	 * @return
	 * @throws IOException
	 */
	public ConcurrentMap getHashMap(String name) throws IOException{
		return getDB().hashMap(name);
	}
	
	public void commit(){
		if(db != null){
			db.commit();
		}
	}
	
	public void close(){
		if(db != null){
			db.close();
		}
	}
	
	public String getFileName(){
		return dbFileName;
	}
	
	public synchronized DB getDB() throws IOException {
		if(db != null){
			return db;
		}
		
		File dbFile = new File(dbFileName);
		if (!dbFile.exists()) {
			while (!dbFile.getParentFile().exists()) {
				dbFile.getParentFile().mkdirs();
			}
			dbFile.createNewFile();
		}

		db = DBMaker.fileDB(dbFile).closeOnJvmShutdown()
				// .encryptionEnable("password")
				.make();
		return db;
	}
}
