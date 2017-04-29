/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.netty.channel.Channel;
import io.spider.meta.SpiderPacketHead;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static io.spider.meta.SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class Cluster {
	
	Map<String,Method> getters = new HashMap<String,Method>();
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private String clusterName;
	
	/**
	 * 负载均衡模式
	 * 1: 轮训
	 * 2: 根据报文头中的字段进行hash
	 */
	private int lbType = 1;
	
//	private boolean needLdPackAdapter = false;
	
	private List<String> fields = new ArrayList<String>();
	
	public List<String> getFields() {
		return fields;
	}

	public void setFields(List<String> fields) {
		this.fields = fields;
	}

	@JsonIgnore
	private boolean isDisconnected = false;
	
	private Map<String,WorkNode> workNodes = new ConcurrentHashMap<String,WorkNode>();
	@JsonIgnore
	private Random random = new Random();
	@JsonIgnore
	String[] keys;
	@JsonIgnore
//	private boolean needReverseRegister = false;
	public Map<String,WorkNode> getWorkNodes() {
		return workNodes;
	}
	
	public Cluster() {}
	
	public Cluster(String clusterName) {
		this.clusterName = clusterName;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
	
	public void addWorkNode(WorkNode member) {
		workNodes.put(member.getWorkNodeName(), member);
		keys = workNodes.keySet().toArray(new String[0]);
	}
	
	//仅用于验证客户端有效性,因为不进行考虑回调,所以不需要存储实际的channel引用
	public void addWorkNode(String address,int port,boolean needLdPackAdapter) {
		workNodes.put(address + ADDRESS_AND_PORT_SEP + port,new WorkNode(address,port,needLdPackAdapter));
		keys = workNodes.keySet().toArray(new String[0]);
	}
	
	public WorkNode removeWorkNode(String address,int port) {
		WorkNode workNode = workNodes.remove(address + ADDRESS_AND_PORT_SEP + port);
		keys = workNodes.keySet().toArray(new String[0]);
		return workNode;
	}
	
	public int getWorkNodeCount() {
		return this.workNodes.size();
	}
	public boolean isDisconnected() {
		return isDisconnected;
	}
	public void setDisconnected(boolean isDisconnected) {
		this.isDisconnected = isDisconnected;
	}

	public Channel getRandomConn(SpiderPacketHead packetHead, String threadName) {
		if(this.lbType == 1) {
			int i = 0;
			while (i < keys.length) {
				WorkNode randomValue = workNodes.get(keys[random.nextInt(keys.length)]);
				if(randomValue.getConnected()) {
					logger.debug("为[" + threadName + "]获取连接成功.");
					return randomValue.getChannel(threadName);
				}
				i++;
			}
			logger.warn(MessageFormat.format("{0}下有{1}个服务器节点,但是均无法建立连接！",clusterName,keys.length));
			return null;
		} else {
			String values = "";
			// request stick 模式
			for(String field : fields) {
				String value = packetHead.getSpiderOpts().get(field);
				if(value == null) {
					try {
						Object val = getters.get(field).invoke(packetHead, null);
						value = (val == null ? "" : val.toString());
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						logger.warn("报文头中找不到字段" + field + ".");
					}
					value = (value == null ? "" : value);
				}
				values = values + value;
			}
			int n = values.hashCode() % keys.length;
			WorkNode randomValue = workNodes.get(keys[n]);
			if(randomValue.getConnected()) {
				logger.debug("为[" + threadName + "]获取连接成功.");
				return randomValue.getChannel(threadName);
			}
			logger.warn(MessageFormat.format("无法建立到{0}的连接！",clusterName));
			return null;
		}
	}

//	public boolean isNeedReverseRegister() {
//		return needReverseRegister;
//	}
//
//	public void setNeedReverseRegister(boolean needReverseRegister) {
//		this.needReverseRegister = needReverseRegister;
//	}

	public int getLbType() {
		return lbType;
	}

	public void setLbType(int lbType) {
		this.lbType = lbType;
		logger.info("Cluster[" + clusterName + "]的负载均衡模式=" + lbType + ".");
	}

//	public boolean isNeedLdPackAdapter() {
//		return needLdPackAdapter;
//	}
//
//	public void setNeedLdPackAdapter(boolean needLdPackAdapter) {
//		this.needLdPackAdapter = needLdPackAdapter;
//	}
}
