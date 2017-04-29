/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.spider.SpiderRouter;
import io.spider.channel.SocketClientHelper;
import io.spider.channel.SocketHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.AES128Utils;
import io.spider.utils.UUIDUtils;
import io.spider.utils.ZlibUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.spider.meta.SpiderPacketPosConstant.SPIDER_FIXED_PACKET_HEAD_SIZE;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_OFFSET;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class SpiderClientBusiHandler extends SimpleChannelInboundHandler<Object> {

	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object origMsg) {
		String msgId = "";
		if (!GlobalConfig.compress) {
			msgId = GlobalConfig.logMsgIdPrefix ? (origMsg.toString().substring(SPIDER_MSG_ID_OFFSET, SPIDER_MSG_ID_OFFSET + SPIDER_MSG_ID_LEN) + " ") : "";
		}
		String remoteAddr = ((InetSocketAddress)ctx.channel().remoteAddress()).toString();
		if(GlobalConfig.dev) {
			if(GlobalConfig.noLoggingList.containsKey(origMsg.toString().substring(SPIDER_SERVICE_ID_OFFSET, SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN).trim())) {
				logger.debug(msgId + "spider resp from [" + remoteAddr + "]:" + origMsg.toString().substring(0, SPIDER_FIXED_PACKET_HEAD_SIZE));
			} else {
				logger.info(msgId + "spider resp from [" + remoteAddr + "]:" + origMsg.toString().substring(0, Math.min(origMsg.toString().length(),10000)));
			}
		} else {
			if(GlobalConfig.loggingList.containsKey(origMsg.toString().substring(SPIDER_SERVICE_ID_OFFSET, SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN).trim())) {
				logger.info(msgId + "spider resp from [" + remoteAddr + "]:" + origMsg.toString().substring(0, Math.min(origMsg.toString().length(),10000)));
			}
		}
		StringBuilder sb = new StringBuilder();
		int action = SpiderClientDispatcher.execute(ctx.channel(),GlobalConfig.compress ? ZlibUtils.decompress(origMsg.toString()) : origMsg,sb);
		if(action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN) {
			SocketHelper.writeMessage(ctx.channel(),sb.toString());
		} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION) {
			ctx.channel().deregister();
			ctx.channel().disconnect();
			try {
				ctx.channel().close().sync();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_2SERVER_AUTH_PASS) {
			//TODO 可选的, 需要识别出当前通道所属的cluster, 主要用于识别服务端,以免服务端是伪造的
		} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP) {
			//NOP
		} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL) {
			//发生致命异常
			throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION,SpiderErrorNoConstant.getErrorInfo(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION),this.getClass().getCanonicalName() + "发生了未预知的致命异常！");
		}
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) {
		try {
			SpiderPacketHead head = new SpiderPacketHead();
			head.setServiceId(SpiderServiceIdConstant.SERVICE_ID_PROTOCOL);
			head.setRpcMsgId(UUIDUtils.uuid());
			String msgBody = SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE1_MSG;
			if (GlobalConfig.encrypt) {
				msgBody = AES128Utils.aesEncrypt(msgBody);
			}
			SpiderRouter.call(head, msgBody,ctx.channel(),0);
		} catch (Exception e) {
			logger.error("client channelActive throwed uncatched exception.",e);
		}
	}
	
	@Override
	public void channelInactive(ChannelHandlerContext ctx) {
		ctx.close();
		logger.warn("已断开和服务端" + ((InetSocketAddress)ctx.channel().remoteAddress()).toString() + "的连接.");
		if(SocketClientHelper.clientGroups.contains(ctx.channel().eventLoop().parent())) {
			try {
				ctx.channel().eventLoop().parent().shutdownGracefully(0, 100, TimeUnit.MILLISECONDS).await(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.info((SocketClientHelper.clientGroups.remove(ctx.channel().eventLoop().parent()) ? "已" : "未") + "删除断开的EventLoopGroup实例!");
		}
	}
	
	@Override 
    public void exceptionCaught(ChannelHandlerContext ctx,  
            Throwable cause) {  
        logger.error("channel to " + ((InetSocketAddress)ctx.channel().remoteAddress()).toString() + " exception:",cause);
        ctx.close();
        if(SocketClientHelper.clientGroups.contains(ctx.channel().eventLoop().parent())) {
        	try {
				ctx.channel().eventLoop().parent().shutdownGracefully(0, 100, TimeUnit.MILLISECONDS).await(150);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			logger.info((SocketClientHelper.clientGroups.remove(ctx.channel().eventLoop().parent()) ? "已" : "未") + "删除异常的EventLoopGroup实例!");
		}
    }
}
