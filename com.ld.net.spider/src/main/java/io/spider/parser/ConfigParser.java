/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.parser;

import io.spider.BeanManagerHelper;
import io.spider.filter.AfterFilter;
import io.spider.filter.BeforeFilter;
import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderEnv;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.plugin.SpiderPluginTemplate;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.WorkNode;
import io.spider.utils.JsonUtils;
import io.spider.utils.NetworkUtils;
import io.spider.utils.StringHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import static io.spider.meta.SpiderOtherMetaConstant.CONFIG_SEPARATOR;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class ConfigParser {
	static List<String> excludeProxyPkgs = new ArrayList<String>();
	static List<String> excludeExportPkgs = new ArrayList<String>();
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static Document load() {
		return load(true);
	}
	
	public static Document load(boolean init) {
		SAXReader sr = new SAXReader();
		File spiderCfgFile = null;
		String configPath = null;
		try {
			configPath = System.getenv(SpiderEnv.SPIDER_CONFIG);
			if (configPath == null) {
				configPath = System.getProperty("spider.config");
			}
			
			if(configPath == null) {
				spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			} else {
				spiderCfgFile = new File(configPath);
			}
			
			if (!init) {
				if (System.currentTimeMillis() - spiderCfgFile.lastModified() > 65000) {
					logger.info("spider.xml最近60秒没有修改过.");
					return null;
				}
			}
			Document doc = sr.read(spiderCfgFile);
			logger.info("loaded config from " + spiderCfgFile.getAbsolutePath() + ".");
			return doc;
		} catch (DocumentException | FileNotFoundException e1) {
			logger.error("cannot find config file from specified by environment variable SPIDER_CONFIG[" + configPath + "] "
					+ "or system property spider.config[" + configPath + "] or classpath:spider.xml");
			e1.printStackTrace();
			System.exit(-1);
		}
		return null;
	}
	
	public static void parse(Document spiderDoc) {
		getLocalName(spiderDoc.getRootElement());
		if(GlobalConfig.checkPidFile) {
			//判断是否有本机器是否有同名节点存在，如果有，则不允许启动
			File file=new File("/tmp/spider/" + GlobalConfig.clusterName + ".pid");    
	    	if(file.exists()) {
				logger.error("spider pid " + file.getAbsolutePath() + " is exist, only one instance of same name spider runtime is permit to run on one server, please change spider.xml's nodeName!");
				System.exit(-1);
	    	} else {
	    		//先判断/tmp/spider是否存在,如果不存在,则创建，否则File.createNewFile会报IOException,这个接口实现太out了
	    		if (!file.getParentFile().exists()) {
	    			file.getParentFile().mkdirs();
	    		}
	    		try {    
			        file.createNewFile();    
			    } catch (IOException e) {    
			        logger.error("spider pid " + file.getAbsolutePath() + " create failed!",e);
			        System.exit(-1);
			    }
	    	}
		}
		parseLocalService(spiderDoc.getRootElement());
		parseRoute(spiderDoc.getRootElement());
		parseChannel(spiderDoc.getRootElement());
		parseFilter(spiderDoc.getRootElement());
		parseCustomPlugin(spiderDoc.getRootElement());
	}
	
	private static void parseRoute(Element rootElement) {
		RouteParser.parse(rootElement);
	}

	private static void parseFilter(Element rootElement) {
		List<Element> plugins = rootElement.element(SpiderConfigName.ELE_PLUGINS).elements(SpiderConfigName.ELE_PLUGIN);
		for(Element plugin : plugins) {
			if(plugin.attribute(SpiderConfigName.ATTR_PLUGIN_ID).getStringValue().equals(SpiderOtherMetaConstant.PLUGIN_ID_FILTER)) {
				List<Element> filters = plugin.elements(SpiderConfigName.ELE_FILTER);
				for(Element filter : filters) {
					try {
						Class clz = ConfigParser.class.getClassLoader().loadClass(filter.getText());
						if (AfterFilter.class.isAssignableFrom(clz)) {
							GlobalConfig.afterFilters.add((AfterFilter)clz.newInstance());
							logger.info("加载AfterFilter实现" + clz.getCanonicalName() + "成功!");
						} else if (BeforeFilter.class.isAssignableFrom(clz)) {
							GlobalConfig.beforeFilters.add((BeforeFilter)clz.newInstance());
							logger.info("加载BeforeFilter实现" + clz.getCanonicalName() + "成功!");
						}
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						logger.warn("loading the implementation of filter [" + filter.getText() + "]failed:",e);
					}
				}
			}
		}
	}
	private static void getLocalName(Element rootElement) {
		Element nodeNameEle = rootElement.element(SpiderConfigName.ELE_NODE_NAME);
		GlobalConfig.dev = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_DEV),"true");
		if (GlobalConfig.dev) {
			logger.warn("spider is running under dev mode, performance will be degrade 50%~95%, please ensure it is disabled in production server.");
		}
		
		GlobalConfig.logOutput = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOG_OUTPUT),GlobalConfig.logOutput);
		
		if(GlobalConfig.logOutput.equals("mongodb")) {
			GlobalConfig.mongoDb = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_MONGO_DB),GlobalConfig.mongoDb);
			GlobalConfig.mongoURI = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_MONGO_URI),GlobalConfig.mongoURI);
			GlobalConfig.slowCol = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SLOW_COL),GlobalConfig.slowCol);
			GlobalConfig.statCol = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_STAT_COL),GlobalConfig.statCol);
		}
		
		GlobalConfig.clusterName = nodeNameEle.attributeValue(SpiderConfigName.ATTR_VALUE);
		GlobalConfig.charset = nodeNameEle.attributeValue(SpiderConfigName.ATTR_CHARSET);
		GlobalConfig.serviceDefineType = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SERVICE_DEFINE_TYPE),"spider");
		GlobalConfig.supportPlainParams = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SUPPORT_PLAIN_PARAMS),"true");
//		GlobalConfig.needLdPackAdapter = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NEED_LDPACK_ADAPTER),"true");
		GlobalConfig.isCloud = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_CLOUD),"true");
		GlobalConfig.logMsgIdPrefix = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOG_MSG_ID_PREFIX),"true");
		GlobalConfig.enableEpoll = StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_ENABLE_EPOLL),"true");
		GlobalConfig.isServiceCenter = StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_ROLE)) ? false : StringHelper.isEqual(nodeNameEle.attributeValue(SpiderConfigName.ATTR_ROLE),SpiderOtherMetaConstant.SPIDER_RT_ROLE_SC);
		if(!GlobalConfig.isServiceCenter) {
			GlobalConfig.role = nodeNameEle.attributeValue(SpiderConfigName.ATTR_ROLE);
		}
		GlobalConfig.serviceCenterInetAddress = nodeNameEle.attributeValue(SpiderConfigName.ATTR_SERVICE_CENTER);
		GlobalConfig.appVersion = StringHelper.ifEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_APP_VERSION),SpiderOtherMetaConstant.DEFAULT_APP_VERSION);
		
		GlobalConfig.dumpStat = StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_DUMPSTAT));
		GlobalConfig.tcpdump = !StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_TCPDUMP));
		String nodeValue = nodeNameEle.attributeValue(SpiderConfigName.ATTR_DETECT_INTERVAL);
		GlobalConfig.detectInterval = StringUtils.isEmpty(nodeValue) ? GlobalConfig.detectInterval : Integer.valueOf(nodeValue);
		GlobalConfig.slowLongTime = StringHelper.parseInt(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SLOW_LONG_TIME),3000);
//		GlobalConfig.broadcastMode = StringHelper.parseInt(nodeNameEle.attributeValue(SpiderConfigName.ATTR_BROADCAST_MODE),1);
		GlobalConfig.needSFtp = !StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NEED_SFTP));
		if(GlobalConfig.needSFtp) {
			GlobalConfig.SFtpPort = StringHelper.parseInt(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SFTP_PORT),22);
			GlobalConfig.SFtpUsername = nodeNameEle.attributeValue(SpiderConfigName.ATTR_SFTP_USERNAME);
			GlobalConfig.SFtpPassword = nodeNameEle.attributeValue(SpiderConfigName.ATTR_SFTP_PASSWORD);
		}
		
		if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NOLOGGING_LIST))) {
			//NOP
		} else {
			String[] noLoggingServices = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NOLOGGING_LIST),CONFIG_SEPARATOR);
			for(int i=0;i<noLoggingServices.length;i++) {
				GlobalConfig.noLoggingList.put(noLoggingServices[i], "");
			}
			logger.info("不记日志服务列表:" + JsonUtils.toJson(GlobalConfig.noLoggingList));
		}
		
		if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOGGING_LIST))) {
			//NOP
		} else {
			String[] loggingServices = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOGGING_LIST),CONFIG_SEPARATOR);
			for(int i=0;i<loggingServices.length;i++) {
				GlobalConfig.loggingList.put(loggingServices[i], "");
			}
			logger.info("记日志服务列表:" + JsonUtils.toJson(GlobalConfig.loggingList));
		}
		
		if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SUPPRESS_ERROR_NO_LIST))) {
			//NOP
		} else {
			String[] suppressErrorNos = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SUPPRESS_ERROR_NO_LIST),CONFIG_SEPARATOR);
			for(int i=0;i<suppressErrorNos.length;i++) {
				GlobalConfig.suppressErrorNoList.put(suppressErrorNos[i], "");
			}
			logger.info("不记日志错误列表" + JsonUtils.toJson(GlobalConfig.suppressErrorNoList));
		}
	}
	private static void parseLocalService(Element rootElement) {
		logger.info("preparing to parse spider local service plugin...");
		
		List<Element> plugins = rootElement.element(SpiderConfigName.ELE_PLUGINS).elements(SpiderConfigName.ELE_PLUGIN);
		for(Element plugin : plugins) {
			if(plugin.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_LOCALSERVICE)) {
				String tmpProxyPkgs = plugin.attributeValue(SpiderConfigName.ATTR_SERVICE_PROXY_PACKAGE);
				GlobalConfig.proxyPackages = tmpProxyPkgs == null ? "" : tmpProxyPkgs.split(":")[0];
				GlobalConfig.proxyServices = tmpProxyPkgs.split(":").length == 2 ? Arrays.asList(StringUtils.tokenizeToStringArray(tmpProxyPkgs.split(":")[1],CONFIG_SEPARATOR)) : GlobalConfig.proxyServices;
				GlobalConfig.excludeProxyPackages = plugin.attributeValue(SpiderConfigName.ATTR_EXCLUDE_PROXY_PACKAGE);
				excludeProxyPkgs = GlobalConfig.excludeProxyPackages == null ? new ArrayList<String>() : Arrays.asList(StringUtils.tokenizeToStringArray(GlobalConfig.excludeProxyPackages,SpiderOtherMetaConstant.CONFIG_SEPARATOR));
				GlobalConfig.compress = StringHelper.isEqual(plugin.attributeValue(SpiderConfigName.ATTR_COMPRESS),"true");
				GlobalConfig.encrypt = StringHelper.isEqual(plugin.attributeValue(SpiderConfigName.ATTR_ENCRYPT),"true");
				GlobalConfig.threadAffinity = StringHelper.isEqual(plugin.attributeValue(SpiderConfigName.ATTR_THREAD_AFFINITY),"true");
				GlobalConfig.timeout = StringHelper.parseInt(plugin.attributeValue(SpiderConfigName.ATTR_SERVICE_TIMEOUT),SpiderOtherMetaConstant.DEFAULT_TIMEOUT_MS);
				GlobalConfig.anonymous = !StringHelper.isEqual(plugin.attributeValue(SpiderConfigName.ATTR_ANONYMOUS),"false");
				
				Element serverEle = plugin.element(SpiderConfigName.ELE_SERVER);
				if(serverEle == null || StringHelper.isEqual(serverEle.attributeValue(SpiderConfigName.ATTR_ENABLE),"false")) {
					logger.info("spider server is disabled, providing client function only...");
					GlobalConfig.isServer = false;
					// 如果是client模式, 则自动使用broadcastMode = 1, 具体原因见spider概要设计文档说明
//					GlobalConfig.broadcastMode = 1;
				} else {
					GlobalConfig.isServer = true;
					GlobalConfig.isSSL = StringHelper.isEqual(plugin.attributeValue(SpiderConfigName.ATTR_SSL), "true");
					if(GlobalConfig.isSSL) {
						GlobalConfig.sslServerCert = StringHelper.ifEmpty(plugin.attributeValue(SpiderConfigName.ATTR_SSL_SERVER_CERT),"");
						GlobalConfig.sslServerKey = StringHelper.ifEmpty(plugin.attributeValue(SpiderConfigName.ATTR_SSL_SERVER_KEY),"");
						if (StringUtils.isEmpty(GlobalConfig.sslServerCert) || StringUtils.isEmpty(GlobalConfig.sslServerKey)) {
							logger.error("ssl模式下服务端证书和加密键不能为空,请检查配置!");
							System.exit(-1);
						}
					}
					GlobalConfig.port = Integer.parseInt(serverEle.attributeValue(SpiderConfigName.ATTR_PORT));
					//currently unsupport
					//GlobalConfig.backlog = Integer.parseInt(plugin.element("server").attributeValue("backlog"));
					GlobalConfig.busiThreadCount = StringHelper.parseInt(serverEle.attributeValue(SpiderConfigName.ATTR_THREAD_COUNT),Runtime.getRuntime().availableProcessors() * 20);
					String tmpExportPkgs = serverEle.attributeValue(SpiderConfigName.ATTR_SERVICE_EXPORT_PACKAGE);
					GlobalConfig.exportPackages = tmpExportPkgs == null ? "" : tmpExportPkgs.split(":")[0];
					GlobalConfig.exportServices = tmpExportPkgs.split(":").length == 2 ? Arrays.asList(StringUtils.tokenizeToStringArray(tmpExportPkgs.split(":")[1],CONFIG_SEPARATOR)) : GlobalConfig.exportServices;
					GlobalConfig.excludeExportPackages = serverEle.attributeValue(SpiderConfigName.ATTR_EXCLUDE_EXPORT_PACKAGE);
					excludeExportPkgs = GlobalConfig.excludeExportPackages == null ? new ArrayList<String>() : Arrays.asList(StringUtils.tokenizeToStringArray(GlobalConfig.excludeExportPackages,CONFIG_SEPARATOR));
					GlobalConfig.ipPrefix = serverEle.attributeValue(SpiderConfigName.ATTR_IP_PREFIX);
					GlobalConfig.reliable = StringHelper.isEqual(serverEle.attributeValue(SpiderConfigName.ATTR_RELIABLE),"true");
					if(GlobalConfig.reliable) {
						GlobalConfig.maxQueueCount = StringHelper.parseInt(serverEle.attributeValue(SpiderConfigName.ATTR_MAX_QUEUE_COUNT), GlobalConfig.maxQueueCount);
						GlobalConfig.ha = StringHelper.isEqual(serverEle.attributeValue(SpiderConfigName.ATTR_HA),"true");
						GlobalConfig.forceRecovery = StringHelper.parseInt(serverEle.attributeValue(SpiderConfigName.ATTR_FORCE_RECOVERY),SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL);
						if(GlobalConfig.ha || GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_FROM_REMOTE) {
							GlobalConfig.haRemoteServerAddress[0] = serverEle.attributeValue(SpiderConfigName.ATTR_HA_REMOTE_SERVER_ADDRESS).split(SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP)[0];
							GlobalConfig.haRemoteServerAddress[1] = serverEle.attributeValue(SpiderConfigName.ATTR_HA_REMOTE_SERVER_ADDRESS).split(SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP)[1];
							GlobalConfig.nodeId = GlobalConfig.clusterName + "_" + NetworkUtils.getIPByPrefix(serverEle.attributeValue(SpiderConfigName.ATTR_IP_PREFIX));
							GlobalConfig.remoteServerPassword = serverEle.attributeValue(SpiderConfigName.ATTR_REMOTE_SERVER_PASSWORD);
						}
					} else {
						GlobalConfig.ha = false;
						GlobalConfig.forceRecovery = 0;
					}
				}
				if (GlobalConfig.timeout == 0) {
					GlobalConfig.timeout = SpiderOtherMetaConstant.DEFAULT_TIMEOUT_MS;
				}
				break;
			}
		}
		logger.info("spider local service plugin " + SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE + " parsed success.");
		
		if (hasIntersection(StringUtils.tokenizeToStringArray(GlobalConfig.exportPackages,CONFIG_SEPARATOR),
							StringUtils.tokenizeToStringArray(GlobalConfig.proxyPackages,CONFIG_SEPARATOR))) {
			logger.error("export service path is overlap with proxy service path, please check the config.");
			System.exit(-1);
		}
		
		if (StringUtils.isEmpty(GlobalConfig.exportPackages) && StringUtils.isEmpty(GlobalConfig.proxyPackages)) {
			logger.error("exportPackages or proxyPackages must at least not empty!");
			System.exit(-1);
		}
		
		if(StringUtils.isEmpty(GlobalConfig.exportPackages)) {
			logger.info("no auto export spider service.");
		} else {
			ClassScanner.createExportService(GlobalConfig.exportPackages);
		}
		/**
		 * 2016年10月31日调整为，即使非云模式下，节点自身的管理服务仍然开放，便于后续将管理功能当做普通服务时更易转变
		 * 只要节点自身不是服务中心即可,否则会导致bean重复的问题
		 */
		if(!GlobalConfig.isServiceCenter) {
			ClassScanner.createExportService("io.spider.sc.client.api");
			//因为这里需要根据条件决定加载的bean,同时不给用户暴露相关接口,所以动态创建bean实现
			BeanManagerHelper.createNodeProxyForManagedServer();
			
			//为了防止意外,仅在非服务中心节点发布广播相关的API,这里自动开放,无需动态创建bean
			if (GlobalConfig.isServer) {
				ClassScanner.createExportService("io.spider.manage.api");
				BeanManagerHelper.createOtherManageBeanForManagedServer();
				
				ClassScanner.createExportService("io.spider.monitor.api");
			}
			BeanManagerHelper.createMonitorServiceForManagedServer();
		}
		
		if(StringUtils.isEmpty(GlobalConfig.proxyPackages)) {
			logger.info("no auto proxy spider service.");
		} else {
			ClassScanner.createProxyService(GlobalConfig.proxyPackages);
		}
		
		//验证配置文件的正确性
		checkConfig();
	}

	private static void checkConfig() {
		if(!GlobalConfig.isServer) {
			GlobalConfig.ha = false;
			GlobalConfig.forceRecovery = SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL;
//			GlobalConfig.needLdPackAdapter = false;
		}
		if(GlobalConfig.ha || GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_FROM_REMOTE) {
			if(StringUtils.isEmpty(GlobalConfig.nodeId.substring(GlobalConfig.nodeId.lastIndexOf("_") + 1))) {
				logger.error("must config ipPrefix in ha mode!");
				System.exit(-1);
			}
			if(StringUtils.isEmpty(GlobalConfig.remoteServerPassword)) {
				logger.error("must set not empty remoteServerPassword in ha mode!");
				System.exit(-1);
			}
		}
	}
	private static boolean hasIntersection(String[] split, String[] split2) {
		Map<String, Boolean> map = new HashMap<String, Boolean>();
		for (String str : split) {
			map.put(str, Boolean.FALSE);
		}
		for (String str : split2) {
			if (map.containsKey(str)) {
				return true;
			}
		}
		return false;
	}
	
	private static void parseChannel(Element rootElement) {
		List<Element> plugins = rootElement.element(SpiderConfigName.ELE_PLUGINS).elements(SpiderConfigName.ELE_PLUGIN);
		for(Element plugin : plugins) {
			if(plugin.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_CHANNEL)) {
				// 加载客户端SSL仓库配置
				GlobalConfig.sslClientCert = StringHelper.ifEmpty(plugin.attributeValue(SpiderConfigName.ATTR_SSL_CLIENT_CERT),"");
				GlobalConfig.sslClientKey = StringHelper.ifEmpty(plugin.attributeValue(SpiderConfigName.ATTR_SSL_CLIENT_KEY),"");
				
				List<Element> clusters = plugin.elements(SpiderConfigName.ELE_CLUSTER);
				for(Element cluster : clusters) {
					Cluster cl = new Cluster();
					cl.setClusterName(cluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME));
//					cl.setNeedLdPackAdapter(StringHelper.isEqual(cluster.attributeValue(SpiderConfigName.ATTR_NEED_LDPACK_ADAPTER),"true"));
					cl.setLbType(StringHelper.parseInt(cluster.attributeValue(SpiderConfigName.ATTR_LB_TYPE),1));
					if(cl.getLbType() == 2) {
						String fields = cluster.attributeValue(SpiderConfigName.ATTR_FIELDS);
						cl.setFields(fields == null ? new ArrayList<String>() : Arrays.asList(StringUtils.tokenizeToStringArray(fields, CONFIG_SEPARATOR)));
					}
					GlobalConfig.addCluster(cl);
//					if (!StringUtils.isEmpty(cluster.attributeValue(SpiderConfigName.ATTR_REVERSE_REGISTER))) {
//						GlobalConfig.getCluster(cluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME)).setNeedReverseRegister(true);
//					}
					List<Element> workNodes = cluster.elements(SpiderConfigName.ELE_WORK_NODE);
					for(Element workNode : workNodes) {
						WorkNode workNod = new WorkNode();
						workNod.setAddress(workNode.attributeValue(SpiderConfigName.ATTR_ADDRESS)); 
						workNod.setPort(Integer.parseInt(workNode.attributeValue(SpiderConfigName.ATTR_PORT)));
						workNod.setConnectionSize(StringHelper.parseInt(workNode.attributeValue(SpiderConfigName.ATTR_CONNECTION_SIZE),1));
						workNod.setSsl(StringHelper.isEqual(workNode.attributeValue(SpiderConfigName.ATTR_SSL),"true"));
						if(workNod.isSsl()) {
							if (StringUtils.isEmpty(GlobalConfig.sslClientCert) || StringUtils.isEmpty(GlobalConfig.sslClientKey)) {
								logger.error("ssl模式下客户端证书和加密键不能为空,请检查配置!");
								System.exit(-1);
							}
						}
//						workNod.setNeedLdPackAdapter(StringHelper.isEqual(workNode.attributeValue(SpiderConfigName.ATTR_NEED_LDPACK_ADAPTER),"true"));
						GlobalConfig.addWorkNode(cluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME), workNod);
					}
				}
			}
		}
	}
	
	private static void parseCustomPlugin(Element rootElement) {
		List<Element> plugins = rootElement.element(SpiderConfigName.ELE_PLUGINS).elements(SpiderConfigName.ELE_PLUGIN);
		for(Element plugin : plugins) {
			if(plugin.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_CUSTOM)) {
				List<Element> customPlugins = plugin.elements(SpiderConfigName.ELE_CUSTOM_PLUGIN);
				for(Element customPlugin : customPlugins) {
					String pluginName = customPlugin.attributeValue(SpiderConfigName.ATTR_NAME);
					String className = customPlugin.attributeValue(SpiderConfigName.ATTR_CLASS);
					logger.info("loading custom plugin: " + pluginName);
					try {
						Class clz = ConfigParser.class.getClassLoader().loadClass(className);
						if(SpiderPluginTemplate.class.isAssignableFrom(clz)) {
							SpiderPluginTemplate spiderPlugin = (SpiderPluginTemplate) clz.newInstance();
							spiderPlugin.setPluginName(pluginName);
							spiderPlugin.doParse(customPlugin);
							GlobalConfig.plugins.put(pluginName, spiderPlugin);
							logger.info("loaded custom plugin[" + pluginName + "].");
						}
					} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
						logger.error("cannot find the implementation class of plugin " + className + ", or init failed, please ensure it is extends SpiderPluginTemplate");
						logger.error("system exit.");
						System.exit(-1);
					}
				}
			}
		}
	}
}
