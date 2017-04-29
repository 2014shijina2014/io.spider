/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;

import io.spider.stat.ServiceStat;
import io.spider.utils.DateUtils;

import java.util.Date;
import java.util.List;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class StatReq extends NodeInfo {
	
	public StatReq() {
		super();
	}
	
	private String statTime = DateUtils.SDF_DATETIME.format(new Date());
	
	private List<ServiceStat> serviceStats;

	public StatReq(List<ServiceStat> serviceStats) {
		this.serviceStats = serviceStats;
	}

	public String getStatTime() {
		return statTime;
	}

	public void setStatTime(String statTime) {
		this.statTime = statTime;
	}

	public List<ServiceStat> getServiceStats() {
		return serviceStats;
	}

	public void setServiceStats(List<ServiceStat> serviceStats) {
		this.serviceStats = serviceStats;
	}
}
