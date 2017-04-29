/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.monitor.pojo;

public class DynRouteCache {
	private String routeKey;
	private String clusterName;
	public DynRouteCache(String key, String value) {
		this.routeKey = key;
		this.clusterName = value;
	}
	public String getRouteKey() {
		return routeKey;
	}
	public void setRouteKey(String routeKey) {
		this.routeKey = routeKey;
	}
	public String getClusterName() {
		return clusterName;
	}
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}
}
