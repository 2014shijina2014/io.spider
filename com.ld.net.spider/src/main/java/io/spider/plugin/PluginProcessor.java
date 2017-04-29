/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.plugin;

import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;

import java.util.Map.Entry;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class PluginProcessor {
	public static SpiderPacketPluginResp doService(SpiderPacketPluginReq packet) {
		SpiderPacketPluginInternalResp resp = new SpiderPacketPluginInternalResp();
		// 深复制一份原报文, 这样插件如果本身只修改报文头或者报文体都可以, 否则会导致报文头或者报文体为空而出现运行时异常
		resp.setSpiderPacketHead(packet.getSpiderPacketHead().copy());
		resp.setDecryptRespBody(packet.getDecryptRequestBody());
		if(GlobalConfig.plugins.size() == 0) {
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
			return resp;
		}
		String pluginName = "";
		for(Entry<String, SpiderPluginTemplate> plugin : GlobalConfig.plugins.entrySet()) {
			plugin.getValue().doService(resp,packet);
			if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN_AND_CLOSE
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_PARALLEL
				|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON) {
				return resp;
			} else if (resp.getDispatchRetCode() == SpiderOtherMetaConstant.PLUGIN_RET_CODE_NEXT_PLUGIN) {
				//NOP
			} else if (resp.getDispatchRetCode() == SpiderOtherMetaConstant.PLUGIN_RET_CODE_SPECIFIED_PLUGIN) {
				pluginName = plugin.getKey();
				break;
			} else {
				throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_UNSUPPORT_PLUGIN_RET_CODE,SpiderErrorNoConstant.ERROR_INFO_UNSUPPORT_PLUGIN_RET_CODE);
			}
		}
		
		while (resp.getDispatchRetCode() == SpiderOtherMetaConstant.PLUGIN_RET_CODE_SPECIFIED_PLUGIN) {
			GlobalConfig.plugins.get(pluginName).doService(resp,packet);
		}
		if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.PLUGIN_RET_CODE_NEXT_PLUGIN
			|| resp.getDispatchRetCode() == SpiderOtherMetaConstant.PLUGIN_RET_CODE_SPECIFIED_PLUGIN) {
			resp.setDispatchRetCode(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON);
		}
		return resp;
	}
}
