/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;

import io.spider.meta.SpiderBizHead;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ShellReq extends SpiderBizHead {
	
	private short execMode = 1; // 1: 本地执行; 2: ssh;
	
	private String proxyNode;
	
	private int port = 22;
	
	private String cmd;
	
	private String username;
	
	private String base64Pwd;
	
	private boolean checkProxyNode = false;
	
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	public short getExecMode() {
		return execMode;
	}

	public void setExecMode(short execMode) {
		this.execMode = execMode;
	}

	public String getProxyNode() {
		return proxyNode;
	}

	public void setProxyNode(String proxyNode) {
		this.proxyNode = proxyNode;
	}

	public String getBase64Pwd() {
		return base64Pwd;
	}

	public void setBase64Pwd(String base64Pwd) {
		this.base64Pwd = base64Pwd;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public boolean isCheckProxyNode() {
		return checkProxyNode;
	}

	public void setCheckProxyNode(boolean checkProxyNode) {
		this.checkProxyNode = checkProxyNode;
	}
}
