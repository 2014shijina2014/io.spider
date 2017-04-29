/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.channel;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.ZlibUtils;

import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SocketHelper {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	//仅用于内部通信,不供业务直接使用
	public static ChannelFuture writeMessage(Channel channel,String msg) {  
        if(channel!=null && !channel.eventLoop().isShuttingDown()){  
            try {
            	if(GlobalConfig.compress) {
            		return channel.writeAndFlush(ZlibUtils.compress(msg)).sync();
            	}
            	return channel.writeAndFlush(msg).sync();
			} catch (Exception e) {
				String otherInfo = "";
				
				if(channel.remoteAddress() != null) {
					otherInfo = "remote address [" + ((InetSocketAddress)channel.remoteAddress()).toString() + "]";
				} else {
					otherInfo = "channel is null.";
				}
				
				if(e instanceof ClosedChannelException || e instanceof RejectedExecutionException) {
					logger.error("channel to " + otherInfo + " is closed",e);
				} else {
					logger.error("timeout occured during channel send msg, " + otherInfo,e);
				}
			}
        }else{
        	logger.error("send msg[" + msg + "] failed, channel is disconnected or not connect or channel is null, please see caller log.");
        }
        return null;
    }
	
	public static ChannelFuture writeMessage(Channel channel,ByteBuf msg) {  
        if(channel!=null){  
            try {
				return channel.writeAndFlush(msg);
			} catch (Exception e) {
				logger.error("timeout occured during channel send msg. remote address is:" + ((InetSocketAddress)channel.remoteAddress()).toString(),e);
			}
        }else{
        	logger.error("send msg failed, channel is disconnected or not connect, channel is null, please see caller log.");
        }
        return null;
    }
}
