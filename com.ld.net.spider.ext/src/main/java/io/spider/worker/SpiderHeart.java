/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.worker;

import io.netty.channel.Channel;
import io.spider.SpiderRouter;
import io.spider.channel.SocketClientHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.WorkNode;
import io.spider.utils.AES128Utils;
import io.spider.utils.UUIDUtils;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.spider.pojo.GlobalConfig.detectInterval;

/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderHeart implements Runnable {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	@Override
	public void run() {
		Thread.currentThread().setName(SpiderOtherMetaConstant.THREAD_NAME_HEARTBEAT_THREAD);
		while(GlobalConfig.shutdownStatus.poll() == null) {
			try {
				Thread.sleep(detectInterval);
				if(GlobalConfig.getClusters().size() > 0) {
					logger.debug("spider heartbeat detect with interval=" + detectInterval/1000 + "s...");
					int totalValidChannels = 0;
					for(Cluster cluster : GlobalConfig.getClusters().values()) {
						for(WorkNode workNode : cluster.getWorkNodes().values()) {
							boolean disconnected = false;
							if(workNode.getChannels().size() == 0) {
								logger.warn(workNode.toString() + "没有任何通道,应是反向注册后第一次心跳或者与服务器断开过,尝试建立第一个连接！");
								Channel channel = SocketClientHelper.createChannel(workNode.getAddress(),workNode.getPort(),workNode.isSsl());
								if(channel != null) {
									workNode.addChannel(Thread.currentThread().getName(), channel);
									workNode.setConnected(true);
								} else {
									logger.error("cannot connect to server [" + workNode.getWorkNodeName() + "], retry after " + detectInterval/1000 + " seconds.");
								}
								continue;
							}
							totalValidChannels = totalValidChannels + workNode.getChannels().size();
							logger.debug(workNode.toString() + "下共有有效连接数" + workNode.getChannels().size() + ".");
							for(Entry<String, Channel> channelEntry : workNode.getChannels().entrySet()) {
								if(channelEntry.getValue() == null || channelEntry.getKey() == null) { //理论上不可能发生
									logger.warn(workNode.toString() + "尚未建立连接,尝试自动建立连接！注意：这种情况理论上不可能发生！");
									Channel channel = SocketClientHelper.createChannel(workNode.getAddress(),workNode.getPort(),workNode.isSsl());
									if(channel != null) {
										workNode.addChannel(channelEntry.getKey(), channel);
										workNode.setConnected(true);
									} else {
										logger.error("cannot connect to server [" + workNode.getWorkNodeName() + "] for " + channelEntry.getKey() + ", retry after " + detectInterval/1000 + " seconds.");
									}
								} else {
									SpiderPacketHead head = new SpiderPacketHead();
									head.setServiceId(SpiderServiceIdConstant.SERVICE_ID_HEARTBEAT);
									head.setRpcMsgId(UUIDUtils.uuid());
									String msgBody = SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HEARTBEAT;
									if (GlobalConfig.encrypt) {
										msgBody = AES128Utils.aesEncrypt(msgBody);
									}
									try {
										String retJsonStr = SpiderRouter.call(head, msgBody,workNode.getChannel(channelEntry.getKey()),detectInterval);
										if(retJsonStr == null) {
											workNode.setConnected(false);
											logger.error("disconnected from server [" + workNode.getWorkNodeName() + "] for " + channelEntry.getKey() + ",retry now...");
											Channel channel = SocketClientHelper.createChannel(workNode.getAddress(),workNode.getPort(),workNode.isSsl());
											if(channel != null) {
												workNode.addChannel(channelEntry.getKey(), channel);
												workNode.setConnected(true);
											} else {
												logger.error("cannot connect to server [" + workNode.getWorkNodeName() + "] for " + channelEntry.getKey() + ", retry after " + detectInterval/1000 + " seconds.");
											}
										} else {
											logger.debug("connected to server [" + workNode.getWorkNodeName() + "] for " + channelEntry.getKey() + " successfully.");
										}
									} catch (SpiderException e) {
										if(e.getErrorNo().equals(SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER)) {
											disconnected = true;
											break;
										}
									} catch (NullPointerException e) {
										logger.error("连接已经失效,准备清空到本节点的所有通道",e);
										disconnected = true;
										break;
									}
								}
							}
							if(disconnected) {
								logger.warn(cluster.getClusterName() + "下到节点" + workNode.toString() + "的连接已经断开,断开并清空其下所有通道以最小化影响！");
								workNode.getChannels().clear();
								//Iterator<Entry<String, Channel>> it = workNode.getChannels().entrySet().iterator();
								//while(it.hasNext()) {
									//Entry<String, Channel> entry = it.next();
									//if (entry.getValue() != null) {
									//	entry.getValue().disconnect().sync();
									//	entry.getValue().close().sync();
									//}
								//	it.remove();
								//}
								logger.warn(cluster.getClusterName() + "下到节点" + workNode.toString() + "的连接已经全部释放！");
							}
						}
					}
					logger.debug("当前共有连接数: " + SocketClientHelper.clientGroups.size() + ",有效连接数: " + totalValidChannels + ", 相差10以内(或者有效连接>=全部连接)一般为误差, 属于正常, 否则属于不正常!");
				}
			} catch (Exception e) {
				logger.error("心跳线程发生异常：",e);
			}
		}
		logger.info("收到停止心跳线程的信号, 线程退出.");
	}
}
