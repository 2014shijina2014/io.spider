/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.spider.channel.SocketHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderPacketPosConstant;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.server.SpiderServerDispatcher;
import io.spider.stat.ServiceStatHelper;
import io.spider.utils.AES128Utils;
import io.spider.utils.JsonUtils;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.spider.meta.SpiderErrorNoConstant.ERROR_INFO_DISCONNECT_FROM_SERVER;
import static io.spider.meta.SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER;
import static io.spider.server.SpiderServerDispatcher.localAddress;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class SpiderRouter {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static Map<String,BlockingQueue<String>> spiderMultiplex = new ConcurrentHashMap<String,BlockingQueue<String>>(Runtime.getRuntime().availableProcessors() * 2000);
	public static Map<String,BlockingQueue<Object>> spiderLdPackMultiplex = new ConcurrentHashMap<String,BlockingQueue<Object>>();
	
	/**
	 * 
	 * @param head
	 * @param msgBody
	 * @param serviceDef
	 * @return
	 * @throws Exception
	 */
	public static String call(SpiderPacketHead head,String msgBody,ServiceDefinition serviceDef) throws Exception {
		Channel channel = GlobalConfig.getRandomConn(head,serviceDef.getClusterName(),Thread.currentThread().getName());
		//这里需要判断channel是否为空,因为getRandomWorkNode会按需建立必要的socket连接
		if (channel == null) {
			throw new SpiderException(head.getServiceId(),
									ERROR_NO_DISCONNECT_FROM_SERVER,
									ERROR_INFO_DISCONNECT_FROM_SERVER.replace("{2}",serviceDef.getClusterName()).replace("{1}",SpiderServerDispatcher.localAddress + "(" + GlobalConfig.clusterName + ")").replace("{2}","channel is null"));
		}
		return call(head, msgBody, channel,serviceDef.getTimeout());
	}

	/**
	 * 
	 * @param head
	 * @param msgBody
	 * @param channel
	 * @param timeout
	 * @return 包含spiderPacketHead头的完整报文串
	 * @throws Exception
	 */
	public static String call(SpiderPacketHead head, String msgBody, Channel channel,int timeout) throws Exception {
		if(head.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_PROTOCOL)) {
			SocketHelper.writeMessage(channel,SpiderPacketHead.getReqSpiderPacket(head, msgBody));
			return "";
		}
		//获取定义获取移到外面，这样动态和静态调用本身的逻辑就不变 20160805 changed
		//ServiceDefinition serviceDef = ServiceDefinitionContainer.getService(serviceId);
		//调用spider服务,为了便于公用,加解密,crc32计算,msgId均从外部传入,只负责rpc调用本身
		/* 2016年12月1日增加判断是否已经包含相同的rpcMsgId, 虽然出现重复的概率极低,理论上每秒产生10亿笔UUID，100年后只产生一次重复的机率是50%。如果地球上每个人都各有6亿笔UUID，发生一次重复的机率是50%
		 * 或者说,如果一台机器每秒产生10000000个GUID,则可以 保证(概率意义上)3240年不重复。
		 * 以后rpcMsgId可能会暴露给用户设置, 故先增加保护
		 */
		if(spiderMultiplex.containsKey(head.getRpcMsgId())) {
			throw new SpiderException(head.getServiceId(),SpiderErrorNoConstant.ERROR_NO_RPC_MSG_ID_IS_DUPLICATE,SpiderErrorNoConstant.ERROR_INFO_RPC_MSG_ID_IS_DUPLICATE,head.toString());
		}
		BlockingQueue<String> retQueue = new LinkedBlockingQueue<String>(1);
		spiderMultiplex.put(head.getRpcMsgId(), retQueue);
		long beg = System.currentTimeMillis();
		if(GlobalConfig.dev) {
			if(channel == null) {
				logger.warn("channel is null");
				spiderMultiplex.remove(head.getRpcMsgId());
				throw new SpiderException(head.getServiceId(),ERROR_NO_DISCONNECT_FROM_SERVER,MessageFormat.format(ERROR_INFO_DISCONNECT_FROM_SERVER,((InetSocketAddress)channel.remoteAddress()).toString(),localAddress + "(" + GlobalConfig.clusterName + ")",""));
			}
		}
		String packetStr = SpiderPacketHead.getReqSpiderPacket(head, msgBody);
		ChannelFuture future = SocketHelper.writeMessage(channel,packetStr);
		if (future == null) {
			logger.error("future is null for packet [" + packetStr + "]");
			spiderMultiplex.remove(head.getRpcMsgId());
			throw new SpiderException(head.getServiceId(),ERROR_NO_DISCONNECT_FROM_SERVER,ERROR_INFO_DISCONNECT_FROM_SERVER.replace("{0}",((InetSocketAddress)channel.remoteAddress()).toString()).replace("{1}",localAddress).replace("{2}",""));
		}
		
		if (future.isSuccess()) {
			if (GlobalConfig.dev) {
				if (!GlobalConfig.noLoggingList.containsKey(head.getServiceId().trim())) {
					logger.info((GlobalConfig.logMsgIdPrefix ? (head.getRpcMsgId() + " ") : "") + "spider req to [" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]:" + packetStr);
				}
			} else {
				if (GlobalConfig.loggingList.containsKey(head.getServiceId().trim())) {
					logger.info((GlobalConfig.logMsgIdPrefix ? (head.getRpcMsgId() + " ") : "") + "spider req to [" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]:" + packetStr);
				}
			}
			String retJsonStr = retQueue.poll(timeout, TimeUnit.MILLISECONDS);
			long end = System.currentTimeMillis();
			if(end-beg >= GlobalConfig.slowLongTime) {
				ServiceStatHelper.writeSlowRequest(beg,end,head,msgBody,"[" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]");
			}
			ServiceStatHelper.putStat(Thread.currentThread().getName(),head.getServiceId(),end-beg);
			spiderMultiplex.remove(head.getRpcMsgId());
			if(retJsonStr == null) {
				logger.error("调用发送到[" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]" + "的服务"
										+ head.getServiceId() + "发生超时,该服务超时时间为" + timeout + "毫秒！");
				throw new SpiderException(head.getServiceId(),SpiderErrorNoConstant.ERROR_NO_TIMEOUT,SpiderErrorNoConstant.ERROR_INFO_TIMEOUT);
			}
			return retJsonStr;
		} else if(future.isCancelled()) {
			spiderMultiplex.remove(head.getRpcMsgId());
			throw new SpiderException(head.getServiceId(),SpiderErrorNoConstant.ERROR_NO_REQUEST_IS_CANCELLED,SpiderErrorNoConstant.ERROR_INFO_REQUEST_IS_CANCELLED,"");
		} else {
			spiderMultiplex.remove(head.getRpcMsgId());
			logger.error("netty future发生异常：",future.cause());
			throw new SpiderException(head.getServiceId(),SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION,SpiderErrorNoConstant.ERROR_INFO_UNEXPECT_EXCEPTION.replace("{0}", localAddress));
		}
	}
	
	@Deprecated
	public static String call(SpiderPacketHead head, byte[] msgBody, Channel channel,int timeout) throws Exception {
		//获取定义获取移到外面，这样动态和静态调用本身的逻辑就不变 20160805 changed
		//ServiceDefinition serviceDef = ServiceDefinitionContainer.getService(serviceId);
		//调用spider服务,为了便于公用,加解密,crc32计算,msgId均从外部传入,只负责rpc调用本身
		BlockingQueue<String> retQueue = new LinkedBlockingQueue<String>(1);
		if (timeout != -1) {
			spiderMultiplex.put(head.getRpcMsgId(), retQueue);
		}
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(SpiderPacketHead.getReqSpiderPacketHead(head, msgBody).getBytes());
		bos.write(msgBody);
		ChannelFuture future = SocketHelper.writeMessage(channel,Unpooled.wrappedBuffer(bos.toByteArray()));
		if (!future.await(5000)) {
			if(timeout != -1) {
				spiderMultiplex.remove(head.getRpcMsgId());
			}
			throw new SpiderException(ERROR_NO_DISCONNECT_FROM_SERVER,ERROR_INFO_DISCONNECT_FROM_SERVER.replace("{0}",((InetSocketAddress)channel.remoteAddress()).toString()).replace("{1}", localAddress));
		}
		if(timeout == -1) {
			return "";
		}
		String retJsonStr = retQueue.poll(timeout, TimeUnit.MILLISECONDS);
		spiderMultiplex.remove(head.getRpcMsgId());
		if(retJsonStr == null) {
			logger.error("调用发送到[" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]" + 
						"的服务" + head.getServiceId() + "发生超时,该服务超时时间为" + timeout + "毫秒！");
			throw new SpiderException(head.getServiceId(),SpiderErrorNoConstant.ERROR_NO_TIMEOUT,SpiderErrorNoConstant.ERROR_INFO_TIMEOUT.replace("{0}", localAddress));
		}
		return retJsonStr;
	}

	//only for internal use
	public static <T> T getBody(String retPacket, Class<T> clz) {
		String retBiz = retPacket.substring(Integer.valueOf(retPacket.substring(SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET,SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET + SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN)));
		String errorNo = retPacket.substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
		if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_SUCCESS)) {
			if(GlobalConfig.encrypt) {
				try {
					retBiz = AES128Utils.aesDecrypt(retBiz);
				} catch (Exception e) {
					throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_DECRYPT_FAIL,
							SpiderErrorNoConstant.ERROR_INFO_DECRYPT_FAIL.replace("{0}", localAddress),retBiz);
				}
			}
			
			T retObj = JsonUtils.json2Object(retBiz,clz);
			return retObj;
		}
		
		throw new SpiderException(errorNo,
				SpiderErrorNoConstant.getErrorInfo(errorNo),
				"some exception throwed during call the spider service. "
				+ "cause: errorNo=" + errorNo + ",errorInfo=" + SpiderErrorNoConstant.getErrorInfo(errorNo) + ", "
				+ "error first occured in server " + retBiz);
	}
}
