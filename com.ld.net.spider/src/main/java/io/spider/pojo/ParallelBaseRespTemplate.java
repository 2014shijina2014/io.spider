/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.sc.pojo.WorkNodeReq;

/**
 * spider并行执行响应基类模板
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ParallelBaseRespTemplate extends ParallelBaseResp {
	
	public ParallelBaseRespTemplate() {}

	/**
	 * @param resp
	 * @param workNode
	 */
	public ParallelBaseRespTemplate(SpiderBaseResp resp, WorkNodeReq workNode) {
		super(resp, workNode);
	}
	
	private Object data;
	
	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "ParallelBaseRespTemplate [data=" + data + ", getClusterName()="
				+ getClusterName() + ", getIp()=" + getIp() + ", getPort()="
				+ getPort() + ", getErrorNo()=" + getErrorNo()
				+ ", getErrorInfo()=" + getErrorInfo() + ", getCause()="
				+ getCause() + "]";
	}
}
