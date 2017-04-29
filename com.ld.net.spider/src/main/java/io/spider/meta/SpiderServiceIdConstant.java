/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import java.util.HashSet;
import java.util.Set;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderServiceIdConstant {
	public static final String SPIDER_INTERNAL_SERVICE_ID_PREFIX = "000000";
	
	public static final String SERVICE_ID_HEARTBEAT = "00000021    ";
	public static final String SERVICE_ID_AUTH = "00000020    ";
	public static final String SERVICE_ID_PROTOCOL = "00000019    ";
	public static final String SERVICE_ID_MONITOR = "00000018    ";
	public static final String SERVICE_ID_INTERCEPT = "00000025    ";
	public static final String SERVICE_ID_REGISTER = "00000017    ";
	public static final String SERVICE_ID_ADD_WORKNODE = "00000008    ";
	public static final String SERVICE_ID_ADD_CLUSTER = "00000009    ";
	public static final String SERVICE_ID_REMOVE_WORKNODE = "00000012    ";
	public static final String SERVICE_ID_REMOVE_CLUSTER = "00000013    ";
	public static final String SERVICE_ID_ADD_ROUTEITEM = "00000010    ";
	public static final String SERVICE_ID_EXECUTE_SHELL = "00000029    ";
	public static final String SERVICE_ID_SEND_FILE = "00000030    ";
	public static final String SERVICE_ID_STOP_INTERCEPT = "00000032    ";
	public static final String SERVICE_ID_INTERCEPT_PUSH = "00000033    ";

	public static final String SERVICE_ID_BROADCAST_QUERY_NODE_INFO = "00000034    ";
//	public static final String SERVICE_ID_REVERSE_REGISTER = "00000035    ";
	public static final String SERVICE_ID_INTERCEPT_PULL = "00000037    ";
	
	public static final Set<String> internalServices = new HashSet<String>();
	static {
		internalServices.add(SERVICE_ID_HEARTBEAT);
		internalServices.add(SERVICE_ID_AUTH);
		internalServices.add(SERVICE_ID_INTERCEPT_PULL);
		internalServices.add(SERVICE_ID_PROTOCOL);
		internalServices.add(SERVICE_ID_INTERCEPT);
		internalServices.add(SERVICE_ID_STOP_INTERCEPT);
//		internalServices.add(SERVICE_ID_REVERSE_REGISTER);
	}
	
	public static final Set<String> internalClientServices = new HashSet<String>();
	static {
		internalClientServices.add(SERVICE_ID_PROTOCOL);
		internalClientServices.add(SERVICE_ID_AUTH);
	}
}
