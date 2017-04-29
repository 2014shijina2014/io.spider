/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin.impl;

import io.spider.BeanManagerHelper;
import io.spider.meta.SpidePacketVarHeadTagName;
import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.plugin.SpiderPacketPluginInternalResp;
import io.spider.plugin.SpiderPacketPluginReq;
import io.spider.plugin.SpiderPluginTemplate;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.utils.JsonUtils;
import io.spider.utils.UUIDUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * spider 通信中间件
 * spider 会话校验处理插件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SessionPluginImpl extends SpiderPluginTemplate {
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	/* (non-Javadoc)
	 * @see com.ld.net.spider.plugin.SpiderPlugin#doService(com.ld.net.spider.plugin.SpiderPacketPluginReq)
	 */
	@Override
	public void doService(SpiderPacketPluginInternalResp resp,SpiderPacketPluginReq packet) {
		if(!GlobalConfig.role.equalsIgnoreCase(SpiderOtherMetaConstant.SPIDER_RT_ROLE_NB) && !GlobalConfig.role.equalsIgnoreCase(SpiderOtherMetaConstant.SPIDER_RT_ROLE_NP)) {
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
			return;
		}
		// TODO 实际校验
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.plugin.SpiderPlugin#doParse(org.dom4j.Element)
	 */
	@Override
	public void doParse(Element customPluginEle) {
		if(customPluginEle.element(SpiderConfigName.ELE_ARGS) == null) {
			return;
		}
		List<Element> argList = customPluginEle.element(SpiderConfigName.ELE_ARGS).elements("arg");
		for(Element arg : argList) {
			super.addArg(arg.attributeValue(SpiderConfigName.ATTR_NAME),arg.getText());
		}
	}
}
