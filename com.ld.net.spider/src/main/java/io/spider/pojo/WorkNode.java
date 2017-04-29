/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.netty.channel.Channel;
import io.spider.channel.SocketClientHelper;
import io.spider.meta.SpiderOtherMetaConstant;

import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class WorkNode {
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	@JsonIgnore
	private String workNodeName;
	private String address;
	private int port;
	
	private boolean ssl = false;
//	private boolean needLdPackAdapter;
	
//	public boolean isNeedLdPackAdapter() {
//		return needLdPackAdapter;
//	}
//
//	public void setNeedLdPackAdapter(boolean needLdPackAdapter) {
//		this.needLdPackAdapter = needLdPackAdapter;
//	}
	@Deprecated
	private String sslClientCert;
	@Deprecated
	private String sslClientKey;
	
	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public String getSslClientCert() {
		return sslClientCert;
	}

	public void setSslClientCert(String sslClientCert) {
		this.sslClientCert = sslClientCert;
	}

	public String getSslClientKey() {
		return sslClientKey;
	}

	public void setSslClientKey(String sslClientKey) {
		this.sslClientKey = sslClientKey;
	}
	@JsonIgnore
	private Map<String,Channel> channels = new ConcurrentHashMap<String,Channel>();
	@JsonIgnore
	private boolean connected = true;
	@JsonIgnore
	private boolean isSpiderConnected = false;
	
	private final int elapsedMs = 100;
	
	private int connectionSize = 1;
	
	private AtomicInteger connNum = new AtomicInteger(0);
	
	public Map<String,Channel> getChannels() {
		return channels;
	}

	public boolean getConnected() {
		return connected;
	}
	public void setConnected(boolean connected) {
		this.connected = connected;
	}

	public WorkNode() {}
	
	public WorkNode(String address, int port,boolean needLdPackAdapter) {
		this.address = address;
		this.port = port;
//		this.needLdPackAdapter = needLdPackAdapter;
	}
	public WorkNode(String address, int port,boolean needLdPackAdapter, Channel channel) {
		this.address = address;
		this.port = port;
		channels.put(this.getWorkNodeName(), channel);
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	
	public String getWorkNodeName() {
		return this.address + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + this.port;
	}
	
	@JsonIgnore
	public Channel getChannel(String threadName) {
//		if(channels.get(threadName) == null) {
//			return createChannelAndReturn(threadName);
//		} else {
//			if(GlobalConfig.dev) {
//				logger.debug("已获取到通道!");
//			}
//			return channels.get(threadName);
//		}
		int n = RandomUtils.nextInt(connectionSize);
		if(channels.get(this.getWorkNodeName() + "#" + n) == null) {
			return createChannelAndReturn(this.getWorkNodeName() + "#" + n);
		} else {
			if(GlobalConfig.dev) {
				logger.debug("已获取到通道!");
			}
			return channels.get(this.getWorkNodeName() + "#" + n);
		}
	}
	
	/**
	 * @param threadName
	 * @return
	 */
	private Channel createChannelAndReturn(String threadName) {
		logger.warn(MessageFormat.format("尚未为线程{0}建立连接,这通常是该线程第一次调用远程请求或者远程服务器已重启,现在开始连接！",threadName));
		Channel channel = SocketClientHelper.createChannel(this.getAddress(),this.getPort(),this.ssl);
		if(channel == null) {
			logger.error(MessageFormat.format("无法建立到服务器{0}:{1,number,#}的连接！",this.getAddress(),this.getPort()));
			return null;
		}
		channels.put(threadName, channel);
		try {
			logger.info("waiting for " + elapsedMs + " millisecond for spider handshake finish.");
			Thread.sleep(elapsedMs);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return channel;
	}
	
	public void addChannel(String threadName,Channel channel) {
		//this.channels.put(threadName, channel);
		this.channels.put(this.getWorkNodeName() + "#" + connNum.getAndIncrement(), channel);
	}

	public boolean isSpiderConnected() {
		return isSpiderConnected;
	}

	public void setSpiderConnected(boolean isSpiderConnected) {
		this.isSpiderConnected = isSpiderConnected;
	}

	public void setWorkNodeName(String workNodeName) {
		this.workNodeName = workNodeName;
	}

	@Override
	public String toString() {
		return "WorkNode [workNodeName=" + this.getWorkNodeName() + ", address="
				+ address + ", port=" + port + "]";
	}
	/**
	 * @return
	 */
	@JsonIgnore
	public Channel getRandomChannel() {
		for(Channel channel : this.channels.values()) {
			// 广播发送请求给反向注册服务器时, 有可能会连接还没有spider逻辑层面建立
			if(channel.isActive()) {
				return channel;
			}
		}
		return createChannelAndReturn(this.getWorkNodeName() + "#" + RandomUtils.nextInt(connectionSize));
		// return createChannelAndReturn(Thread.currentThread().getName());
	}

	public int getConnectionSize() {
		return connectionSize;
	}

	public void setConnectionSize(int connectionSize) {
		this.connectionSize = connectionSize;
	}
}
