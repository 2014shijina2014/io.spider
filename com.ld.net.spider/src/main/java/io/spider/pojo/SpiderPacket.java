/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderPacketHead;
import io.spider.utils.JsonUtils;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderPacket {
	private SpiderPacketHead spiderPacketHead;
	private Object body;
	public SpiderPacketHead getSpiderPacketHead() {
		return spiderPacketHead;
	}
	public void setSpiderPacketHead(SpiderPacketHead spiderPacketHead) {
		this.spiderPacketHead = spiderPacketHead;
	}
	public Object getBody() {
		return body;
	}
	public void setBody(Object body) {
		this.body = body;
	}
	public String getJsonBody() {
		return JsonUtils.toJson(body);
	}
	@Override
	public String toString() {
		return "SpiderPacket [spiderPacketHead=" + spiderPacketHead + ", body="
				+ body + "]";
	}
}
