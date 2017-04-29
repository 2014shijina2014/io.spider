/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import java.util.List;

import com.ld.net.spider.sc.pojo.WorkNodeReq;

public class WorkNodeManageReq {
	private List<WorkNodeReq> targetNodes;
	/*将被增加或者删除的节点本身,使用WorkNodeReq纯属为了重用现有刚好完全匹配的POJO*/
	private WorkNodeReq obj;
	public List<WorkNodeReq> getTargetNodes() {
		return targetNodes;
	}
	public void setTargetNodes(List<WorkNodeReq> targetNodes) {
		this.targetNodes = targetNodes;
	}
	public WorkNodeReq getObj() {
		return obj;
	}
	public void setObj(WorkNodeReq obj) {
		this.obj = obj;
	}
}
