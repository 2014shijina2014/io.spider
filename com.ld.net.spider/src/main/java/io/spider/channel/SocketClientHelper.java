/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.channel;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.spider.client.SpiderClientBusiHandler;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SocketClientHelper {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	volatile static AtomicInteger groupCount = new AtomicInteger(0);
	// warning: only for debug, just ignore it
	public static final List<EventLoopGroup> clientGroups = new ArrayList<EventLoopGroup>();
	
	public static Channel createChannel(String host, int port,boolean ssl) {
		Channel channel = null;  
		Bootstrap b = getBootstrap(ssl);
		if (b == null) {
			return null;
		}
        try {  
			channel = b.connect(host, port).sync().channel();
            logger.info(MessageFormat.format("connect to spider server ({0}:{1,number,#}) success for thread [" + Thread.currentThread().getName() + "].", host,port));
        } catch (Exception e) {
        	b.group().shutdownGracefully();
        	logger.info((clientGroups.remove(b.group()) ? "已" : "未") + "删除未创建成功Channel[" + host + ":" + port + "]的EventLoopGroup实例!");
            logger.error(MessageFormat.format("connect to spider server ({0}:{1,number,#}) failed, please check the spider.xml and ensure target server is started."
            				+ System.getProperty("line.separator") + "heartbeat thread will trying to connect to spider server per " + SpiderOtherMetaConstant.HEARTBEAT_INTERVAL_MS/1000 + " seconds.", host,port),e);
        }  
        return channel;
    }
	
	/** 
     * 初始化Bootstrap, Bootstrap可以认为是Netty封装的一个用于创建Socket连接的工具类
     * EventLoopGroup相当于一个线程组,处理这个Bootstrap创建的Socket连接的IO请求,通常一个Socket连接只有一个对应的EventLoopGroup,反之不然
     * @return 
     */  
    public static Bootstrap getBootstrap(boolean ssl){  
    	EventLoopGroup group;
    	Class<? extends Channel> channelClass = NioSocketChannel.class;
    	if(Epoll.isAvailable() && GlobalConfig.enableEpoll) {
    		logger.info("Epoll is enabled!");
    		/**1.0.8调整为通道共享模式, 所以EventLoopGroup中的线程数将是cpu*20,而非默认值或者1*/
    		group = new EpollEventLoopGroup(GlobalConfig.busiThreadCount); 
    		channelClass = EpollSocketChannel.class;
    	} else {
    		logger.info("Using NIO event!");
    		group = new NioEventLoopGroup(GlobalConfig.busiThreadCount);
    	} 
    	
    	Bootstrap b = new Bootstrap();  
        b.group(group).channel(channelClass);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.TCP_NODELAY, true);
        b.option(ChannelOption.SO_REUSEADDR, true);
        b.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000);
        if (ssl) {
        	TrustManagerFactory tf = null; 
            try {
    	        KeyStore keyStore = KeyStore.getInstance("JKS");
    	        keyStore.load(new FileInputStream(GlobalConfig.sslClientCert), GlobalConfig.sslClientKey.toCharArray());
    	        tf = TrustManagerFactory.getInstance("SunX509");
    	        tf.init(keyStore);
    			SslContext sslContext = SslContextBuilder.forClient().trustManager(tf).build();
    			b.handler(new SslChannelInitializer(sslContext,true));
            } catch(Exception e) {
            	logger.error("",e);
            	return null;
            }
        } else {
	        b.handler(new ChannelInitializer<Channel>() {  
	            @Override  
	            protected void initChannel(Channel ch) throws Exception {  
	                ChannelPipeline pipeline = ch.pipeline();  
	                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));  
	                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));  //最大16M                
	                pipeline.addLast("decoder", new StringDecoder(Charset.forName(GlobalConfig.charset)));  
	                pipeline.addLast("encoder", new StringEncoder(Charset.forName(GlobalConfig.charset)));
	                pipeline.addLast(new SpiderClientBusiHandler());
	            }  
	        });
        }
        int cout = groupCount.incrementAndGet();
        clientGroups.add(group);
        logger.debug("一共创建过EventLoopGroup数量: " + cout + ", 当前数量:" + clientGroups.size());
        return b;  
    }
}
