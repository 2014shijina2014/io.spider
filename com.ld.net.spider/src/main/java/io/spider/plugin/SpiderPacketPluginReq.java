/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;

import io.spider.meta.SpiderPacketHead;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderPacketPluginReq {
	private SpiderPacketHead spiderPacketHead;
	private String decryptRequestBody;
	public SpiderPacketHead getSpiderPacketHead() {
		return spiderPacketHead;
	}
	public void setSpiderPacketHead(SpiderPacketHead spiderPacketHead) {
		this.spiderPacketHead = spiderPacketHead;
	}
	public String getDecryptRequestBody() {
		return decryptRequestBody;
	}
	public void setDecryptRequestBody(String decryptRequestBody) {
		this.decryptRequestBody = decryptRequestBody;
	}
}
