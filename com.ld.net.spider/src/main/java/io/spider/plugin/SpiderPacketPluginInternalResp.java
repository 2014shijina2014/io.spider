/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderPacketPluginInternalResp extends SpiderPacketPluginResp {
	/**
	 * 仅在super.dispatchRetCode=PLUGIN_RET_CODE_SPECIFIED_PLUGIN时有效
	 */
	private String nextPluginName;

	public String getNextPluginName() {
		return nextPluginName;
	}

	public void setNextPluginName(String nextPluginName) {
		this.nextPluginName = nextPluginName;
	}
}
