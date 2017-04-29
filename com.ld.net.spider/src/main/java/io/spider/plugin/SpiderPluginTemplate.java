/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;

import java.util.HashMap;
import java.util.Map;

import org.dom4j.Element;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public abstract class SpiderPluginTemplate implements SpiderPlugin {
	public final static ObjectMapper mapper = new ObjectMapper();
	
	/**
	 * 插件名称,实现时必须设置
	 */
	private String pluginName;
	/**
	 * 插件参数,可选
	 */
	private Map<String,Object> args = new HashMap<String,Object>();
	
	public Map<String, Object> getArgs() {
		return args;
	}

	public void setArgs(Map<String, Object> args) {
		this.args = args;
	}

	public void addArg(String name,Object value) {
		args.put(name, value);
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.plugin.SpiderPlugin#doParse(org.dom4j.Element)
	 */
	@Override
	public abstract void doParse(Element customPluginEle);

	/* (non-Javadoc)
	 * @see com.ld.net.spider.plugin.SpiderPlugin#doService(com.ld.net.spider.plugin.SpiderPacketPluginReq)
	 */
	@Override
	public abstract void doService(SpiderPacketPluginInternalResp resp,SpiderPacketPluginReq packet);

	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

}
