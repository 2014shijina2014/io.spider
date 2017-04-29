/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import java.util.List;

import com.ld.net.spider.sc.pojo.WorkNodeReq;

public class ClusterRemoveManageReq {
	private List<WorkNodeReq> targetNodes;
	private String clusterName;
	public List<WorkNodeReq> getTargetNodes() {
		return targetNodes;
	}
	public void setTargetNodes(List<WorkNodeReq> targetNodes) {
		this.targetNodes = targetNodes;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
}
