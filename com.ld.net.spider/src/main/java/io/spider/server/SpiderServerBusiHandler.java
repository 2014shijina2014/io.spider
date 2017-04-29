/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.server;

import static io.spider.meta.SpiderPacketPosConstant.SPIDER_FIXED_PACKET_HEAD_SIZE;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_OFFSET;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.spider.channel.SocketHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.TcpDumpContainer;
import io.spider.utils.ZlibUtils;

import java.net.InetSocketAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * @since 1.0.9 SSL支持
 */
public class SpiderServerBusiHandler extends SimpleChannelInboundHandler<Object> {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public final static ExecutorService executor = Executors.newFixedThreadPool(GlobalConfig.busiThreadCount);
	
	public final static ConcurrentHashMap<String,Object> allRequests = new ConcurrentHashMap<String,Object>(); 
	
	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
		logger.info("received tcp connection from [" + sa.toString() + "].");
	}
	/**
	 * 需要断开的时候进行善后处理
     */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
		logger.info("disconnected from tcp connection [" + sa.toString() + "].");
		logger.info("从GlobalConfig.sourceWorkNode删除!");
		GlobalConfig.removeSourceWorkNode(sa.getAddress().getHostAddress(), sa.getPort());
		if(GlobalConfig.tcpdump) {
			if(GlobalConfig.tcpDumpClients.containsKey(ctx.channel())) {
				logger.info("从GlobalConfig.tcpDumpClients删除抓包客户端!");
				GlobalConfig.tcpDumpClients.remove(ctx.channel());
			}
			if(TcpDumpContainer.tcpDumps.containsKey(ctx.channel())) {
				logger.info("从TcpDumpContainer.tcpDumps删除已捕获包!");
				TcpDumpContainer.tcpDumps.remove(ctx.channel());
			}
		}
	}
	
	@Override
	protected void channelRead0(final ChannelHandlerContext ctx, final Object msg)
			throws Exception {
		
		final String msgId = msg.toString().substring(SPIDER_MSG_ID_OFFSET, SPIDER_MSG_ID_OFFSET + SPIDER_MSG_ID_LEN);
		
		final String prefixMsgId = GlobalConfig.logMsgIdPrefix ? (msgId + " ") : "";
		
		if(GlobalConfig.dev) {
			InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
			logger.info(prefixMsgId + "spider req from [" + sa.toString() + "]: " + msg);
		}
		
		if (!GlobalConfig.compress) {
			allRequests.put(msgId, msg);
		}
		
		executor.submit(new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				StringBuilder sb = new StringBuilder();
				int action = SpiderServerDispatcher.execute(ctx.channel(),((InetSocketAddress)ctx.channel().remoteAddress()),GlobalConfig.compress ? ZlibUtils.decompress(msg.toString()) : msg.toString(),sb);
				String retJson = sb.toString();
				if(GlobalConfig.dev) {
					InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
					if(GlobalConfig.noLoggingList.containsKey(retJson.substring(SPIDER_SERVICE_ID_OFFSET, SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN).trim())) {
						logger.info(prefixMsgId + "spider fin for [" + sa.toString() + "],action:" + action + ",return: " + retJson.substring(0, SPIDER_FIXED_PACKET_HEAD_SIZE));
					} else {
						logger.info(prefixMsgId + "spider fin for [" + sa.toString() + "],action:" + action + ",return: " + retJson);
					}
				} else {
					InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
					if(GlobalConfig.loggingList.containsKey(retJson.substring(SPIDER_SERVICE_ID_OFFSET, SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN).trim())) {
						logger.info(prefixMsgId + "spider fin for [" + sa.toString() + "],action:" + action + ",return: " + retJson);
					}
				}
				if(action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN) {
					ChannelFuture future = SocketHelper.writeMessage(ctx.channel(),retJson);
					if(!future.isSuccess()) {
						logger.error("send response failed:");
						if(future.isCancelled()) {
							logger.error(SpiderErrorNoConstant.ERROR_INFO_REQUEST_IS_CANCELLED);
						} else {
							logger.error("",future.cause());
						}
					}
				} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION) {
					InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
					logger.warn("spider server forcely disconnect client [" + sa.toString() + "]");
					ctx.channel().deregister().sync();
					ctx.channel().disconnect().sync();
					ctx.channel().close().sync();
				} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP) {
					//NOP
				} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN_AND_CLOSE) {
					SocketHelper.writeMessage(ctx.channel(),retJson);
					InetSocketAddress sa = (InetSocketAddress)ctx.channel().remoteAddress();
					logger.warn("spider server forcely disconnect client [" + sa.toString() + "]: " + retJson);
					ctx.channel().deregister().sync();
					ctx.channel().disconnect().sync();
					ctx.channel().close().sync();
				} else if (action == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL) {
					//发生致命异常
					throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION,SpiderErrorNoConstant.ERROR_INFO_UNEXPECT_EXCEPTION,this.getClass().getCanonicalName() + "发生了未预知的致命异常！");
				}
				allRequests.remove(msgId);
				return null;
			}
		});
	}

	/**
	 * 一般来说,除非SpiderServerDispatcher出现未捕获异常,不然服务端不会主动导致异常
	 * 亦或是客户端异常断开, 此时会抛出下列异常:
	 * io.netty.channel.unix.Errors$NativeIoException: syscall:read(...)() failed: Connection reset by peer
	 * at io.netty.channel.unix.FileDescriptor.readAddress(...)(Unknown Source)
	 * 最后会由事件channelInactive进行统一善后处理
	 */
	@Override 
    public void exceptionCaught(ChannelHandlerContext ctx,  
            Throwable cause) throws Exception {  
        logger.error("channel " + ((InetSocketAddress)ctx.channel().remoteAddress()).toString() + " exception:",cause);
        ctx.close();
    }
	
	@Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }
}
