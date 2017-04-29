/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.listener;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.ContextLoaderListener;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderContextLoaderListener extends ContextLoaderListener {
	@Override
	public void contextDestroyed(ServletContextEvent event) {
		//接收到停止JVM、web容器（两者机制不同）的请求,开始停止spider服务,过程公用在SpiderShutdownCleaner中.
		SpiderShutdownCleaner.shutdown();
		super.contextDestroyed(event);
	}
}
