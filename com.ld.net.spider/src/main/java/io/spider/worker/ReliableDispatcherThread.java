/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.worker;

import io.spider.pojo.GlobalConfig;
import io.spider.pojo.SpiderRequest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ReliableDispatcherThread implements Runnable {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);

	private static int sleepFactor = 1;
	private static final int maxSleepInMilliSeconds = 5000;
	
	public static ExecutorService execWorker;
	
	@Override
	public void run() {
		execWorker = Executors.newFixedThreadPool(GlobalConfig.busiThreadCount);
		while (GlobalConfig.shutdownStatus.poll() == null) {
			if(((ThreadPoolExecutor) execWorker).getActiveCount() < ((ThreadPoolExecutor) execWorker).getCorePoolSize()) {
				SpiderRequest spiderRequest;
				try {
					sleepFactor = 1;
					spiderRequest = GlobalConfig.requestQueues.take();
					if(GlobalConfig.dev) {
						logger.info("可信模式下调度请求：" + spiderRequest.toString());
					}
					Callable<Boolean> reliableWorkerThread = new ReliableWorkerThread(spiderRequest);
					execWorker.submit(reliableWorkerThread);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			} else {
				try {
					Thread.sleep(sleepFactor);
					sleepFactor = Math.min(maxSleepInMilliSeconds, sleepFactor * 2);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		
		logger.info("收到停止spider服务器的进程, 开始停止" + execWorker.toString() + "线程！");
		execWorker.shutdown();
		logger.info("可信调度线程已停止！");
	}
}
