/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.stat;

public class ServiceStat {
	private String serviceId;
	private long execCount = 0;
	private long elapsedMsTime = 0; // 1/1000秒,该精度会根据业务和jvm稳定性情况随时发生调整
	private long maxMsTime = 0;
	private long minMsTime = 0;
	
	public ServiceStat() {}
	
	public ServiceStat(String serviceId) {
		this.serviceId = serviceId;
	}
	public String getServiceId() {
		return serviceId;
	}
	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
	public long getExecCount() {
		return execCount;
	}
	public void setExecCount(long execCount) {
		this.execCount = execCount;
	}
	public long getElapsedMsTime() {
		return elapsedMsTime;
	}                                                                                                                                                              
	public void setElapsedMsTime(long elapsedMsTime) {
		this.elapsedMsTime = elapsedMsTime;
	}
	public long getMaxMsTime() {
		return maxMsTime;
	}
	public void setMaxMsTime(long maxMsTime) {
		this.maxMsTime = maxMsTime;
	}
	public long getMinMsTime() {
		return minMsTime;
	}
	public void setMinMsTime(long minMsTime) {
		this.minMsTime = minMsTime;
	}
	/**
	 * 这里已经根据线程级别进行统计了, 所以不需要进行同步保护, 这样可以大幅度的提升并发性能
	 * @param elapsedMsTime
	 */
	public void autoInc(long elapsedMsTime) {
		this.execCount++;
		this.elapsedMsTime += elapsedMsTime;
		this.maxMsTime = Math.max(this.maxMsTime, elapsedMsTime);
		// 最小值要剔除 0 默认值
		this.minMsTime = Math.min(this.minMsTime == 0 ? elapsedMsTime : this.minMsTime, elapsedMsTime);
	}
	public void addServiceStat(ServiceStat another) {
		this.execCount += another.getExecCount();
		this.elapsedMsTime += another.getElapsedMsTime();
		this.maxMsTime = Math.max(this.maxMsTime, another.getMaxMsTime());
		// 最小值要剔除 0 默认值
		this.minMsTime = Math.min(this.minMsTime == 0 ? another.getMinMsTime() : this.minMsTime, another.getMinMsTime());
	}
}
