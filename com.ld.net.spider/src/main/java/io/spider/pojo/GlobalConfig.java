/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.netty.channel.Channel;
import io.spider.exception.SpiderException;
import io.spider.filter.AfterFilter;
import io.spider.filter.BeforeFilter;
import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.plugin.SpiderPluginTemplate;
import io.spider.utils.DateUtils;
import io.spider.utils.JsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.springframework.util.ResourceUtils;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 注：volatile只保证后面启动的线程能够得到最新值,在启动后所做的变更不能保证被线程观察到
 */
public class GlobalConfig {
	public static ConcurrentLinkedQueue<String> interceptQueues = new ConcurrentLinkedQueue<>();
	
	public static boolean logMsgIdPrefix = false;
	public static String logOutput = SpiderOtherMetaConstant.LOG_OUTPUT_FILE;
	public static String mongoURI = "localhost:27017";
	public static String mongoDb = "spider";
	public static String statCol = "stat";
	public static String slowCol = "slow_request";
	
	/**
	 * 1.0.6 RC版本修改为默认启用
	 */
	public static boolean consistent = true;
	public static int maxQueueCount = SpiderOtherMetaConstant.RELIABLE_MAX_QUEUE_COUNT;
	public static LinkedBlockingQueue<SpiderRequest> requestQueues = new LinkedBlockingQueue<SpiderRequest>(maxQueueCount);
	/**
	 * @see io.spider.meta.SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_xxx
	 */
	public static volatile int status = SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_SHUTDOWNING;
	public static volatile String clusterName;
	public static volatile boolean isServer;
	public static volatile int port;
	public static volatile int timeout; //ms
	// 2.0.0 开始支持 sessionTimeout控制
	public static volatile int sessionTimeout; //s 默认-1, 无超时时间
	public static volatile boolean compress = false;
	public static volatile boolean supportPlainParams = false;
	public static volatile boolean isDynamicRouteEnable = false;
	public static volatile List<RouteItem> routeItems = new CopyOnWriteArrayList<RouteItem>();
	public static volatile Map<Channel,CopyOnWriteArrayList<RouteItemForTcpDump>> tcpDumpClients = new ConcurrentHashMap<Channel,CopyOnWriteArrayList<RouteItemForTcpDump>>();
	public static volatile boolean encrypt = false;
	
	private static volatile Map<String,Cluster> clusters = new ConcurrentHashMap<String,Cluster>();
	private static volatile Map<String,SourceWorkNode> clientConnections = new ConcurrentHashMap<String,SourceWorkNode>();
	public static volatile boolean anonymous;
	/** 业务处理线程大小*/  
    public static volatile int busiThreadCount = Runtime.getRuntime().availableProcessors() * 20;
	public static volatile boolean isCloud = true;
	public static volatile boolean isServiceCenter = false;
	public static volatile String serviceCenterInetAddress; //ip:port
	public static boolean reliable = false;
	public static String appVersion = "";
	public static boolean ha = true;
	public static String nodeId; //用于在reliable模式下保存在远程存储使用 nodeName + IP
	public static String[] haRemoteServerAddress = new String[2];
	public static int forceRecovery = SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL;
	public static volatile String remoteServerPassword = "";
	public static String charset = "UTF-8";
	public static String serviceDefineType;
//	public static boolean needLdPackAdapter = false;
	public static boolean dev = false;
	public static boolean dumpStat = true;
	public static int slowLongTime = SpiderOtherMetaConstant.SLOW_LONG_TIME_MS; //ms
	public static boolean tcpdump = false;
	public static int detectInterval = SpiderOtherMetaConstant.HEARTBEAT_INTERVAL_MS;
	
	public static List<BeforeFilter> beforeFilters = new ArrayList<BeforeFilter>();
	public static List<AfterFilter> afterFilters = new ArrayList<AfterFilter>();
	public static boolean needSFtp;
	public static int SFtpPort;
	public static String SFtpUsername;
	public static String SFtpPassword;
	public static ConcurrentLinkedQueue<String> shutdownStatus = new ConcurrentLinkedQueue<String>();
	public static String role = SpiderOtherMetaConstant.SPIDER_RT_ROLE_NP;
	public static String tcpDumpMode = SpiderOtherMetaConstant.TCP_DUMP_MODE_PULL; //pull: 客户端主动拉的模式; push: 服务端推的模式
//	public static int broadcastMode = 1;
	public static String proxyPackages = "";
	public static List<String> proxyServices = new ArrayList<String>();
	public static String excludeProxyPackages = "";
	public static String exportPackages = "";
	public static String excludeExportPackages = "";
	public static List<String> exportServices = new ArrayList<String>();
	
	/**
	 * 1.0.9-RELEASE 新增
	 */
	public static boolean isSSL = false;
	public static String sslServerCert = "";
	public static String sslServerKey = "";
	
	
	
	/**
	 * 1.0.7-RELEASE 新增
	 */
	public static Map<String,String> noLoggingList = new ConcurrentHashMap<String,String>();
	/**
	 * 1.0.8-RELEASE 新增
	 */
	public static Map<String,String> loggingList = new ConcurrentHashMap<String,String>();
	/**
	 * 1.0.7-RELEASE 新增
	 */
	public static Map<String,String> suppressErrorNoList = new ConcurrentHashMap<String,String>();
	/**
	 * 1.0.7-RELEASE 新增
	 */
	public static boolean threadAffinity = false;
	public static boolean checkPidFile = false;
	public static boolean enableEpoll = false;
	public static String ipPrefix = "";
	public static String sslClientCert;
	public static String sslClientKey;
	
	public static final ConcurrentHashMap<String,SpiderPluginTemplate> plugins = new ConcurrentHashMap<String,SpiderPluginTemplate>();
	
	public static Map<String,Cluster> getClusters() {
		return clusters;
	}
	
	public static void addCluster(String clusterName) {
		clusters.put(clusterName,new Cluster(clusterName));
	}
	
	public static void addCluster(Cluster cluster) {
		clusters.put(cluster.getClusterName(),cluster);
	}
	
	public static Cluster getCluster(String clusterName) {
		return clusters.get(clusterName);
	}
	
	/**
	 * 1.0.9 有时候存在遗漏了配置目标路由指向的cluster, 所以先判断目标节点是否存在, 再取成员就不会出现空指针异常
	 * @param packetHead 
	 * @param clusterName
	 * @param threadName
	 * @return
	 */
	public static Channel getRandomConn(SpiderPacketHead packetHead, String clusterName,String threadName) {
		if (clusters.get(clusterName) != null) {
			return clusters.get(clusterName).getRandomConn(packetHead,threadName);
		}
		throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_CLUSTER_NOT_EXIST,MessageFormat.format(SpiderErrorNoConstant.ERROR_INFO_CLUSTER_NOT_EXIST,clusterName));
	}
	
	public static void removeWorkNode(String clusterName,String address,int port) {
		clusters.get(clusterName).removeWorkNode(address, port);
		if(clusters.get(clusterName).getWorkNodeCount() == 0) {
			clusters.get(clusterName).setDisconnected(true);
		}
	}
	
	public static void addWorkNode(String clusterName,WorkNode workNode) {
		clusters.get(clusterName).addWorkNode(workNode);
		clusters.get(clusterName).setDisconnected(false);
	}
	
	public static void addWorkNode(String clusterName,String address,int port,boolean needLdPackAdapter) {
		clusters.get(clusterName).addWorkNode(address, port,needLdPackAdapter);
		clusters.get(clusterName).setDisconnected(false);
	}
	
	public static void addSourceWorkNode(String address,int port, Channel channel) {
		clientConnections.put(address + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + port, new SourceWorkNode(address,port,channel));
	}
	public static void removeSourceWorkNode(String address,int port) {
		clientConnections.remove(address + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + port);
	}
	public static boolean existsSourceWorkNode(String address,int port) {
		return clientConnections.containsKey(address + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + port);
	}
	
	public static Map<String,SourceWorkNode> querySourceWorkNodes() {
		return clientConnections;
	}

	public static void addInterceptClient(Channel channel,String routesJsonStr) {
		CopyOnWriteArrayList<RouteItemForTcpDump> routes = JsonUtils.json2ConcurrentListAppointed(routesJsonStr, RouteItemForTcpDump.class);
		if(routes == null || routes.size() == 0){
			return;
		}
		tcpDumpClients.put(channel, routes);
	}

	public static SpiderBaseResp removeCluster(String clusterName) {
		
		try {
			File spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			String path = spiderCfgFile.getAbsolutePath() + "." + DateUtils.SDF_DATETIME_NUM.format(new Date());
			SAXReader sr = new SAXReader();
			Document spiderDoc = sr.read(spiderCfgFile); 
	        List list = spiderDoc.selectNodes("spider/" + SpiderConfigName.ELE_PLUGINS + "/" + SpiderConfigName.ELE_PLUGIN);
	        Iterator iter = list.iterator();
	        while(iter.hasNext()){
	            Element elem = (Element)iter.next();
	            if (elem.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_CHANNEL)) {
	            	Iterator iterCluster = elem.elements().iterator();
	            	while(iterCluster.hasNext()){
	            		Element elemCluster = (Element)iterCluster.next();
	            		if (elemCluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME).equals(clusterName)) {
	            			iterCluster.remove();
	            		}
	            	}
				}
	        }
	        
	        FileWriter newFile = new FileWriter(new File(path));
            XMLWriter newWriter = new XMLWriter(newFile);
            newWriter.write(spiderDoc);
            newWriter.close();
            
            spiderCfgFile.delete();
            File oldfile=new File(path); 
            File newfile=new File(path.substring(0, path.lastIndexOf(".")));
            oldfile.renameTo(newfile);
		} catch (DocumentException | IOException e1) {
			e1.printStackTrace();
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SPIDER_XML_LOAD_FAILED);
		}
		
		for (WorkNode workNode : GlobalConfig.getCluster(clusterName).getWorkNodes().values()) {
			try {
				for(Channel channel : workNode.getChannels().values()) {
					channel.disconnect().sync();
				}
			} catch (InterruptedException e) {
			}
		}
		GlobalConfig.clusters.remove(clusterName);
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
	}

	public static Channel getSourceWorkNode(String clusterName, String ip, int port) {
		if(GlobalConfig.clientConnections.get(clusterName) == null) {
			return null;
		}
		return GlobalConfig.clientConnections.get(clusterName).getChannel(ip + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + port);
	}

	/**
	 * @param channel
	 * @return
	 */
	public static boolean isInterceptExist(Channel channel) {
		Iterator<Channel> iter = GlobalConfig.tcpDumpClients.keySet().iterator();
		while(iter.hasNext()) {
			Channel tmpChannel = iter.next();
			if(((InetSocketAddress)tmpChannel.remoteAddress()).toString().equals(((InetSocketAddress)channel.remoteAddress()).toString())) {
				return true;
			}	
		}
		return false;
	}

	/**
	 * @param host
	 * @param port
	 * @return
	 */
//	public static boolean isClusterNeedReverseRegister(String host, int port) {
//		for(Entry<String, Cluster> entry : clusters.entrySet()) {
//			for(String workNode : entry.getValue().getWorkNodes().keySet()) {
//				if(workNode.equals(host + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEPARATOR + port)) {
//					return entry.getValue().isNeedReverseRegister();
//				}
//			}
//		}
//		return false;
//	}

	/**
	 * @param host
	 * @param port
	 * @return
	 */
	public static Cluster getClusterBySocketAddress(String host, int port) {
		for(Entry<String, Cluster> entry : clusters.entrySet()) {
			for(String workNode : entry.getValue().getWorkNodes().keySet()) {
				if(workNode.equals(host + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + port)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}
}
