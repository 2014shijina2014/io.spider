package io.spider.channel;

import java.nio.charset.Charset;

import javax.net.ssl.SSLEngine;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.spider.client.SpiderClientBusiHandler;
import io.spider.server.SpiderServerBusiHandler;

public class SslChannelInitializer extends ChannelInitializer<Channel> {
	private final SslContext context;
	private final boolean clientMode;

	public SslChannelInitializer(SslContext context,boolean clientMode) {
		this.context = context;
		this.clientMode = clientMode;
	}

	@Override
	protected void initChannel(Channel ch) throws Exception {
		SSLEngine engine = context.newEngine(ch.alloc());
		engine.setUseClientMode(clientMode);
		ch.pipeline().addFirst("ssl", new SslHandler(engine));
		ChannelPipeline pipeline = ch.pipeline(); 
        pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));  
        pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));  //最大16M                
        pipeline.addLast("decoder", new StringDecoder(Charset.forName("UTF-8")));  
        pipeline.addLast("encoder", new StringEncoder(Charset.forName("UTF-8")));
        if (clientMode) {
        	pipeline.addLast(new SpiderClientBusiHandler());
        } else {
        	pipeline.addLast(new SpiderServerBusiHandler());
        }
	}
}
