/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;

public class MyInfo extends MyInfoMini {
	private int maxQueueCount;
	private int status;
	private String clusterName;
	private boolean isServer;
	private int port;
	private boolean isDynamicRouteEnable;	
	/** 业务处理线程大小*/ 
	private int busiThreadCount;
	private boolean isCloud;
	private boolean isServiceCenter;
	private String serviceCenterInetAddress; //ip:port
	private boolean reliable;
	private boolean reliableStatus; //运行时自动设置
	private boolean ha;
	private String nodeId;
	private String haRemoteServerAddress;
	private int forceRecovery;
	private int pendingTaskCount;
	
	private boolean tcpDump;
	private boolean dumpStat;
	private int slowLongTime;
	
	private int SSHPort = 22;
	private String SSHUsername;
	private String SSHPassword;
	
	public int getSSHPort() {
		return SSHPort;
	}
	public void setSSHPort(int sSHPort) {
		SSHPort = sSHPort;
	}
	public String getSSHUsername() {
		return SSHUsername;
	}
	public void setSSHUsername(String sSHUsername) {
		SSHUsername = sSHUsername;
	}
	public String getSSHPassword() {
		return SSHPassword;
	}
	public void setSSHPassword(String sSHPassword) {
		SSHPassword = sSHPassword;
	}
	public boolean isTcpDump() {
		return tcpDump;
	}
	public void setTcpDump(boolean tcpDump) {
		this.tcpDump = tcpDump;
	}
	public boolean isDumpStat() {
		return dumpStat;
	}
	public void setDumpStat(boolean dumpStat) {
		this.dumpStat = dumpStat;
	}
	public int getSlowLongTime() {
		return slowLongTime;
	}
	public void setSlowLongTime(int slowLongTime) {
		this.slowLongTime = slowLongTime;
	}
	public int getMaxQueueCount() {
		return maxQueueCount;
	}
	public void setMaxQueueCount(int maxQueueCount) {
		this.maxQueueCount = maxQueueCount;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
	public boolean isServer() {
		return isServer;
	}
	public void setServer(boolean isServer) {
		this.isServer = isServer;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public boolean isDynamicRouteEnable() {
		return isDynamicRouteEnable;
	}
	public void setDynamicRouteEnable(boolean isDynamicRouteEnable) {
		this.isDynamicRouteEnable = isDynamicRouteEnable;
	}
	public int getBusiThreadCount() {
		return busiThreadCount;
	}
	public void setBusiThreadCount(int busiThreadCount) {
		this.busiThreadCount = busiThreadCount;
	}
	public boolean isCloud() {
		return isCloud;
	}
	public void setCloud(boolean isCloud) {
		this.isCloud = isCloud;
	}
	public boolean isServiceCenter() {
		return isServiceCenter;
	}
	public void setServiceCenter(boolean isServiceCenter) {
		this.isServiceCenter = isServiceCenter;
	}
	public String getServiceCenterInetAddress() {
		return serviceCenterInetAddress;
	}
	public void setServiceCenterInetAddress(String serviceCenterInetAddress) {
		this.serviceCenterInetAddress = serviceCenterInetAddress;
	}
	public boolean isReliable() {
		return reliable;
	}
	public void setReliable(boolean reliable) {
		this.reliable = reliable;
	}
	public boolean isHa() {
		return ha;
	}
	public void setHa(boolean ha) {
		this.ha = ha;
	}
	public String getNodeId() {
		return nodeId;
	}
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}
	public String getHaRemoteServerAddress() {
		return haRemoteServerAddress;
	}
	public void setHaRemoteServerAddress(String haRemoteServerAddress) {
		this.haRemoteServerAddress = haRemoteServerAddress;
	}
	public int getForceRecovery() {
		return forceRecovery;
	}
	public void setForceRecovery(int forceRecovery) {
		this.forceRecovery = forceRecovery;
	}
	public int getPendingTaskCount() {
		return pendingTaskCount;
	}
	public void setPendingTaskCount(int pendingTaskCount) {
		this.pendingTaskCount = pendingTaskCount;
	}
	public boolean isReliableStatus() {
		return reliableStatus;
	}
	public void setReliableStatus(boolean reliableStatus) {
		this.reliableStatus = reliableStatus;
	}
}
