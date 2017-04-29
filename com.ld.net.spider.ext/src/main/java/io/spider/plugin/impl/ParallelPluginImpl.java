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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * spider 通信中间件
 * spider 并行处理插件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ParallelPluginImpl extends SpiderPluginTemplate {
	public static final String FACTOR_TYPE = SpidePacketVarHeadTagName.PARALLEL_FACTOR_TYPE;
	public static final String FACTORS = SpidePacketVarHeadTagName.PARALLEL_FACTORS;
	public static final String DEGREE = SpidePacketVarHeadTagName.PARALLEL_DEGREE;
	public static final String FACTOR_DRIVER = SpidePacketVarHeadTagName.PARALLEL_FACTOR_DRIVER;
	public static final String COND_JSON_PATH = SpidePacketVarHeadTagName.PARALLEL_COND_JSON_PATH;
	
	public static final String FACTOR_TYPE_ENUM = "1";
	public static final String FACTOR_TYPE_CALL_SERVICE = "2";
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	/* (non-Javadoc)
	 * @see com.ld.net.spider.plugin.SpiderPlugin#doService(com.ld.net.spider.plugin.SpiderPacketPluginReq)
	 */
	@Override
	public void doService(SpiderPacketPluginInternalResp resp,SpiderPacketPluginReq packet) {
		if(!GlobalConfig.role.equalsIgnoreCase(SpiderOtherMetaConstant.SPIDER_RT_ROLE_NP)) {
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
			return;
		}
		Map<String,String> spiderOpt = packet.getSpiderPacketHead().getSpiderOpts();
		if(spiderOpt != null && spiderOpt.containsKey(FACTOR_TYPE) && spiderOpt.containsKey(FACTORS) && spiderOpt.containsKey(COND_JSON_PATH)) {
			if(GlobalConfig.dev) {
				logger.info("收到并行执行请求：" + JsonUtils.toJson(packet));
			}
			String factorType = spiderOpt.get(FACTOR_TYPE);
			String factorsStr = spiderOpt.get(FACTORS);
			int degree = spiderOpt.get(DEGREE) == null ? Math.max(4,Runtime.getRuntime().availableProcessors()/4) : Integer.valueOf(spiderOpt.get(DEGREE));
			List<ArrayList<String>> parallelCondList = new ArrayList<ArrayList<String>>();
			for(int i=0;i<degree;i++) {
				parallelCondList.add(new ArrayList<String>());
			}
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_PARALLEL);
			if(factorType.equals(FACTOR_TYPE_ENUM)) {
				String[] factors = factorsStr.split(",");
				for(int i=0;i<factors.length;i++) {
					parallelCondList.get(i % degree).add(factors[i]);
				}
			} else if (factorType.equals(FACTOR_TYPE_CALL_SERVICE)) {
				ServiceDefinition service = ServiceDefinitionContainer.getService(factorsStr);
				if (service == null) {
					logger.warn("找不到根据并行因子查询服务号：" + factorsStr + ", 转为串行执行！");
					resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
					return;
				}
				try {
					Object obj = BeanManagerHelper.getBean(service.getClz());
					@SuppressWarnings("unchecked")
					List<String> factors = (List<String>)service.getMethod().invoke(obj, spiderOpt.get(FACTOR_DRIVER));
					for(int i=0;i<factors.size();i++) {
						parallelCondList.get(i % degree).add(factors.get(i));
					}
				} catch (Exception e) {
					logger.error("获取并行计算因子执行出错, 转为串行执行！", e);
					resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
					return;
				}
			} else {
				logger.warn("并行执行类型[" + factorType + "]无法识别,转为串行执行！");
				resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
				return;
			}
			// 组装成dispatcher能够识别的请求
			try {
				for(int i=0;i<degree;i++) {
					SpiderPacketHead head = packet.getSpiderPacketHead().copy();
					head.getSpiderOpts().remove(FACTOR_TYPE);
					head.getSpiderOpts().remove(FACTORS);
					head.getSpiderOpts().remove(FACTOR_DRIVER);
					head.getSpiderOpts().remove(COND_JSON_PATH);
					head.getSpiderOpts().remove(DEGREE);
					JsonNode rootNode = mapper.readTree(packet.getDecryptRequestBody());
					String[] jsonPaths = spiderOpt.get(COND_JSON_PATH).split("\\.");
					JsonNode targetNode = null;
					JsonNode prevNode = null;
					for(int j=0;j<jsonPaths.length;j++) {
						targetNode = rootNode.findValue(jsonPaths[j]);
						if(targetNode == null) {
							logger.error("根据查询条件json路径" + spiderOpt.get(COND_JSON_PATH) + "无法找到目标属性, 转为串行执行！");
							resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
							return;
						} else if (targetNode instanceof NullNode) {
							if (j != jsonPaths.length-1) {
								logger.error("路径中间节点[" + jsonPaths[j] + "是null节点,节点路径不正确,转为串行执行！");
								resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
								return;
							} else {
								prevNode = targetNode;
							}
						} else {
							prevNode = targetNode;
						}
					}
					if(prevNode instanceof NullNode) {
						((ObjectNode)rootNode).put(jsonPaths[jsonPaths.length-1], StringUtils.collectionToCommaDelimitedString(parallelCondList.get(i)));
					} else {
						((ObjectNode)prevNode).put(jsonPaths[jsonPaths.length-1], StringUtils.collectionToCommaDelimitedString(parallelCondList.get(i)));
					}
					head.setRpcMsgId(UUIDUtils.uuid());
					resp.addRespBody(head,mapper.writeValueAsString(rootNode));
				}
			} catch (IOException e) {
				logger.error("读取请求参数为JsonNode失败,转为串行执行！",e);
				resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
				return;
			}
		} else {
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
		}
		return;
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
