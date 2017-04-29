/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import java.util.List;

import io.spider.sc.pojo.RouteItemReq;
import io.spider.sc.pojo.WorkNodeReq;

public class RouteItemManageReq {
	private List<WorkNodeReq> targetNodes;
	/*将被增加或者删除的节点本身,使用WorkNodeReq纯属为了重用现有刚好完全匹配的POJO*/
	private RouteItemReq obj;
	public List<WorkNodeReq> getTargetNodes() {
		return targetNodes;
	}
	public void setTargetNodes(List<WorkNodeReq> targetNodes) {
		this.targetNodes = targetNodes;
	}
	public RouteItemReq getObj() {
		return obj;
	}
	public void setObj(RouteItemReq obj) {
		this.obj = obj;
	}
}
