/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.parser;

import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.pojo.DynamicRouterContainer;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.RouteItem;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.utils.JsonUtils;
import io.spider.utils.StringHelper;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class RouteParser {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	public static void parse(Element rootElement) {
		Element eleRoutes = rootElement.element(SpiderConfigName.ELE_ROUTE_ITEMS);
		/**
		 * 1.0.6 RC版本修改为默认启用
		 */
		if(StringHelper.isEqual(eleRoutes.attributeValue(SpiderConfigName.ATTR_CONSISTENT),"false")) {
			GlobalConfig.consistent = false;
		}
		List<Element> routes = eleRoutes.elements(SpiderConfigName.ELE_ROUTE_ITEM);
		for(Element route : routes) {
			parseInternal(route);
		}
		if(GlobalConfig.consistent) {
			Collections.sort(GlobalConfig.routeItems, new Comparator<RouteItem>() {
				@Override
				public int compare(RouteItem o1, RouteItem o2) {
					return o1.compareTo(o2);
				}
			});
		}
		logger.info("路由表: " + JsonUtils.toJson(GlobalConfig.routeItems));
	}

	public static void parseInternal(Element route) {
		RouteItem routeItem = new RouteItem();
		routeItem.setCompanyId(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_COMPANY_ID),"*"));
		routeItem.setServiceId(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_SERVICE_ID),"*"));
		routeItem.setSubSystemId(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_SUB_SYSTEM_ID),"*"));
		routeItem.setSystemId(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_SYSTEM_ID),"*"));
		routeItem.setAppVersion(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_APP_VERSION),"*"));
		routeItem.setBatch(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_BATCH),"*"));
		routeItem.setHis(StringHelper.ifEmpty(route.attributeValue(SpiderConfigName.ATTR_HIS),"*"));
		routeItem.setClusterName(route.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME));
		setRouteInfo(routeItem);
	}

	private static void setRouteInfo(RouteItem routeItem) {
		String[] serviceIds = StringUtils.tokenizeToStringArray(routeItem.getServiceId(), SpiderOtherMetaConstant.CONFIG_SEPARATOR);
		String[] subSystemIds = StringUtils.tokenizeToStringArray(routeItem.getSubSystemId(), SpiderOtherMetaConstant.CONFIG_SEPARATOR);
		String[] systemIds = StringUtils.tokenizeToStringArray(routeItem.getSystemId(), SpiderOtherMetaConstant.CONFIG_SEPARATOR);
		String[] appVersions = StringUtils.tokenizeToStringArray(routeItem.getAppVersion(), SpiderOtherMetaConstant.CONFIG_SEPARATOR);
		String[] companyIds = StringUtils.tokenizeToStringArray(routeItem.getCompanyId(), SpiderOtherMetaConstant.CONFIG_SEPARATOR);
		for(int i=0;i<serviceIds.length;i++) {
			for(int j=0;j<subSystemIds.length;j++) {
				for(int k=0;k<systemIds.length;k++) {
					for(int m=0;m<appVersions.length;m++) {
						for(int n=0;n<companyIds.length;n++) {
							RouteItem tmpRouteItem = new RouteItem();
							tmpRouteItem.setCompanyId(companyIds[n]);
							tmpRouteItem.setAppVersion(appVersions[m]);
							tmpRouteItem.setServiceId(serviceIds[i]);
							tmpRouteItem.setSubSystemId(subSystemIds[j]);
							tmpRouteItem.setSystemId(systemIds[k]);
							tmpRouteItem.setBatch(routeItem.getBatch());
							tmpRouteItem.setHis(routeItem.getHis());
							tmpRouteItem.setClusterName(routeItem.getClusterName());
							GlobalConfig.routeItems.add(tmpRouteItem);
							if(!(appVersions[m].equals("*") && companyIds[n].equals("*") && systemIds[k].equals("*"))) {
								GlobalConfig.isDynamicRouteEnable = true;
							}
							if(appVersions[m].equals("*") && companyIds[n].equals("*") && systemIds[k].equals("*")) {
								setRouteInfo(serviceIds[i],subSystemIds[j],routeItem.getHis(),routeItem.getBatch(),routeItem.getClusterName());
							}
						}
					}
				}
			}
		}
	}
	
	private static void setRouteInfo(String serviceId, String subSystemId,String his,String batch, String clusterName) {
		for (Entry<String, ServiceDefinition> entry : ServiceDefinitionContainer.getAllService().entrySet()) {
			if(entry.getValue().getClusterName() != null) {
				continue;
			}
			
			if (Pattern.matches(serviceId.replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), entry.getKey().trim())
					&& Pattern.matches(subSystemId.replace("*", "[0-9]+").replace("?", "[0-9]"), entry.getValue().getSubSystemId())
					&& Pattern.matches(his.replace("*", "[\\s\\S]").replace("?", "[\\s\\S]"), entry.getValue().isHis() ? "Y" : "N")
					&& Pattern.matches(batch.replace("*", "[\\s\\S]").replace("?", "[\\s\\S]"), entry.getValue().isBatch() ? "Y" : "N")){
				entry.getValue().setClusterName(clusterName);
			}
		}
	}
	
	public static void calcDynamicRoute(SpiderPacketHead packetHead, ServiceDefinition serviceDef) {
		String clusterName = DynamicRouterContainer.getRoute(packetHead.getRouteInfo(serviceDef.getSubSystemId(),serviceDef.getServiceId()));
		if(clusterName != null) {
			serviceDef.setClusterName(clusterName);
		} else {
			if(GlobalConfig.dev) {
				logger.info(packetHead.toString());
				logger.info(serviceDef.toString());
			}
			for(RouteItem routeItem : GlobalConfig.routeItems) {
				if(GlobalConfig.dev) {
					logger.info(routeItem.toString());
				}
				if (Pattern.matches(routeItem.getServiceId().replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), serviceDef.getServiceId().trim())
						&& Pattern.matches(routeItem.getCompanyId().replace("*", "\\S+").replace("?", "\\S"), packetHead.getCompanyId().trim() == null ? "*" : packetHead.getCompanyId().trim())
						&& Pattern.matches(routeItem.getAppVersion().replace("*", "\\S+").replace("?", "\\S"), packetHead.getAppVersion().trim() == null ? "*" : packetHead.getAppVersion().trim())
						&& Pattern.matches(routeItem.getSubSystemId().replace("*", "[0-9]+").replace("?", "[0-9]"), serviceDef.getSubSystemId())
						&& Pattern.matches(routeItem.getSystemId().replace("*", "\\S+").replace("?", "\\S"),  packetHead.getSystemId().trim() == null ? "*" : packetHead.getSystemId().trim())
						&& Pattern.matches(routeItem.getHis().replace("*", "[\\s\\S]").replace("?", "[\\s\\S]"), serviceDef.isHis() ? "Y" : "N")
						&& Pattern.matches(routeItem.getBatch().replace("*", "[\\s\\S]").replace("?", "[\\s\\S]"), serviceDef.isBatch() ? "Y" : "N")){
					logger.info("缓存中没有请求" + packetHead.toString() + "的动态路由条目,本次重新计算匹配到动态路由定义[" + routeItem.toString() + "]");
					serviceDef.setClusterName(routeItem.getClusterName());
					DynamicRouterContainer.setRoute(packetHead.getRouteInfo(serviceDef.getSubSystemId(),serviceDef.getServiceId()),routeItem.getClusterName());
					break;
				}
			}
			if(StringUtils.isEmpty(serviceDef.getClusterName())) {
				logger.warn("没有匹配到动态路由定义且缓存中不存在动态路由条目,使用默认静态路由定义[" + ServiceDefinitionContainer.getServiceDefaultClusterName(serviceDef.getServiceId()) + "]!");
				serviceDef.setClusterName(ServiceDefinitionContainer.getServiceDefaultClusterName(serviceDef.getServiceId()));
			}
		}
	}
}
