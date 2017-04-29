/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.listener;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 仅用于非web容器模式,web容器模式运行参见SpiderContextLoaderListener
 */
public class SpiderStandaloneShutdownHook extends Thread {
    
    @Override  
    public void run() {
    	// 接收到停止JVM、web容器（两者机制不同）的请求,开始停止spider服务,过程公用在SpiderShutdownCleaner中.
    	SpiderShutdownCleaner.shutdown();
    }
}
