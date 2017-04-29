/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public final class SpiderOtherMetaConstant {
	public static final String NODE_NAME_LOCALSERVICE = "spider.localService";
	
	/**
	 * 配置文件中插件名称
	 */
	public static final String PLUGIN_ID_LOCALSERVICE = "spider.localService";
	public static final String PLUGIN_ID_CHANNEL = "spider.channel";
	public static final String PLUGIN_ID_FILTER = "spider.filter";
	public static final String PLUGIN_ID_CUSTOM = "spider.customPlugin";
	
	public static final String SPIDER_RT_ROLE_NP = "np";
	public static final String SPIDER_RT_ROLE_SC = "sc";
	public static final String SPIDER_RT_ROLE_NB = "nb";
	public static final String SPIDER_RT_ROLE_CLIENT = "client";
	
	public static final String LOG_OUTPUT_FILE = "file";
	public static final String LOG_OUTPUT_MONGODB = "mongodb";
	
	public static final int DEFAULT_TIMEOUT_MS = 300000;
	public static final int HEARTBEAT_INTERVAL_MS = 60000;
	public static final int METRICS_REPORT_INTERVAL_MS = 300000;
	public static final int SLOW_LONG_TIME_MS = 300;
	public static final int RELIABLE_MAX_QUEUE_COUNT = 300;
	
	public static final String CONFIG_SEPARATOR = ";,";
	public static final String SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP = ";";
	public static final String CLUSTERNAME_AND_ADDRESS_SEP = ";";
	public static final String ADDRESS_AND_PORT_SEP = ":";
	public static final String BIZ_ERROR_NO_AND_INFO_SEP = "&~";
	
	public static final String SPIDER_INTERNAL_MSG_HEARTBEAT = "drpcpqq";
	public static final String SPIDER_INTERNAL_MSG_HANDSHAKE1_MSG = "spider";
	public static final String SPIDER_INTERNAL_MSG_HANDSHAKE_ACK = "ack";
	public static final String SPIDER_INTERNAL_MSG_HANDSHAKE2_AUTH = "auth";
	
	public static final int DISPATCHER_RET_CODE_RETURN = 0;
	public static final int DISPATCHER_RET_CODE_CLOSE_CONNECTION = -1;
	public static final int DISPATCHER_RET_CODE_RETURN_AND_CLOSE = -2;
	public static final int DISPATCHER_RET_CODE_2SERVER_AUTH_PASS = 2;
	public static final int DISPATCHER_RET_CODE_NOP = 1;
	/**
	 * 这个返回码正常情况下不会发生,除非主程序发生未捕获的运行时异常
	 */
	public static final int DISPATCHER_RET_CODE_FATAL = 3;
	/**
	 * 插件处理器容器返回值, 正常往后执行
	 */
	public static final int DISPATCHER_RET_CODE_GO_ON = 4;
	/**
	 * 插件处理器容器返回值, 正常往后,但并行处理
	 */
	public static final int DISPATCHER_RET_CODE_PARALLEL = 5;
	public static final int DISPATCHER_RET_CODE_PRE_CHECK_FOR_OLD_OK = 10;
	/**
	 * 插件处理器内部返回值, 发给下一个插件处理, 作为DISPATCHER_RET_CODE的子集
	 */
	public static final int PLUGIN_RET_CODE_NEXT_PLUGIN = 6;
	
	/**
	 * 插件处理器内部返回值, 发给指定插件处理, 作为DISPATCHER_RET_CODE的子集
	 */
	public static final int PLUGIN_RET_CODE_SPECIFIED_PLUGIN = 7;
	
	/**
	 * 报文类型1:请求;2:响应;3:广播;
	 */
	public static final char MSG_TYPE_RESP = '2'; 
	public static final char MSG_TYPE_REQ = '1';
	public static final char MSG_TYPE_BROADCAST = '3';
	
	/**
	 * 请求类别 1:业务请求; 0:spider内部管理请求
	 */
	public static final char REQ_TYPE_BIZ = '1'; 
	public static final char REQ_TYPE_SPIDER = '0';

	public static final String DEFAULT_SYSTEM_ID = "qq";

	public static final String DEFAULT_APP_VERSION = "qq.qq.qq";

	public static final String DEFAULT_COMPANY_ID = "qqqqqq";
	
	public static final char DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR = ' ';

	public static final String NOTHING = "";
	public static final String SERVICE_DEFINE_TYPE_LD = "ld";
	
	public static final String BEAN_NAME_LOCAL_REDIS_TEMPLATE = "localRedisTemplate";
	public static final String BEAN_NAME_REMOTE_REDIS_TEMPLATE = "remoteRedisTemplate";
	/**
	 * 可信请求处理状态,INIT:未处理;FIN:完成;
	 */
	public static final String REQ_PROCESS_STATUS_INIT = "INIT";
	public static final String REQ_PROCESS_STATUS_FINISH = "FIN";
	
	/**
	 * spider内核运行状态
	 */
	public static final int SPIDER_KERNEL_STATUS_FORCE_RECOVER = -1;
	public static final int SPIDER_KERNEL_STATUS_SHUTDOWNING = 0;
	public static final int SPIDER_KERNEL_STATUS_STARTING = 1;
	public static final int SPIDER_KERNEL_STATUS_STARTED = 2;
	
	/**
	 * 可信模式spider启动级别
	 */
	public static final int SPIDER_RECOVER_LEVEL_NORMAL = 0;
	public static final int SPIDER_RECOVER_LEVEL_FROM_REMOTE = 1;

	/**
	 * 线程名称
	 */
	public static final String THREAD_NAME_RELIABLE_DISPATCHER_THREAD = "spider-reliable-dispatch";
	public static final String THREAD_NAME_HEARTBEAT_THREAD = "spider-heartbeat";
	public static final String THREAD_NAME_MONITOR_REPORT = "spider-metrics-uploader";
	public static final String THREAD_NAME_SPIDER_BUSI_GROUP = "spider-busi-group";
	public static final String THREAD_NAME_SPIDER_WORKER_GROUP = "spider-io-group";

	public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String DATETIMEMS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
	
	public static final String DATETIME_FORMAT_NUM = "yyyyMMddHHmmss";
	public static final String DATE_FORMAT_NUM = "yyyyMMdd";
	public static final String DATETIMEMS_FORMAT_NUM = "yyyyMMddHHmmssSSS";

	public static final String TCP_DUMP_MODE_PUSH = "push";
	public static final String TCP_DUMP_MODE_PULL = "pull";

	public static final String SPIDER_AUTO_PROXY_SERVICE_PREFIX = "AutoProxy.";

	public static String HOSTNAME = null;
	
	static {
		InetAddress addr;
		try {
			addr = InetAddress.getLocalHost();
			HOSTNAME = addr.getHostName();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
}
