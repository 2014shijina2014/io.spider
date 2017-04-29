/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.client;

import static io.spider.meta.SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP;
import static io.spider.meta.SpiderOtherMetaConstant.CLUSTERNAME_AND_ADDRESS_SEP;
import io.netty.channel.Channel;
import io.spider.BeanManagerHelper;
import io.spider.SpiderRouter;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.utils.AES128Utils;
import io.spider.utils.CRCUtils;
import io.spider.utils.JsonUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderClientDispatcher {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static int execute(Channel channel,Object origMsg,StringBuilder spiderRespPacketSB) {
		SpiderPacketHead packetHead = new SpiderPacketHead();
		SpiderPacketHead.getPacketHead(origMsg.toString(), packetHead);
		
		String spiderReqPacket = origMsg.toString();
		String spiderPacketBody = spiderReqPacket.substring(packetHead.getPacketHeadSize());
		if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT_PUSH)) {
			GlobalConfig.interceptQueues.add(spiderPacketBody);
			return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
		}
		
		String tmpParamJsonStr = "";
		
		if(!packetHead.getCrc32().equals(CRCUtils.getCRC32Value(spiderPacketBody,GlobalConfig.charset))) {
			try {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CRC32_CHECK_FAIL);
				SpiderRouter.spiderMultiplex.get(packetHead.getRpcMsgId()).put(spiderReqPacket);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
		}
		
		try {
			if(GlobalConfig.encrypt) 
				tmpParamJsonStr = AES128Utils.aesDecrypt(spiderPacketBody);
			else 
				tmpParamJsonStr = spiderPacketBody;
		} catch (Exception e1) {
			logger.error("spider请求正文解密失败, 待解密正文[", spiderPacketBody + "]");
			try {
				SpiderRouter.spiderMultiplex.get(packetHead.getRpcMsgId()).put(spiderReqPacket);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
		}
		
		if(packetHead.getMsgType() == SpiderOtherMetaConstant.MSG_TYPE_RESP) {
			if(SpiderServiceIdConstant.internalClientServices.contains(packetHead.getServiceId())) {
				if(packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_PROTOCOL)) {
					if(tmpParamJsonStr.equals(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE2_AUTH)) {
						//发送00000020请求
						packetHead.setServiceId(SpiderServiceIdConstant.SERVICE_ID_AUTH);
						try {
							spiderRespPacketSB.append(SpiderPacketHead.getReqSpiderPacket(packetHead,SpiderClientAuthServiceImpl.getLicense(((InetSocketAddress)channel.localAddress()).getAddress())));
						} catch (SpiderException e) {
							logger.error("获取spider.dat license文件发生异常,断开与服务端的连接",e);
							return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION;
						}
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
					} else if (tmpParamJsonStr.equals(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK)){
						// 服务端认证通过
//						// 判断自己是否需要反向注册,如果需要,则发送00000035
//						InetSocketAddress address = ((InetSocketAddress)channel.remoteAddress());
//						if(GlobalConfig.isClusterNeedReverseRegister(address.getHostString(),address.getPort())) {
//							packetHead.setServiceId(SpiderServiceIdConstant.SERVICE_ID_REVERSE_REGISTER);
//							String ret = GlobalConfig.clusterName + CLUSTERNAME_AND_ADDRESS_SEPARATOR + ((InetSocketAddress)channel.localAddress()).getAddress().getHostAddress() + ADDRESS_AND_PORT_SEPARATOR + GlobalConfig.port;
//							logger.info(MessageFormat.format("反向注册服务器{0}",ret));
//							if(GlobalConfig.encrypt) {
//								ret = AES128Utils.aesEncrypt(ret);
//							}
//							spiderRespPacketSB.append(SpiderPacketHead.getReqSpiderPacket(packetHead,ret));
//							return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
//						}
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_2SERVER_AUTH_PASS;
					}
				} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_AUTH)) {
					// 服务端认证通过
//					// 判断自己是否需要反向注册,如果需要,则发送00000035
//					InetSocketAddress address = ((InetSocketAddress)channel.remoteAddress());
//					if(GlobalConfig.isClusterNeedReverseRegister(address.getHostString(),address.getPort())) {
//						packetHead.setServiceId(SpiderServiceIdConstant.SERVICE_ID_REVERSE_REGISTER);
//						String ret = GlobalConfig.clusterName + CLUSTERNAME_AND_ADDRESS_SEPARATOR + ((InetSocketAddress)channel.localAddress()).getAddress().getHostAddress() + ADDRESS_AND_PORT_SEPARATOR + GlobalConfig.port;
//						logger.info(MessageFormat.format("反向注册服务器{0}",ret));
//						if(GlobalConfig.encrypt) {
//							ret = AES128Utils.aesEncrypt(ret);
//						}
//						spiderRespPacketSB.append(SpiderPacketHead.getReqSpiderPacket(packetHead,ret));
//						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
//					}
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_2SERVER_AUTH_PASS;
				}
			} else {
				try {
					SpiderRouter.spiderMultiplex.get(packetHead.getRpcMsgId()).put(spiderReqPacket);
				} catch (NullPointerException e) {
					logger.warn("处理收到的响应发生异常,通常原因之一是客户端定义的超时时间不大于服务端的超时时间,请检查配置！请求报文为：" + spiderReqPacket);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
			}
		} else {
			//对于服务中心发送来的心跳检测,直接返回,什么都不做
			if(packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_HEARTBEAT)) {
				return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
			}
			if(packetHead.getServiceId().startsWith(SpiderServiceIdConstant.SPIDER_INTERNAL_SERVICE_ID_PREFIX)) {
				return processInternalMessage(packetHead,tmpParamJsonStr,spiderRespPacketSB);
			} else {
				logger.warn("非内部功能不允许通过客户端channel发送数据,如果接收到,通常意味着被篡改和拦截！");
			}
		}
		return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
	}

	/**
	 * 仅用于spider内部管理消息通信,不用于业务通信
	 * @param packetHead
	 * @param decryptMsgBody
	 * @param spiderRespPacketSB
	 * @return
	 */
	private static int processInternalMessage(SpiderPacketHead packetHead, Object decryptMsgBody, StringBuilder spiderRespPacketSB) {
		String retJsonStr = "";
		ServiceDefinition service = ServiceDefinitionContainer.getService(packetHead.getServiceId());
		Object bean = null;
		try {
			bean = BeanManagerHelper.getBean(service.getClz());
		} catch (BeansException e) {
			logger.error("can not found bean or bean definition is duplicate, bean type [" + service.getClz().getCanonicalName() + "],this problem usually occured in situation where <context:component-scan base-package=\"com.medsoft.spider\" /> shall put in root web application xml, but now put in mvc web application xml." + e);
			try {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CANNOT_FOUND_SERVICE_IMPLEMENT);
				spiderRespPacketSB.append(SpiderPacketHead.getRespSpiderPacket(packetHead,retJsonStr));
			} catch (Exception e1) {
				logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
			}
			return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
		}
		try {
			Object param;
			if(decryptMsgBody instanceof String) { //其他
				param = JsonUtils.json2Object(decryptMsgBody.toString(), service.getParamType().getType());
			} else {
				//文件传送
				param = decryptMsgBody;
			}
			Object retObj = service.getMethod().invoke(bean, param);
			retJsonStr = (GlobalConfig.encrypt ? AES128Utils.aesEncrypt(JsonUtils.toJson(retObj)) : JsonUtils.toJson(retObj));
			spiderRespPacketSB.append(SpiderPacketHead.getRespSpiderPacket(packetHead,retJsonStr));
			return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("调用服务" + service.getServiceId() + "发生异常，确保服务捕获了异常！", e);
			try {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION);
				spiderRespPacketSB.append(SpiderPacketHead.getRespSpiderPacket(packetHead,SpiderOtherMetaConstant.NOTHING));
				return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
			} catch (Exception e1) {
				logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
			}
		}
		return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
	}
}
