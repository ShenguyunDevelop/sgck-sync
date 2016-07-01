package com.sgck.sync.handler;

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.ApplicationContext;

import com.sgck.common.log.DSLogger;
import com.sgck.core.exception.DSException;
import com.sgck.core.rpc.server.amf.AmfRPCHandler;
import com.sgck.sync.InvokeTask;
import com.sgck.sync.InvokeTaskResult;
import com.sgck.sync.SyncTask;

import flex.messaging.io.amf.ASObject;

/**
 * 此类用于下级服务器解析处理上级下发的远程调用任务，
 * 此类依赖Spring容器
 * @author yuan
 * 2015-9-12下午6:38:43
 */
public class AmfInvokor extends AmfRPCHandler {
	private ApplicationContext applicationContext = null;
	static private ConcurrentHashMap<String, String> canonicalNameMap = new ConcurrentHashMap<String, String>();
	
	public AmfInvokor(ApplicationContext context){
		applicationContext = context;
	}
	
	protected String canonicalName(String handlerDomainName){
		if(handlerDomainName == null || handlerDomainName.length() <= 1){
			return handlerDomainName;
		}
		
		String canonicalName = null;
		if((canonicalName = (String)canonicalNameMap.get(handlerDomainName)) != null){
			return canonicalName;
		}
		
		canonicalName = Introspector.decapitalize(handlerDomainName);
		canonicalNameMap.put(handlerDomainName, canonicalName);
		return canonicalName;
	}
	
	@Override
	protected Object doRPC(String className,String funcName,ArrayList<Object> paramList) throws Exception{
		if(applicationContext == null){
			DSLogger.error("hanldeClientReq fatal error,Spring applicationContext is null?!");
			throw new DSException(1100, "fatal error,Spring applicationContext is null?!");
		}
		
		Object object = applicationContext.getBean(canonicalName(className));
		Class<?> clazz = object.getClass();
		Method func = getMethodByName(clazz, funcName);
		if (null == func)
		{
			throw new DSException(1100, "funcName[" + funcName + "] is not exist!");
		}
		
		Class[] paramTypeList = func.getParameterTypes();
		if (paramList != null && paramList.size() != paramTypeList.length)
		{
			throw new DSException(1101, "func need " + paramTypeList.length + "params, argument count is mismatch");
		}

		if (paramList != null)
		{
			for (int index = 0; index < paramList.size(); index++)
			{
				paramList.set(index, typeConvert(paramList.get(index), paramTypeList[index]));
			}
		}
		
		return func.invoke(object, paramList != null ? paramList.toArray() : null);
	}
	
	public InvokeTaskResult handleRequest(SyncTask task){
		InvokeTaskResult result = new InvokeTaskResult();
		result.setTaskId(task.getTaskId());
		String className = null;
		String funcName = null;
		ArrayList paramList = null;
		try
		{
			ASObject rpcObj;
			if ((rpcObj = task.getRpcObj()) != null)
			{
				className = (String) rpcObj.get("domain");
				funcName = (String) rpcObj.get("foo").toString();
				paramList = (ArrayList<?>) rpcObj.get("params");
				result.setCode(0);
				result.setResult(doRPC(className,funcName,paramList));
			}
		}
		catch (Exception e)
		{   
			result.setCode(1010);
			result.setResult(className + "." + funcName + " error : " + e.getMessage());
		}finally{
		}
		
		return result;
	}
}