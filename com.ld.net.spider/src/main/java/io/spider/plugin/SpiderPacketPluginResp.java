/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;

import io.spider.meta.SpiderPacketHead;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderPacketPluginResp {
	/**
	 * 参考SpiderOtherMetaConstant.DISPATCHER_RET_CODE_*
	 */
	private int dispatchRetCode;
	
	private String decryptRespBody;
	
	private Map<SpiderPacketHead,String> decryptRespBodies = new HashMap<SpiderPacketHead,String>();
	
	/**
	 * 插件返回值有必要复制原始请求的报文头,因为有可能会修改,比如在并行计算的时候,此时必须重新计算CRC32以及加密
	 * 插件必须实现,主程序会根据此进行后续处理,不过具体的自定义插件无需关心CRC32以及加解密,PluginProcessor会负责统一处理,并行插件还需自己重新设置rpcMsgId
	 */
	private SpiderPacketHead spiderPacketHead;
	
	public SpiderPacketHead getSpiderPacketHead() {
		return spiderPacketHead;
	}
	public void setSpiderPacketHead(SpiderPacketHead spiderPacketHead) {
		this.spiderPacketHead = spiderPacketHead;
	}
	
	public Map<SpiderPacketHead, String> getDecryptRespBodies() {
		return decryptRespBodies;
	}
	public void setDecryptRespBodies(Map<SpiderPacketHead, String> decryptRespBodies) {
		this.decryptRespBodies = decryptRespBodies;
	}
	public int getDispatchRetCode() {
		return dispatchRetCode;
	}
	public void setDispatchRetCode(int dispatchRetCode) {
		this.dispatchRetCode = dispatchRetCode;
	}

	public void addRespBody(SpiderPacketHead head,String decryptRespBody) {
		decryptRespBodies.put(head,decryptRespBody);
	}
	
	@JsonIgnore
	public String getDecryptRespBody() {
		return this.decryptRespBody;
	}
	public void setDecryptRespBody(String decryptRespBody) {
		this.decryptRespBody = decryptRespBody;
	}
}
