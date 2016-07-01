package com.sgck.sync.support;

import java.util.List;


/**
 * 用于提供上级服务器的信息列表
 * @author yuan
 * 2015-9-1下午2:02:03
 */
@Deprecated
public interface UpperServerInfoListProvider {
	public List<ServerInfo> getUpperServerInfoList();
}
