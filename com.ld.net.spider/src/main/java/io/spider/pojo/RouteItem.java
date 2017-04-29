/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderPacketPosConstant;

import org.apache.commons.lang3.StringUtils;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class RouteItem implements Comparable<RouteItem> {
	private String serviceId = "*";
	private String systemId = "*";
	private String subSystemId = "*";
	private String appVersion = "*";
	private String companyId = "*";
	private String clusterName = "*";
	/**
	 * @since 1.0.10
	 */
	private String his = "*";
	private String batch = "*";
	
	
	public RouteItem() {
		super();
	}
	public RouteItem(String serviceId, String systemId, String subSystemId,
			String appVersion, String companyId, String clusterName,String his,String batch) {
		super();
		this.serviceId = serviceId;
		this.systemId = systemId;
		this.subSystemId = subSystemId;
		this.appVersion = appVersion;
		this.companyId = companyId;
		this.clusterName = clusterName;
		this.his = his;
		this.batch = batch;
	}
	
	public String getHis() {
		return his;
	}
	public void setHis(String his) {
		this.his = his;
	}
	public String getBatch() {
		return batch;
	}
	public void setBatch(String batch) {
		this.batch = batch;
	}
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getSubSystemId() {
		return subSystemId;
	}
	public void setSubSystemId(String subSystemId) {
		this.subSystemId = subSystemId;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String version) {
		this.appVersion = version;
	}
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String corpId) {
		this.companyId = corpId;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String nodeName) {
		this.clusterName = nodeName;
	}
	
	@Override
	public String toString() {
		return "RouteItem [serviceId=" + serviceId + ", systemId=" + systemId
				+ ", subSystemId=" + subSystemId + ", appVersion=" + appVersion
				+ ", companyId=" + companyId + ", clusterName=" + clusterName
				+ ", his=" + his + ", batch=" + batch + "]";
	}
	/**
	 * 用于一致性路由
	 */
	@Override
	public int compareTo(RouteItem o) {
		if (this.appVersion.equals("*")) {
			return 1;
		}
		if (o.appVersion.equals("*")) {
			return -1;
		}
		if(this.companyId.equals("*")) {
			return 1;
		}
		if(o.companyId.equals("*")) {
			return -1;
		}
		if(this.systemId.equals("*")) {
			return 1;
		}
		if(o.systemId.equals("*")) {
			return -1;
		}
		if(this.serviceId.indexOf("?") < 0 && this.serviceId.indexOf("*") < 0) {
			return -1;
		}
		if(o.serviceId.indexOf("?") < 0 && o.serviceId.indexOf("*") < 0) {
			return 1;
		}
		return (StringUtils.leftPad(this.appVersion, SpiderPacketPosConstant.SPIDER_APP_VERSION_LEN, ' ') + 
				StringUtils.leftPad(this.systemId, SpiderPacketPosConstant.SPIDER_SYSTEM_ID_LEN, ' ') + 
				StringUtils.leftPad(this.serviceId, SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, ' ') + 
				StringUtils.leftPad(this.companyId, SpiderPacketPosConstant.SPIDER_COMPANY_ID_LEN, ' '))
			   .compareTo(StringUtils.leftPad(o.appVersion, SpiderPacketPosConstant.SPIDER_APP_VERSION_LEN, ' ') + 
						StringUtils.leftPad(o.systemId, SpiderPacketPosConstant.SPIDER_SYSTEM_ID_LEN, ' ') + 
						StringUtils.leftPad(o.serviceId, SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, ' ') + 
						StringUtils.leftPad(o.companyId, SpiderPacketPosConstant.SPIDER_COMPANY_ID_LEN, ' '));
	}
}
