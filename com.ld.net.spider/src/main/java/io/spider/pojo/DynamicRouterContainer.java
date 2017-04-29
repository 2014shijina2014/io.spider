/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class DynamicRouterContainer {
	private static final Map<String,String> dynRoutes = new ConcurrentHashMap<String,String>();
	
	public static void setRoute(String meta,String clusterName) {
		dynRoutes.put(meta, clusterName);
	}
	
	public static String getRoute(String meta) {
		return dynRoutes.get(meta);
	}

	public static Map<String,String> queryRoutes() {
		return dynRoutes;
	}
	
	public static void removeByClusterName(String clusterName) {
		Iterator<Entry<String, String>> it = dynRoutes.entrySet().iterator();
		while(it.hasNext()) {
			Map.Entry<String, String> entry=it.next();
			if(entry.getValue().equals(clusterName)) {
				it.remove();
			}
		}
	}
}
