/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.monitor.service;

import io.spider.SpiderRouter;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.monitor.api.SpiderMonitorService;
import io.spider.monitor.pojo.BusiThreadInfo;
import io.spider.monitor.pojo.DynRouteCache;
import io.spider.monitor.pojo.Metric;
import io.spider.pojo.Cluster;
import io.spider.pojo.DynamicRouterContainer;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.RouteItem;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.pojo.SourceWorkNode;
import io.spider.pojo.SpiderRequest;
import io.spider.sc.pojo.MyInfo;
import io.spider.server.SpiderServerBusiHandler;
import io.spider.server.SpiderServerDispatcher;
import io.spider.stat.ServiceStat;
import io.spider.stat.ServiceStatContainer;
import io.spider.utils.DateUtils;
import io.spider.utils.Obj2MapUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

@Service
public class SpiderMonitorServiceImpl implements SpiderMonitorService {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	static MongoClient mongoClient;
//	static MongoDatabase database;
//	static MongoCollection<Document> collection;
	static DB database;
	static DBCollection collection;
	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryRoutes()
	 */
	@Override
	public List<RouteItem> queryRoutes() {
		return GlobalConfig.routeItems;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryClients()
	 */
	@Override
	public List<SourceWorkNode> queryClients() {
		List<SourceWorkNode> clients = new ArrayList<SourceWorkNode>();
		for(SourceWorkNode node : GlobalConfig.querySourceWorkNodes().values()) {
			clients.add(node);
		}
		return clients;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryExports()
	 */
	@Override
	public List<ServiceDefinition> queryExports() {
		List<ServiceDefinition> exportServices = new ArrayList<ServiceDefinition>();
		for (ServiceDefinition serviceDef : ServiceDefinitionContainer.getAllService().values()) {
			if(serviceDef.isExport()) {
				exportServices.add(serviceDef);
			}
		}
		return exportServices;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryProxies()
	 */
	@Override
	public List<ServiceDefinition> queryProxies() {
		List<ServiceDefinition> proxyServices = new ArrayList<ServiceDefinition>();
		for (ServiceDefinition serviceDef : ServiceDefinitionContainer.getAllService().values()) {
			if(!serviceDef.isExport()) {
				proxyServices.add(serviceDef);
			}
		}
		return proxyServices;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryMyinfo()
	 */
	@Override
	public MyInfo queryMyinfo() {
		return getMyInfo();
	}
	
	public static MyInfo getMyInfo() {
		MyInfo myInfo = new MyInfo();
		myInfo.setAnonymous(GlobalConfig.anonymous);
		myInfo.setAppVersion(GlobalConfig.appVersion);
		myInfo.setBusiThreadCount(GlobalConfig.busiThreadCount);
		myInfo.setCloud(GlobalConfig.isCloud);
		myInfo.setCompress(GlobalConfig.compress);
		myInfo.setDynamicRouteEnable(GlobalConfig.isDynamicRouteEnable);
		myInfo.setEncrypt(GlobalConfig.encrypt);
		myInfo.setForceRecovery(GlobalConfig.forceRecovery);
		myInfo.setHa(GlobalConfig.ha);
		myInfo.setHaRemoteServerAddress(GlobalConfig.haRemoteServerAddress[0] + SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP + GlobalConfig.haRemoteServerAddress[1]);
		myInfo.setMaxQueueCount(GlobalConfig.maxQueueCount);
		myInfo.setReliable(GlobalConfig.reliable);
		myInfo.setNodeId(GlobalConfig.nodeId);
		myInfo.setClusterName(GlobalConfig.clusterName);
		myInfo.setServer(GlobalConfig.isServer);
		myInfo.setServiceCenter(GlobalConfig.isServiceCenter);
		myInfo.setPort(GlobalConfig.port);
		myInfo.setServiceCenterInetAddress(GlobalConfig.serviceCenterInetAddress);
		myInfo.setStatus(GlobalConfig.status);
		myInfo.setTimeout(GlobalConfig.timeout);
		myInfo.setTcpDump(GlobalConfig.tcpdump);
		myInfo.setDumpStat(GlobalConfig.dumpStat);
		myInfo.setSlowLongTime(GlobalConfig.slowLongTime);
		myInfo.setSSHPort(GlobalConfig.SFtpPort);
		myInfo.setSSHUsername(GlobalConfig.SFtpUsername);
		myInfo.setSSHPassword(GlobalConfig.SFtpPassword);
		
		return myInfo;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryBusiThreadInfo()
	 */
	@Override
	public BusiThreadInfo queryBusiThreadInfo() {
		int pendingTasks = -1;
		int runnable = -1;
		int blocked = -1;
		int waiting = -1;
		int timedWait = -1;
		int news = -1;
		int maxConcurrent = -1;
		if(GlobalConfig.isServer) {
			ThreadPoolExecutor executor = (ThreadPoolExecutor)SpiderServerBusiHandler.executor;
			runnable = executor.getActiveCount();
			maxConcurrent = executor.getLargestPoolSize();
			pendingTasks = executor.getQueue().size();
		}
		BusiThreadInfo threadInfo = new BusiThreadInfo();
		threadInfo.setBlockedThreadCount(blocked);
		threadInfo.setRunnableThreadCount(runnable);
		threadInfo.setTimedWaitThreadCount(timedWait);
		threadInfo.setMaxConcurrent(maxConcurrent);
		threadInfo.setWaitingThreadCount(waiting);
		threadInfo.setPendingTaskCount(pendingTasks);
		threadInfo.setNewThreadCount(news);
		return threadInfo;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryDynRouteCaches()
	 */
	@Override
	public List<DynRouteCache> queryDynRouteCaches() {
		List<DynRouteCache> dynRouteCaches = new ArrayList<DynRouteCache>();
		for (Entry<String,String> entry : DynamicRouterContainer.queryRoutes().entrySet()) {
			DynRouteCache dynRoute = new DynRouteCache(entry.getKey(),entry.getValue());
			dynRouteCaches.add(dynRoute);
		}
		return dynRouteCaches;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryClusters()
	 */
	@Override
	public List<Cluster> queryClusters() {
		List<Cluster> clusters = new ArrayList<Cluster>();
		for(Cluster cluster : GlobalConfig.getClusters().values()) {
			clusters.add(cluster);
		}
		return clusters;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryMetrics()
	 */
	@Override
	public List<Metric> queryMetrics() {
		return queryMetricsInternal();
	}
	
	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#resetMetrics()
	 */
	@Override
	public void resetMetrics() {
		ServiceStatContainer.resetMetrics();
	}
	
	private static List<Metric> queryMetricsInternal() {
		List<Metric> metrics = new ArrayList<Metric>();
		for(ServiceStat stat : ServiceStatContainer.groupByServiceId()) {
			Metric metric = new Metric();
			metric.setServiceId(stat.getServiceId());
			metric.setCount(stat.getExecCount());
			metric.setTotalElapsedTime(Math.round(stat.getElapsedMsTime()/1000));
			metric.setAvgElapsedMilliTime(stat.getExecCount() == 0 ? 0 : Math.round(stat.getElapsedMsTime()/stat.getExecCount()));
			metric.setMaxElapsedMilliTime(Math.round(stat.getMaxMsTime()));
			metric.setMinElapsedMilliTime(Math.round(stat.getMinMsTime()));
			
			if(metric.getCount() > 0) {
				metrics.add(metric);
			}
		}
		return metrics;
	}

	/* (non-Javadoc)
	 * @see com.ld.net.spider.monitor.service.SpiderMonitorService#queryQueuedRequests()
	 */
	@Override
	public List<SpiderRequest> queryReliableQueuedRequests() {
		List<SpiderRequest> requests = new ArrayList<SpiderRequest>();
		Object[] objs = GlobalConfig.requestQueues.toArray();
		for(int i=0;i<objs.length;i++) {
			requests.add((SpiderRequest) objs[i]);
		}
		return requests;
	}

	public synchronized static void dumpStat() {
		if (GlobalConfig.logOutput.equals(SpiderOtherMetaConstant.LOG_OUTPUT_FILE)) {
			String logDir = System.getenv("SPIDER_LOG");
			if (logDir == null) {
				File dir = new File("/tmp/spider/stat/" + GlobalConfig.clusterName);
				logDir = dir.getAbsolutePath();
				if (!dir.exists() && !dir.isDirectory()) {
					dir.mkdirs();
				}
			}
			
			File file = new File(logDir + "/" + DateUtils.SDF_DATE.format(new Date()) + ".log");
			if (!file.exists()) {
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			try {
				BufferedWriter output = new BufferedWriter(new FileWriter(file,true));
				output.write("snapshot time:" + DateUtils.SDF_DATETIME.format(new Date()) + 
						",All unfinished request count:" + SpiderServerBusiHandler.allRequests.size() +
						",In Executing Request Count(only for route):" + SpiderRouter.spiderMultiplex.size() + 
						",Current active thread count:" + ((ThreadPoolExecutor)SpiderServerBusiHandler.executor).getActiveCount() +
						System.lineSeparator());
				output.write(StringUtils.leftPad("serviceId",12)+","+
							StringUtils.leftPad("count",10)+","+
							StringUtils.leftPad("total(s)",10)+","+
							StringUtils.leftPad("avg(ms)",10)+","+
							StringUtils.leftPad("max(ms)",10)+","+
							StringUtils.leftPad("min(ms)",10) + System.lineSeparator());
		        for (Metric metric : queryMetricsInternal()) {
					output.write(StringUtils.rightPad(metric.getServiceId(), 12) + "," + 
							StringUtils.leftPad(String.valueOf(metric.getCount()),10) + "," + 
							StringUtils.leftPad(String.valueOf(metric.getTotalElapsedTime()),10) + "," + 
							StringUtils.leftPad(String.valueOf(metric.getAvgElapsedMilliTime()),10) + "," + 
							StringUtils.leftPad(String.valueOf(metric.getMaxElapsedMilliTime()),10) + "," + 
							StringUtils.leftPad(String.valueOf(metric.getMinElapsedMilliTime()),10) + System.lineSeparator());  
				}
		        output.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
//			List<Document> docs = new ArrayList<Document>();
//			docs.add(new Document("appName",GlobalConfig.clusterName)
//							.append("host",SpiderServerDispatcher.localAddress)
//							.append("snapTime",DateUtils.SDF_DATETIME_NUM.format(new Date()))
//							.append("All unfinished request count",SpiderServerBusiHandler.allRequests.size())
//							.append("In Executing Request Count(only for route)", SpiderRouter.spiderMultiplex.size())
//							.append("Current active thread count", ((ThreadPoolExecutor)SpiderServerBusiHandler.executor).getActiveCount()));
//			for (Metric metric : queryMetricsInternal()) {
//				docs.add(new Document(Obj2MapUtil.convert2Map(metric)));
//			}
//			if (mongoClient == null) {
//				mongoClient = new MongoClient(new MongoClientURI("mongodb://" + GlobalConfig.mongoURI));
//				logger.info("已建立到mongodb://" + GlobalConfig.mongoURI + "的连接.");
//			}
//			if (database == null) {
//				database = mongoClient.getDatabase(GlobalConfig.mongoDb);	
//			}
//			if (collection == null) {
//				collection = database.getCollection(GlobalConfig.statCol);
//			}
//			collection.insertMany(docs);
			if (mongoClient == null) {
				try {
					MongoClientOptions.Builder builder = MongoClientOptions.builder().maxWaitTime(100);
					mongoClient = new MongoClient(new MongoClientURI("mongodb://" + GlobalConfig.mongoURI,builder));
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				logger.info("已建立到mongodb://" + GlobalConfig.mongoURI + "的连接.");
			}
			if (database == null) {
				database = mongoClient.getDB(GlobalConfig.mongoDb);
			}
			if (collection == null) {
				collection = database.getCollection(GlobalConfig.slowCol);
			}
			BulkWriteOperation builder = collection.initializeOrderedBulkOperation();
			builder.insert(new BasicDBObject("appName",GlobalConfig.clusterName)
						.append("host",SpiderServerDispatcher.localAddress)
						.append("snapTime",DateUtils.SDF_DATETIME_NUM.format(new Date()))
						.append("All unfinished request count",SpiderServerBusiHandler.allRequests.size())
						.append("In Executing Request Count(only for route)", SpiderRouter.spiderMultiplex.size())
						.append("Current active thread count", ((ThreadPoolExecutor)SpiderServerBusiHandler.executor).getActiveCount()));
			for (Metric metric : queryMetricsInternal()) {
				builder.insert(new BasicDBObject(Obj2MapUtil.convert2Map(metric)));
			}
			builder.execute();
		}
	}
}
