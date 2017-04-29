/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class RouteItemForTcpDump extends RouteItem {
	private short action; //1:抓包; 2:拦截;

	public short getAction() {
		return action;
	}

	public void setAction(short action) {
		this.action = action;
	}
}
