/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.worker;

import io.spider.listener.SpiderShutdownCleaner;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.SpiderRequest;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ForceRecoverDispatcherThread implements Runnable {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);

	@Override
	public void run() {
		ExecutorService execWorker = Executors.newFixedThreadPool(GlobalConfig.busiThreadCount);
		logger.info("共有[" + GlobalConfig.requestQueues.size() + "]条待处理请求！");
		while (GlobalConfig.requestQueues.peek() != null) {
			SpiderRequest spiderRequest;
			spiderRequest = GlobalConfig.requestQueues.poll();
			if(GlobalConfig.dev) {
				logger.info("可信模式下调度请求：" + spiderRequest.toString());
			}
			Callable<Boolean> reliableWorkerThread = new ReliableWorkerThread(spiderRequest);
			execWorker.submit(reliableWorkerThread);
		}
		logger.info("强制恢复模式下所有待处理请求已全部执行完成,系统开始进入关闭流程！");
		SpiderShutdownCleaner.shutdown();
		System.exit(0);
	}
}
