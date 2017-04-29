/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.listener;

import io.netty.channel.Channel;
import io.spider.BeanManagerHelper;
import io.spider.SpiderRouter;
import io.spider.channel.SocketServerHelper;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.WorkNode;
import io.spider.server.SpiderServerBusiHandler;
import io.spider.worker.ReliableDispatcherThread;

import java.io.File;
import java.text.MessageFormat;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
/**
 * spider 通信中间件
 * 
 * @author zhjh256@163.com 
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderShutdownCleaner {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);

	public static void shutdown() {
		// 1、server停止接收外部请求
		// 2、停止心跳、monitor线程
		logger.info("收到停止spider服务器的请求,进入shutdowning状态.");
		GlobalConfig.status = SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_SHUTDOWNING;
		if(GlobalConfig.shutdownStatus.add("stop")) {
			logger.info("触发停止心跳线程!");
		}
		if(GlobalConfig.shutdownStatus.add("stop")) {
			logger.info("触发停止monitor线程！");
		}
		if(GlobalConfig.reliable) {
			if(GlobalConfig.shutdownStatus.add("stop")) {
				logger.info("触发停止ReliableDispatcher线程！");
			}
		}
		// 3、判断并等待直到各socket channel待处理请求数量是否为0, 判断多路复用通道判断是否为0确保响应已全部发出
		if (GlobalConfig.isServer) {
			ThreadPoolExecutor executor = (ThreadPoolExecutor)SpiderServerBusiHandler.executor;
			while (executor.getQueue().size() != 0) {
				logger.info(MessageFormat.format("业务线程池{0}中还有{1,number,#}个任务,等待处理完成！",executor.getQueue().size()));
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			executor.shutdown();
		}
		while (!SpiderRouter.spiderMultiplex.isEmpty()) {
			logger.warn(MessageFormat.format("服务器中还有{0,number,#}个待返回给客户端的请求！top 10 request as follows: ",SpiderRouter.spiderMultiplex.size()));
			int i=0;
			for(Entry<String, BlockingQueue<String>> entry : SpiderRouter.spiderMultiplex.entrySet()) {
				if(i>=10) {
					break;
				}
				logger.info(MessageFormat.format("key={0},value={1}. value is null only if channel is null(disconnected or not establish).",entry.getKey(),entry.getValue().peek()));
				i++;
			}
			try {
				logger.info("retry in 500ms...");
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		logger.info("开始断开到下游服务器的所有连接...");
		// 4、主动关闭所有下游socket连接,上游的无所谓,因为这个时候监听关闭了,请求都已经返回,客户端再发请求会收到失败的返回值
		for(Cluster cluster : GlobalConfig.getClusters().values()) {
			for(WorkNode workNode : cluster.getWorkNodes().values()) {
				for(Channel channel : workNode.getChannels().values()) {
					try {
						channel.deregister().sync();
						channel.disconnect().sync();
						channel.close().sync();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}	
		}
		logger.info("已断开所有到下游服务器的连接！");
		// 5、停止可信模式下调度请求
		// 6、断开redis连接
		if (GlobalConfig.reliable) {
			logger.info("已经提交的待处理请求数量：" + ((ThreadPoolExecutor) ReliableDispatcherThread.execWorker).getQueue().size());
			ReliableDispatcherThread.execWorker.shutdown();
			logger.info("可信模式请求调度线程已停止！");
			RedisConnectionFactory redisConnectionFactory = null;
			redisConnectionFactory = BeanManagerHelper.getBean("localRedisConnFactory", RedisConnectionFactory.class);
			redisConnectionFactory.getConnection().close();
			
			if(GlobalConfig.ha) {
				redisConnectionFactory = BeanManagerHelper.getBean("remoteRedisConnFactory", RedisConnectionFactory.class);
				redisConnectionFactory.getConnection().close();
			}
		}
		// 7、关闭spider服务
		if (GlobalConfig.isServer) {
			logger.info("开始停止spider服务器...");
			SocketServerHelper.shutdown();
		}
		if(GlobalConfig.checkPidFile) {
			// 8、删除"/tmp/spider/" + GlobalConfig.clusterName + ".pid"文件
			File file = new File("/tmp/spider/" + GlobalConfig.clusterName + ".pid");
			if (file.exists() && file.isFile()) {
				if (!file.delete()) {
					logger.error("/tmp/spider/" + GlobalConfig.clusterName + ".pid文件删除失败！");
				}
			} else {
				logger.error("/tmp/spider/" + GlobalConfig.clusterName + ".pid不存在！");
			}
		}
		logger.info("spider服务器已经干净的停止！");
	}
}
