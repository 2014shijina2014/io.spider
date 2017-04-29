/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.pojo;

import java.util.List;

import com.ld.net.spider.sc.pojo.ShellReq;
import com.ld.net.spider.sc.pojo.WorkNodeReq;

/**
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ShellExecuteReq {
	private List<WorkNodeReq> targetNodes;
	private ShellReq obj;
	public List<WorkNodeReq> getTargetNodes() {
		return targetNodes;
	}
	public void setTargetNodes(List<WorkNodeReq> targetNodes) {
		this.targetNodes = targetNodes;
	}
	public ShellReq getObj() {
		return obj;
	}
	public void setObj(ShellReq obj) {
		this.obj = obj;
	}
}
