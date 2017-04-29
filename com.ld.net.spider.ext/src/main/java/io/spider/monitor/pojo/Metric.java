/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.monitor.pojo;

import io.spider.pojo.GlobalConfig;
import io.spider.server.SpiderServerDispatcher;
import io.spider.utils.DateUtils;

import java.util.Date;

public class Metric implements Comparable<Metric> {
	private String appName = GlobalConfig.clusterName;
	private String host = SpiderServerDispatcher.localAddress;
	private String snapTime = DateUtils.SDF_DATETIME_NUM.format(new Date());
	
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	private String serviceId;
	private long count;
	private int totalElapsedTime;
	private int avgElapsedMilliTime;
	private int minElapsedMilliTime;
	private int maxElapsedMilliTime;
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public long getCount() {
		return count;
	}
	public void setCount(long count) {
		this.count = count;
	}
	public int getAvgElapsedMilliTime() {
		return avgElapsedMilliTime;
	}
	public void setAvgElapsedMilliTime(int avgElapsedMilliTime) {
		this.avgElapsedMilliTime = avgElapsedMilliTime;
	}
	public int getMinElapsedMilliTime() {
		return minElapsedMilliTime;
	}
	public void setMinElapsedMilliTime(int minElapsedMilliTime) {
		this.minElapsedMilliTime = minElapsedMilliTime;
	}
	public int getMaxElapsedMilliTime() {
		return maxElapsedMilliTime;
	}
	public void setMaxElapsedMilliTime(int maxElapsedMilliTime) {
		this.maxElapsedMilliTime = maxElapsedMilliTime;
	}
	public int getTotalElapsedTime() {
		return totalElapsedTime;
	}
	public void setTotalElapsedTime(int totalElapsedTime) {
		this.totalElapsedTime = totalElapsedTime;
	}
	@Override
	public String toString() {
		return "Metric [serviceId=" + serviceId + ", count=" + count
				+ ", totalElapsed(S)=" + totalElapsedTime
				+ ", avgElapsed(MS)=" + avgElapsedMilliTime
				+ ", minElapsed(MS)=" + minElapsedMilliTime
				+ ", maxElapsed(MS)=" + maxElapsedMilliTime + "]";
	}
	public String getSnapTime() {
		return snapTime;
	}
	public void setSnapTime(String snapTime) {
		this.snapTime = snapTime;
	}
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Metric o) {
		if(this.totalElapsedTime > o.totalElapsedTime) {
			return 1;
		}
		return this.serviceId.compareTo(o.serviceId);
	}
}
