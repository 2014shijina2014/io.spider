/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.channel;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
//采用默认自定义线程组
//import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.server.SpiderServerBusiHandler;
import io.spider.worker.DefaultThreadFactory;

import java.io.FileInputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.concurrent.ThreadFactory;

import javax.net.ssl.KeyManagerFactory;

import net.openhft.affinity.AffinityStrategies;
import net.openhft.affinity.AffinityThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SocketServerHelper {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	//默认cpu核心数*2,也是netty的默认值,定义在 MultithreadEventLoopGroup.DEFAULT_EVENT_LOOP_THREADS中
	private static int WORKER_GROUP_SIZE = Runtime.getRuntime().availableProcessors() * 2; 
    /** 
     * NioEventLoopGroup实际上就是个线程池, 
     * NioEventLoopGroup在后台启动了n个NioEventLoop来处理Channel事件, 
     * 每一个NioEventLoop负责处理m个Channel, 
     * NioEventLoopGroup从NioEventLoop数组里挨个取出NioEventLoop来处理Channel 
     */  
    private static EventLoopGroup bossGroup; 
    private static EventLoopGroup workerGroup;  
    /**
     * 下个小版本考虑增加业务线程池的实现方式, 服务端方式解决netty原生一个channel最多只能利用一个busi channel的缺陷, 当前版本通过客户端multi channel的方式实现
     * 注：20161116测试, 调整为服务端线程池模式后, 性能没有提升, 反而有所下降, 故中短期内仍然采用这种模式, 如果tomcat作为web端的话是完全没有问题的
     */

    // 默认创建一个cpu核心数*20个线程的线程组来处理耗时的业务逻辑
    // 1.0.8 改用业务自定义线程池代替netty封装的接口
    public static EventExecutorGroup busiGroup;
    
    private static Class<? extends ServerChannel> channelClass;
    
    public static void startSpiderServer() throws Exception {
    	ThreadFactory threadFactory = null;
    	if (GlobalConfig.threadAffinity) {
    		threadFactory = new AffinityThreadFactory(SpiderOtherMetaConstant.THREAD_NAME_SPIDER_WORKER_GROUP, AffinityStrategies.DIFFERENT_CORE);
    	} else {
    		threadFactory = new DefaultThreadFactory(SpiderOtherMetaConstant.THREAD_NAME_SPIDER_WORKER_GROUP);
    	}
    	ServerBootstrap b = new ServerBootstrap();
    	b.childOption(ChannelOption.TCP_NODELAY, true)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.SO_REUSEADDR, true)     //重用地址
        .childOption(ChannelOption.ALLOCATOR, new PooledByteBufAllocator(false))// heap buf's better 4.1开始默认heap而非direct
        .childOption(ChannelOption.SO_RCVBUF, 1048576)
        .childOption(ChannelOption.SO_SNDBUF, 1048576);
    	if(Epoll.isAvailable()) {
			logger.info("epoll is enabled!");
			bossGroup = new EpollEventLoopGroup(1);
	    	workerGroup = new EpollEventLoopGroup(WORKER_GROUP_SIZE,threadFactory);
	    	channelClass = EpollServerSocketChannel.class;
		} else {
			logger.info("epoll is disabled!");
			bossGroup = new NioEventLoopGroup(1);
	    	workerGroup = new NioEventLoopGroup(WORKER_GROUP_SIZE,threadFactory);
	    	channelClass = NioServerSocketChannel.class;
		}
    	logger.info("workerGroup size:" + WORKER_GROUP_SIZE);
    	// 1.0.8 改用业务自定义线程池代替netty封装的接口
//    	if(GlobalConfig.needLdPackAdapter) {
//	    	if(GlobalConfig.threadAffinity) {
//	    		busiGroup = new DefaultEventExecutorGroup(GlobalConfig.busiThreadCount,new AffinityThreadFactory(SpiderOtherMetaConstant.THREAD_NAME_SPIDER_BUSI_GROUP,AffinityStrategies.DIFFERENT_CORE));
//	    	} else {
//	    		busiGroup = new DefaultEventExecutorGroup(GlobalConfig.busiThreadCount,new DefaultThreadFactory(SpiderOtherMetaConstant.THREAD_NAME_SPIDER_BUSI_GROUP));
//	    	}
//    	}
//    	logger.info("busiGroup size:" + GlobalConfig.busiThreadCount);
    	logger.info("preparing to start spider server...");
        b.group(bossGroup, workerGroup);  
        b.channel(channelClass);
        if (GlobalConfig.isSSL) {
        	KeyManagerFactory keyManagerFactory = null;
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream(GlobalConfig.sslServerCert), GlobalConfig.sslServerKey.toCharArray());
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            keyManagerFactory.init(keyStore,GlobalConfig.sslServerKey.toCharArray());
    		SslContext sslContext = SslContextBuilder.forServer(keyManagerFactory).build();
    		b.childHandler(new SslChannelInitializer(sslContext,false));
        } else {
	        b.childHandler(new ChannelInitializer<SocketChannel>() {  
	        	@Override  
	            public void initChannel(SocketChannel ch) throws Exception {
	                ChannelPipeline pipeline = ch.pipeline();  
	                pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));  
	                pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));
                	pipeline.addLast("decoder", new StringDecoder(Charset.forName(GlobalConfig.charset)));  
                	pipeline.addLast("encoder", new StringEncoder(Charset.forName(GlobalConfig.charset)));
                	// 1.0.8版本改为在SpiderServerHandler中调度, busiGroup可能就不要了
                	pipeline.addLast(/*busiGroup,*/new SpiderServerBusiHandler());
	            }  
	        }); 
        }
        //监听本地端口，不要指定ip，否则如果一台机器有多个IP时，通过其他IP连接将会被拒绝“由于目标计算机积极拒绝，无法连接。 127.0.0.1:8888”
        b.bind(GlobalConfig.port).sync();  
        logger.info("spider server start sucess, listening on port " + GlobalConfig.port + ".");  
    }  
      
    public static void shutdown() {  
    	logger.debug("preparing to shutdown spider server...");
    	bossGroup.shutdownGracefully();
    	busiGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();  
        logger.debug("spider server is shutdown.");
    }
}
