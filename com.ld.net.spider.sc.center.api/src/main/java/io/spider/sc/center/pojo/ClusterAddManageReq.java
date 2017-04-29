/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import java.util.List;

import com.ld.net.spider.sc.pojo.ClusterReq;
import com.ld.net.spider.sc.pojo.WorkNodeReq;

public class ClusterAddManageReq {
	private List<WorkNodeReq> targetNodes;
	private ClusterReq obj;
	public List<WorkNodeReq> getTargetNodes() {
		return targetNodes;
	}
	public void setTargetNodes(List<WorkNodeReq> targetNodes) {
		this.targetNodes = targetNodes;
	}
	public ClusterReq getObj() {
		return obj;
	}
	public void setObj(ClusterReq obj) {
		this.obj = obj;
	}
}
