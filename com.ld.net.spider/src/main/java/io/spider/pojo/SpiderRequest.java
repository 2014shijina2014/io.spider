/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderPacketHead;

import com.fasterxml.jackson.annotation.JsonIgnore;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderRequest {
	
	private SpiderPacketHead spiderPacketHead;
	
	private String requestBody;
	
	private String resultInfo;

	public SpiderPacketHead getSpiderPacketHead() {
		return spiderPacketHead;
	}

	public void setSpiderPacketHead(SpiderPacketHead spiderPacketHead) {
		this.spiderPacketHead = spiderPacketHead;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public void setRequestBody(String requestBody) {
		this.requestBody = requestBody;
	}
	
	@JsonIgnore
	public String getRequestId() {
		return spiderPacketHead.getRpcMsgId();
	}

	public String getResultInfo() {
		return resultInfo;
	}

	public void setResultInfo(String resultInfo) {
		this.resultInfo = resultInfo;
	}

	@Override
	public String toString() {
		return "SpiderRequest [spiderPacketHead=" + spiderPacketHead
				+ ", requestBody=" + requestBody + ", resultInfo=" + resultInfo
				+ "]";
	}
}
