/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.netty.channel.Channel;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class SourceWorkNode extends WorkNode {

	public SourceWorkNode(String address, int port, Channel channel) {
		super(address, port,false,channel);
	}
}
