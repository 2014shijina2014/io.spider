/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.worker;

import io.netty.channel.Channel;
import io.spider.SpiderRouter;
import io.spider.channel.SocketClientHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.monitor.service.SpiderMonitorServiceImpl;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.SpiderBaseResp;
import io.spider.sc.pojo.RegisterReq;
import io.spider.sc.pojo.StatReq;
import io.spider.stat.ServiceStatContainer;
import io.spider.utils.AES128Utils;
import io.spider.utils.DateUtils;
import io.spider.utils.JsonUtils;
import io.spider.utils.UUIDUtils;

import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class SpiderMonitorTimer implements Runnable {
	
	static String scIp;
	static int scPort;
	static boolean scSsl = false;
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private Channel channel;
	
	public SpiderMonitorTimer() {
		//判断是否cloud模式,cloud模式才创建到服务中心的连接
		if (GlobalConfig.isCloud) {
			scIp = GlobalConfig.serviceCenterInetAddress.split(SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP)[0];
			scPort = Integer.parseInt(GlobalConfig.serviceCenterInetAddress.split(SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP)[1]);
			/**
			 * 服务中心默认非ssl模式, 服务中心非LdPackAdapter模式
			 */
			channel = SocketClientHelper.createChannel(scIp,scPort,scSsl);
			if(channel == null) {
				logger.warn(MessageFormat.format("无法连接到service-center[{0}:{1,number,#}],请确认配置是否正确以及服务中心是否已启动！否则将无法接收到服务中心推送的消息！",
						scIp,scPort));
			} else {
				SpiderPacketHead head = new SpiderPacketHead();
				head.setServiceId(SpiderServiceIdConstant.SERVICE_ID_REGISTER);
				head.setRpcMsgId(UUIDUtils.uuid());
				RegisterReq req = new RegisterReq();
				req.setClusterName(GlobalConfig.clusterName);
				req.setClientIp(((InetSocketAddress)channel.localAddress()).getHostString());
				req.setClientPort(((InetSocketAddress)channel.localAddress()).getPort());
				req.setMyInfo(SpiderMonitorServiceImpl.getMyInfo());
				String monitorData = JsonUtils.toJson(req);
				if (GlobalConfig.encrypt) {
					monitorData = AES128Utils.aesEncrypt(monitorData);
				}
				try {
					String retJsonStr = SpiderRouter.call(head, monitorData,channel,SpiderOtherMetaConstant.METRICS_REPORT_INTERVAL_MS);
					SpiderBaseResp resp = SpiderRouter.getBody(retJsonStr, SpiderBaseResp.class);
					if(resp.isSuccess()) {
						logger.info("注册到服务中心成功！");
					} else {
						logger.info(MessageFormat.format("注册到服务中心失败。errorNo:{0}, errorInfo:{1}",resp.getErrorNo(),resp.getErrorInfo()));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	@Override
	public void run() {
		Thread.currentThread().setName(SpiderOtherMetaConstant.THREAD_NAME_MONITOR_REPORT);
		while(GlobalConfig.shutdownStatus.poll() == null) {
			try {
				Thread.sleep(SpiderOtherMetaConstant.METRICS_REPORT_INTERVAL_MS);
				if (GlobalConfig.isCloud) {
					if(channel == null || channel.isActive()) {
						channel = SocketClientHelper.createChannel(scIp,scPort,scSsl);
					}
					
					if(channel != null) {
						Thread.sleep(1000); //sleep 1秒确保连接spider逻辑已经成功建立
						logger.info("开始上传本周期性能指标数据...");
						SpiderPacketHead head = new SpiderPacketHead();
						head.setServiceId(SpiderServiceIdConstant.SERVICE_ID_MONITOR);
						head.setRpcMsgId(UUIDUtils.uuid());
						StatReq req = new StatReq(ServiceStatContainer.groupByServiceId(true));
						if(req.getServiceStats().size() == 0) {
							logger.info("本节点所有服务运行次数均为0,无需上传！");
						} else {
							req.setClusterName(GlobalConfig.clusterName);
							req.setClientIp(((InetSocketAddress)channel.localAddress()).getHostString());
							req.setClientPort(((InetSocketAddress)channel.localAddress()).getPort());
							req.setStatTime(DateUtils.SDF_DATETIME.format(new Date()));
							String monitorData = JsonUtils.toJson(req);
							if (GlobalConfig.encrypt) {
								monitorData = AES128Utils.aesEncrypt(monitorData);
							}
							String retJsonStr = SpiderRouter.call(head, monitorData,channel,SpiderOtherMetaConstant.METRICS_REPORT_INTERVAL_MS);
							
							SpiderBaseResp resp = SpiderRouter.getBody(retJsonStr, SpiderBaseResp.class);
							if(resp.isSuccess()) {
								logger.info("上传本周期性能指标数据完成.");
							} else {
								logger.error(MessageFormat.format("上传本周期性能指标数据失败。errorNo:{0}, errorInfo:{1}",resp.getErrorNo(),resp.getErrorInfo()));
							}
						}
					} else {
						logger.warn(MessageFormat.format("无法连接到service-center[{0}:{1,number,#}],请确认配置是否正确以及服务中心是否已启动！否则将无法接收到服务中心推送的消息！",
								scIp,scPort));
					}
				}
				
				if(GlobalConfig.dumpStat) {
					SpiderMonitorServiceImpl.dumpStat();
				}
				if(GlobalConfig.dumpStat || GlobalConfig.isCloud) {
					logger.info("清空缓存中的性能指标！");
					ServiceStatContainer.resetMetrics();
				}
			} catch (SpiderException e) {
				e.printStackTrace();
				try {
					channel.disconnect().sync();
				} catch (InterruptedException e1) {
					//NOP
				} finally {
					channel = null;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		logger.info("收到停止性能监控线程的信号, 线程退出.");
	}
}
