/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import io.spider.stat.ServiceStat;

public class InternalServiceStat extends ServiceStat {
	private String clientIp;
	private int clientPort;
	private String clusterName;
	
	public String getClientIp() {
		return clientIp;
	}
	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}
	public int getClientPort() {
		return clientPort;
	}
	public void setClientPort(int clientPort) {
		this.clientPort = clientPort;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
}
