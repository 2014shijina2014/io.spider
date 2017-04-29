/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketPosConstant;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ld.core.pack.LDConvert;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ServiceDefinition {
	private String serviceId;
	
	private boolean needLog = false;
	private short broadcast = 0;
	private boolean his = false;
	private boolean batch = false;
	private short bizUserType = 1;
	
	public boolean isBatch() {
		return batch;
	}
	public ServiceDefinition setBatch(boolean batch) {
		this.batch = batch;
		return this;
	}
	public short getBizUserType() {
		return bizUserType;
	}
	public void setBizUserType(short bizUserType) {
		this.bizUserType = bizUserType;
	}
	private boolean isExport;
	public boolean isExport() {
		return isExport;
	}
	public ServiceDefinition setExport(boolean isExport) {
		this.isExport = isExport;
		return this;
	}
	private String desc;
	private int timeout = 0;
	private Class clz;
	private Method method;
	private Type retType;
	public String getSubSystemId() {
		return subSystemId;
	}
	public ServiceDefinition setSubSystemId(String subSystemId) {
		this.subSystemId = subSystemId;
		return this;
	}
	public Parameter[] paramTypes;
	private String clusterName; //启动完成后目标路由节点未被设置的话，代表该服务需要依赖于运行时信息动态计算，一般在spider作为NB或者PNP运行时为空
	private String subSystemId;
	private Map<String,Method> ld2FieldNameMap = new HashMap<String,Method>();
	
	/**
	 * 拷贝原LDPack定义
	 */
	public List<LDConvert> paramConverts;
	public List<String> paramNames;
	public boolean isList;
	public List<String> itemNames;
	public List<Method> itemGetters;
	public Class itemClass;
	
	public String getServiceId() {
		return serviceId;
	}
	public ServiceDefinition setServiceId(String serviceId) {
		this.serviceId = StringUtils.rightPad(serviceId,SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		return this;
	}
	public String getDesc() {
		return desc;
	}
	public ServiceDefinition setDesc(String desc) {
		this.desc = desc;
		return this;
	}
	public int getTimeout() {
		return timeout;
	}
	public ServiceDefinition setTimeout(int timeout) {
		this.timeout = timeout;
		return this;
	}
	public String getClusterName() {
		return clusterName;
	}
	public ServiceDefinition setClusterName(String nodeName) {
		this.clusterName = nodeName;
		return this;
	}
	public Class getClz() {
		return clz;
	}
	public ServiceDefinition setClz(Class clz) {
		this.clz = clz;
		return this;
	}
	public Method getMethod() {
		return method;
	}
	public ServiceDefinition setMethod(Method method) {
		this.method = method;
		return this;
	}
	public Type getRetType() {
		return retType;
	}
	public ServiceDefinition setRetType(Type retType) {
		this.retType = retType;
		return this;
	}
	public Parameter getParamType() {
		return (paramTypes != null && paramTypes.length > 0) ? paramTypes[0] : null;
	}
	public ServiceDefinition setParamTypes(Parameter[] paramTypes) {
		this.paramTypes = paramTypes;
		return this;
	}
	public Parameter[] getParamTypes() {
		return this.paramTypes;
	}
	
	public List<String> getDisplayParamTypes() {
		List<String> typeNames = new ArrayList<String>();
		for(int i=0;i<this.paramTypes.length;i++) {
			typeNames.add(paramTypes[i].getType().getCanonicalName());
		}
		return typeNames;
	}
	
	
	@Override
	public String toString() {
		return "ServiceDefinition [serviceId=" + serviceId + ", needLog="
				+ needLog + ", broadcast=" + broadcast + ", isExport="
				+ isExport + ", desc=" + desc + ", timeout=" + timeout
				+ ", clz=" + clz + ", method=" + method + ", retType="
				+ retType + ", paramTypes=" + Arrays.toString(paramTypes)
				+ ", clusterName=" + clusterName + ", subSystemId="
				+ subSystemId + ", paramConverts=" + paramConverts
				+ ", paramNames=" + paramNames + ", isList=" + isList
				+ ", itemNames=" + itemNames + ", itemGetters=" + itemGetters
				+ ", itemClass=" + itemClass + ", ld2FieldNameMap="
				+ ld2FieldNameMap + "]";
	}

	@JsonIgnore
	public Method getSettByLdName(String name) {
		return ld2FieldNameMap.get(name);
	}
	
	public ServiceDefinition putFieldByLdName(String name,Method field) {
		ld2FieldNameMap.put(name, field);
		return this;
	}
	public boolean isNeedLog() {
		return needLog;
	}
	public ServiceDefinition setNeedLog(boolean needLog) {
		this.needLog = needLog;
		return this;
	}
	public short getBroadcast() {
		return broadcast;
	}
	public ServiceDefinition setBroadcast(short broadcast) {
		this.broadcast = broadcast;
		return this;
	}
	public boolean isHis() {
		return his;
	}
	public ServiceDefinition setHis(boolean his) {
		this.his = his;
		return this;
	}
	/**
	 * @param name
	 */
	public int getParamIndex(String name) {
		for(int i=0;i<this.paramNames.size();i++) {
			if (this.paramNames.get(i).equals(name)) {
				return i;
			}
		}
		return -1;
	}
}
