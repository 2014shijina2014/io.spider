/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import static io.spider.meta.SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderServiceIdConstant;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ServiceDefinitionContainer {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private static final Map<String,ServiceDefinition> services = new TreeMap<String,ServiceDefinition>();
	private static final Map<String,String> methodServiceMap = new HashMap<String,String>();
	
	public static ServiceDefinition getService(String serviceId) {
		serviceId = StringUtils.rightPad(serviceId, SPIDER_SERVICE_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		if(GlobalConfig.isDynamicRouteEnable) {
			return getCloneServiceDef(serviceId);
		} else {
			return services.get(serviceId);
		}
	}
	
	private static ServiceDefinition getCloneServiceDef(String serviceId) {
		serviceId = StringUtils.rightPad(serviceId, SPIDER_SERVICE_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		ServiceDefinition sd = null;
		try {
			if(services.containsKey(serviceId)) {
				sd = new ServiceDefinition();
				ServiceDefinition org = services.get(serviceId);
				sd.setServiceId(org.getServiceId());
				sd.setClz(org.getClz());
				sd.setMethod(org.getMethod());
				sd.setParamTypes(org.getParamTypes());
				sd.setTimeout(org.getTimeout());
				sd.setRetType(org.getRetType());
				sd.setSubSystemId(org.getSubSystemId());
				sd.setExport(org.isExport());
				sd.setDesc(org.getDesc());
			} else if (serviceId.startsWith(SpiderServiceIdConstant.SPIDER_INTERNAL_SERVICE_ID_PREFIX)) {
				//NOP
			} else {
				logger.error("服务号[" + serviceId + "]不是spider内部功能号,且ServiceDefinitionContainer.services中不包含该服务,应该是spider代理服务配置不正确,请检查spider.xml,然后重试！");
				throw new InstantiationException();
			}
			return sd;
		} catch (InstantiationException | IllegalArgumentException e) {
			logger.error("动态路由模式下克隆服务出错,配置正确的情况下不会发生异常！",e);
			throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION,SpiderErrorNoConstant.getErrorInfo(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION),"动态路由模式下获取[" + serviceId + "]ServiceDefinition失败！此异常永远不会发生！");
		}
	}

	public static void addService(String serviceId,ServiceDefinition service) {
		if(serviceId.trim().isEmpty()) {
			logger.error("遇到服务编号为空的服务定义(理论上这不会发生): " + service.toString());
			// logger.info("系统退出!");
			// System.exit(-1);
		}
		serviceId = StringUtils.rightPad(serviceId, SPIDER_SERVICE_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		if (services.containsKey(serviceId)) {
			throw new RuntimeException("spider service id [" + serviceId + "] in " + service.getClz().getCanonicalName() + " is duplicate!");
		}
		services.put(serviceId, service);
		methodServiceMap.put(service.getMethod().toString(), serviceId);
	}
	
	public static ServiceDefinition getServiceByMethodName(String method) {
		if(GlobalConfig.isDynamicRouteEnable) {
			return getCloneServiceDef(methodServiceMap.get(method));
		} else {
			return services.get(methodServiceMap.get(method));
		}
	}

	public static Map<String,ServiceDefinition> getAllService() {
		return services;
	}

	public static String getServiceDefaultClusterName(String serviceId) {
		serviceId = StringUtils.rightPad(serviceId, SPIDER_SERVICE_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		return services.get(serviceId).getClusterName();
	}
}
