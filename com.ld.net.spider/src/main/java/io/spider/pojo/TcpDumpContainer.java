package io.spider.pojo;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class TcpDumpContainer {
	public static final Map<Channel,LinkedBlockingQueue<String>> tcpDumps = new ConcurrentHashMap<Channel,LinkedBlockingQueue<String>>();
}
