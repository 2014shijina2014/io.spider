/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;
import org.dom4j.Element;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public interface SpiderPlugin {
	/**
	 * 解析spider自定义插件参数和配置
	 * @param customPluginEle
	 */
	public void doParse(Element customPluginEle);
	
	/**
	 * 插件处理逻辑实现
	 * @param packet
	 * @return
	 */
	public void doService(SpiderPacketPluginInternalResp resp,SpiderPacketPluginReq packet);
}
