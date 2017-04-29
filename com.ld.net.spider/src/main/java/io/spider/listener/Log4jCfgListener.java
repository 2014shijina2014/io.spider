/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.listener;

import javax.servlet.ServletContextEvent;

import org.springframework.web.util.Log4jConfigListener;

/**
 * spider 通信中间件
 * 
 * @author zhjh256@163.com 
 * {@link} http://www.cnblogs.com/zhjh256
 */
@SuppressWarnings("deprecation")
public class Log4jCfgListener extends Log4jConfigListener {
	public static final String log4jdirkey = "log4jdir";
	public static final String HOSTNAME = "hostname";
	public static final String APPNAME = "appname";

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * javax.servlet.ServletContextListener#contextInitialized(javax.servlet
	 * .ServletContextEvent)
	 */
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		super.contextInitialized(sce);
//		String log4jdir = sce.getServletContext().getRealPath("/");
//		System.out.println("==========应用程序部署目录" + log4jdir + "==========");
//		System.out.println((log4jdir.startsWith("/home/ldtrader/spider/deploy") ? ""
//						: "规范要求程序部署目录为/home/ldtrader/spider/deploy，为了符合规范，请按规范进行部署！"));
//		InetAddress netAddress = null;
//		try {
//			netAddress = InetAddress.getLocalHost();
//		} catch (UnknownHostException e) {
//			throw new RuntimeException();
//		}
//		if (null == netAddress) {
//			throw new RuntimeException();
//		}
//		String hostname = netAddress.getHostName(); // get the host address
//		System.setProperty(log4jdirkey, "/home/ldtrader/spider/log/" + GlobalConfig.clusterName + "/" + hostname);
//		System.setProperty(HOSTNAME, hostname);
//		System.setProperty(APPNAME, GlobalConfig.clusterName);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.
	 * ServletContextEvent)
	 */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		super.contextDestroyed(sce);
		System.getProperties().remove(log4jdirkey);
	}
}
