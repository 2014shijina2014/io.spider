/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderErrorNoConstant;
import io.spider.sc.pojo.WorkNodeReq;
/**
 * 并行执行内部响应基类
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ParallelBaseResp extends SpiderBaseResp {
	private String clusterName = "-1";
	private String ip = "-1";
	private int port = -1;
	
	public ParallelBaseResp() {}
	
	public ParallelBaseResp(SpiderBaseResp resp, WorkNodeReq workNode) {
		super(resp.getErrorNo(),resp.getErrorInfo());
		this.clusterName = workNode.getClusterName();
		this.ip = workNode.getIp();
		this.port = workNode.getPort();
	}
	public ParallelBaseResp(String errorNo, WorkNodeReq workNode) {
		super(errorNo,SpiderErrorNoConstant.getErrorInfo(errorNo));
		this.clusterName = workNode.getClusterName();
		this.ip = workNode.getIp();
		this.port = workNode.getPort();
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public String toString() {
		return "ParallelBaseResp [clusterName=" + clusterName + ", ip=" + ip
				+ ", port=" + port + ", getErrorNo()=" + getErrorNo()
				+ ", getErrorInfo()=" + getErrorInfo() + ", getCause()="
				+ getCause() + "]";
	}
}
