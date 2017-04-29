/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.io.File;
import java.io.StringWriter;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class VelocityUtils {
	static VelocityEngine velocityEngine;
	
	static final Logger logger = LoggerFactory.getLogger(VelocityUtils.class);
	
	public static void setJarPath(HttpServletRequest request) {
		if(velocityEngine == null) {
			Properties properties=new Properties();
			//设置velocity资源加载方式为jar
	        properties.setProperty("resource.loader", "jar");
	        //设置velocity资源加载方式为file时的处理类
	        properties.setProperty("jar.resource.loader.class", "org.apache.velocity.runtime.resource.loader.JarResourceLoader");
	        String path = request.getSession().getServletContext().getRealPath("/").replace('\\', '/');
	        File file = new File(path + "WEB-INF/lib/"); 
	        File[] tempFile = file.listFiles();
	        String spiderExtJarName = "";
	        for(int i = 0; i < tempFile.length; i++){ 
	        	if(tempFile[i].getName().indexOf("spider.ext")>=0 && tempFile[i].getName().endsWith(".jar")) {
	        		spiderExtJarName = tempFile[i].getName();
	        	}
	        }
	        
	        if(spiderExtJarName.equals("")) {
	        	throw new RuntimeException(path + "下找不到spider.ext jar");
	        }
	        
	        //设置jar包所在的位置
	        properties.setProperty("jar.resource.loader.path", "jar:file:" + path + "WEB-INF/lib/" + spiderExtJarName);
	        //实例化一个VelocityEngine对象
	        velocityEngine=new VelocityEngine(properties);
		}
	}
	public static String mergeTemplate(HttpServletRequest request,String templateName, VelocityContext context) {
		setJarPath(request);
		StringWriter writer=new StringWriter();
		velocityEngine.mergeTemplate(templateName, "utf-8", context, writer);
		logger.debug(writer.toString());
		return writer.toString();
	}
}
