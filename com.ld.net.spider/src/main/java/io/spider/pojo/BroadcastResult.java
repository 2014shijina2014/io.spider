/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import java.util.ArrayList;
import java.util.List;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 这个实体类仅用于响应式服务广播的用途,此时超时时间会自动设置为120秒，无条件，以确保尽可能不发生异常
 */
public class BroadcastResult extends ParallelBaseRespTemplate {
	private boolean isServer;
	private List<BroadcastResult> children = new ArrayList<BroadcastResult>();

	public boolean isServer() {
		return isServer;
	}
	public void setServer(boolean isServer) {
		this.isServer = isServer;
	}
	public List<BroadcastResult> getChildren() {
		return children;
	}
	public void setChildren(List<BroadcastResult> children) {
		this.children = children;
	}
}
