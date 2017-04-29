/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.monitor.action;

import io.spider.SpiderRouter;
import io.spider.exception.SpiderException;
import io.spider.manage.api.OtherManage;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.monitor.api.SpiderMonitorService;
import io.spider.monitor.pojo.BusiThreadInfo;
import io.spider.monitor.pojo.DynRouteCache;
import io.spider.monitor.pojo.Metric;
import io.spider.monitor.service.SpiderMonitorServiceImpl;
import io.spider.mx.SpiderManageServiceImpl;
import io.spider.pojo.BroadcastResult;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.RouteItem;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.SourceWorkNode;
import io.spider.pojo.SpiderBaseReq;
import io.spider.pojo.SpiderBaseResp;
import io.spider.pojo.SpiderRequest;
import io.spider.sc.pojo.ClusterReq;
import io.spider.sc.pojo.MyInfo;
import io.spider.sc.pojo.RouteItemReq;
import io.spider.sc.pojo.WorkNodeReq;
import io.spider.server.SpiderServerAuthServiceImpl;
import io.spider.utils.VelocityUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Controller
@RequestMapping(value="/spider")
public class SpiderMonitorAction {
	@Autowired
	private SpiderMonitorService spiderMonitorService;
	
	@Autowired
	private SpiderManageServiceImpl spiderManageServiceImpl;
	
	@Autowired
	private OtherManage otherManage;
	
	@RequestMapping(value="/spider.html",method=RequestMethod.GET)
	public @ResponseBody String index(HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		context.put("isServiceCenter", GlobalConfig.isServiceCenter);
		context.put("isServer", GlobalConfig.isServer);
		context.put("isCloud", GlobalConfig.isCloud);
		return VelocityUtils.mergeTemplate(request,"vm/spider.vm",context);
	}
	
	@RequestMapping(value="/routes.html",method=RequestMethod.GET)
	public @ResponseBody String queryRoutes(HttpServletRequest request) {
		List<RouteItem> routes = spiderMonitorService.queryRoutes();
		VelocityContext context = new VelocityContext();
		context.put("routes", routes);
		return VelocityUtils.mergeTemplate(request,"vm/routes.vm",context);
	}
	
	@RequestMapping(value="/route/delete.html",method=RequestMethod.POST)
	public @ResponseBody String deleteRoute(@RequestParam String clusterName,HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if (GlobalConfig.isCloud) {
			context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
			context.put("errorInfo", SpiderErrorNoConstant.ERROR_INFO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
		} else {
			SpiderBaseResp resp = spiderManageServiceImpl.removeRouteItem(clusterName);
			context.put("errorNo", resp.getErrorNo());
			context.put("errorInfo", resp.getErrorInfo());
		}
		return VelocityUtils.mergeTemplate(request,"vm/route/delete.vm",context);
	}
	
	@RequestMapping(value="/route/add.html")
	public @ResponseBody String addRoute(RouteItemReq req,HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().equals("GET")) {
			//NOP
		} else {
			if (GlobalConfig.isCloud) {
				context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
				context.put("errorInfo", SpiderErrorNoConstant.ERROR_INFO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
			} else {
				SpiderBaseResp resp = spiderManageServiceImpl.addRouteItem(req);
				context.put("errorNo", resp.getErrorNo());
				context.put("errorInfo", resp.getErrorInfo());
			}
		}
		return VelocityUtils.mergeTemplate(request,"vm/route/add.vm",context);
	}
	
	@RequestMapping(value="/clusters.html",method=RequestMethod.GET)
	public @ResponseBody String queryClusters(HttpServletRequest request) {
		List<Cluster> clusters = spiderMonitorService.queryClusters();
		VelocityContext context = new VelocityContext();
		context.put("clusters", clusters);
		return VelocityUtils.mergeTemplate(request,"vm/clusters.vm",context);
	}
	
	@RequestMapping(value="/cluster/delete.html",method=RequestMethod.POST)
	public @ResponseBody String deleteCluster(@RequestParam String clusterName,HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if (GlobalConfig.isCloud) {
			context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
		} else {
			SpiderBaseResp resp = spiderManageServiceImpl.removeCluster(clusterName);
			context.put("errorNo", resp.getErrorNo());
			context.put("errorInfo", resp.getErrorInfo());
		}
		return VelocityUtils.mergeTemplate(request,"vm/cluster/delete.vm",context);
	}
	
	@RequestMapping(value="/cluster/add.html")
	public @ResponseBody String addCluster(WorkNodeReq req,HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().equals("GET")) {
			//NOP
		} else {
			if (GlobalConfig.isCloud) {
				context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
			} else {
				ClusterReq clusterReq = new ClusterReq();
				clusterReq.setCluster(new Cluster(req.getClusterName()));
				clusterReq.getCluster().addWorkNode(req.getIp(), req.getPort(),false);
				SpiderBaseResp resp = spiderManageServiceImpl.addCluster(clusterReq);
				context.put("errorNo", resp.getErrorNo());
				context.put("errorInfo", resp.getErrorInfo());
			}
		}
		return VelocityUtils.mergeTemplate(request,"vm/cluster/add.vm",context);
	}
	
	@RequestMapping(value="/worknode/delete.html",method=RequestMethod.POST)
	public @ResponseBody String deleteWorknode(WorkNodeReq req,
												HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if (GlobalConfig.isCloud) {
			context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
		} else {
			SpiderBaseResp resp = spiderManageServiceImpl.removeWorkNode(req);
			context.put("errorNo", resp.getErrorNo());
			context.put("errorInfo", resp.getErrorInfo());
		}
		return VelocityUtils.mergeTemplate(request,"vm/worknode/delete.vm",context);
	}
	
	@RequestMapping(value="/worknode/add.html")
	public @ResponseBody String addWorknode(WorkNodeReq req,HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().equals("GET")) {
			//NOP
		} else {
			if (GlobalConfig.isCloud) {
				context.put("errorNo", SpiderErrorNoConstant.ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE);
			} else {
				SpiderBaseResp resp = spiderManageServiceImpl.addWorkNode(req);
				context.put("errorNo", resp.getErrorNo());
				context.put("errorInfo", resp.getErrorInfo());
			}
		}
		return VelocityUtils.mergeTemplate(request,"vm/worknode/add.vm",context);
	}
	
	@RequestMapping(value="/clients.html",method=RequestMethod.GET)
	public @ResponseBody String queryClients(HttpServletRequest request) {
		List<SourceWorkNode> clients = spiderMonitorService.queryClients();
		VelocityContext context = new VelocityContext();
		context.put("clients", clients);
		return VelocityUtils.mergeTemplate(request,"vm/clients.vm",context);
		
	}
	@RequestMapping(value="/exports.html",method=RequestMethod.GET)
	public @ResponseBody String queryExports(HttpServletRequest request) {
		List<ServiceDefinition> exportServices = spiderMonitorService.queryExports();
		VelocityContext context = new VelocityContext();
		context.put("services", exportServices);
		return VelocityUtils.mergeTemplate(request,"vm/exports.vm",context);
		
	}
	@RequestMapping(value="/proxies.html",method=RequestMethod.GET)
	public @ResponseBody String queryProxies(HttpServletRequest request) {
		List<ServiceDefinition> proxyServices = spiderMonitorService.queryProxies();
		VelocityContext context = new VelocityContext();
		context.put("services", proxyServices);
		return VelocityUtils.mergeTemplate(request,"vm/proxies.vm",context);
		
	}
	@RequestMapping(value="/myinfo.html",method=RequestMethod.GET)
	public @ResponseBody String queryMyinfo(HttpServletRequest request) {
		MyInfo tmpMyInfo = spiderMonitorService.queryMyinfo();
		Map<String,String> myinfo = new TreeMap<String,String>();
		try {
			myinfo.putAll(BeanUtils.describe(tmpMyInfo));
			if(GlobalConfig.isServiceCenter) {
				myinfo.remove("nodeId");
				myinfo.remove("ha");
				myinfo.remove("appVersion");
				myinfo.remove("busiThreadCount");
				myinfo.remove("anonymous");
				myinfo.remove("cloud");
				myinfo.remove("compress");
				myinfo.remove("dynamicRouteEnable");
				myinfo.remove("encrypt");
				myinfo.remove("forceRecovery");
				myinfo.remove("haRemoteServerAddress");
				myinfo.remove("maxQueueCount");
				myinfo.remove("pendingTaskCount");
				myinfo.remove("reliable");
				myinfo.remove("reliableStatus");
				myinfo.remove("serviceCenterInetAddress");
				myinfo.remove("tcpDump");
				myinfo.remove("dumpStat");
				myinfo.remove("slowLongTime");
			}
			
			if (!GlobalConfig.reliable) {
				myinfo.remove("nodeId");
				myinfo.remove("ha");
				myinfo.remove("forceRecovery");
				myinfo.remove("haRemoteServerAddress");
				myinfo.remove("reliableStatus");
			}
			
			if(!GlobalConfig.ha) {
				myinfo.remove("nodeId");
				myinfo.remove("haRemoteServerAddress");
			}
			
			if (!GlobalConfig.isCloud) {
				myinfo.remove("serviceCenterInetAddress");
			}
			
			myinfo.remove("class");
		} catch (IllegalAccessException | InvocationTargetException
				| NoSuchMethodException e) {
			e.printStackTrace();
		}
		VelocityContext context = new VelocityContext();
		context.put("myinfo", myinfo);
		return VelocityUtils.mergeTemplate(request,"vm/myinfo.vm",context);
	}
	
	@RequestMapping(value="/dyn-route-caches.html",method=RequestMethod.GET)
	public @ResponseBody String queryDynRouteCaches(HttpServletRequest request) {
		List<DynRouteCache> dynRouteCaches = spiderMonitorService.queryDynRouteCaches();
		VelocityContext context = new VelocityContext();
		context.put("dynRouteCaches", dynRouteCaches);
		return VelocityUtils.mergeTemplate(request,"vm/dyn-route-caches.vm",context);
	}
	
	@RequestMapping(value="/metrics.html",method=RequestMethod.GET)
	public @ResponseBody String queryMetrics(HttpServletRequest request) {
		List<Metric> metrics = spiderMonitorService.queryMetrics();
		VelocityContext context = new VelocityContext();
		context.put("metrics", metrics);
		return VelocityUtils.mergeTemplate(request,"vm/metrics.vm",context);
	}
	
	@RequestMapping(value="/queues.html",method=RequestMethod.GET)
	public @ResponseBody String queryQueues(HttpServletRequest request) {
		List<SpiderRequest> queues = spiderMonitorService.queryReliableQueuedRequests();
		VelocityContext context = new VelocityContext();
		context.put("queues", queues);
		return VelocityUtils.mergeTemplate(request,"vm/queues.vm",context);
	}
	
	@RequestMapping(value="/thread-info.html",method=RequestMethod.GET)
	public @ResponseBody String queryBusiThreadInfo(HttpServletRequest request) {
		BusiThreadInfo threadInfo = spiderMonitorService.queryBusiThreadInfo();
		VelocityContext context = new VelocityContext();
		context.put("threadInfo", threadInfo);
		return VelocityUtils.mergeTemplate(request,"vm/thread-info.vm",context);
	}
	
	@RequestMapping(value="/reset-metrics.html")
	public @ResponseBody String resetMetrics(HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().toUpperCase().equals("GET")) {
			context.put("errorNo", "-1");
		} else {
			context.put("errorNo", "0");
			spiderMonitorService.resetMetrics();
		}
		return VelocityUtils.mergeTemplate(request,"vm/reset-metrics.vm",context);
	}
	
	@RequestMapping(value="/dump-metrics.html")
	public @ResponseBody String dumpMetrics(HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().toUpperCase().equals("GET")) {
			context.put("errorNo", "-1");
		} else {
			context.put("errorNo", "0");
			SpiderMonitorServiceImpl.dumpStat();
		}
		return VelocityUtils.mergeTemplate(request,"vm/dump-metrics.vm",context);
	}
	
	@RequestMapping(value="/node-tree.html")
	public @ResponseBody String nodeTree(HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		BroadcastResult broadcastResult;
		if(GlobalConfig.isServer) {
			context.put("errorNo", "-1");
			// 因为当前版本的spider不支持当节点作为server的时候，调用某个既是代理、又是实现的服务，准确的说，这是spring wire_by_type的原因, 因为目前创建代理的时候采用的是wire_by_type的机制
			// 除非在1.0.5版本对这个机制进行调整
			context.put("errorInfo", "在当前版本中,该功能必须从客户端节点发起！"); 
		} else {
			context.put("errorNo", "0");
			broadcastResult = otherManage.queryNodeBaseInfoForBroadcast(new SpiderBaseReq());
			context.put("nodeTree", broadcastResult);
		}
		return VelocityUtils.mergeTemplate(request,"vm/node-tree.vm",context);
	}
	
	@RequestMapping(value="/pending-requests.html")
	public @ResponseBody String pendingRequests(HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		context.put("pendingRequestCount", SpiderRouter.spiderMultiplex.size());
		int i = 0;
		Map<String,String> pendingRequests = new HashMap<String,String>();
		for(Entry<String, BlockingQueue<String>> entry : SpiderRouter.spiderMultiplex.entrySet()) {
			if(i>=200) {
				break;
			}
			try {
				pendingRequests.put(entry.getKey(), entry.getValue().peek() == null ? "null" : entry.getValue().peek());
			} catch (Exception e) {
				pendingRequests.put(entry.getKey(), "null");
			}
			i++;
		}
		context.put("pendingRequests", pendingRequests);
		return VelocityUtils.mergeTemplate(request,"vm/pending-requests.vm",context);
	}
	
	@RequestMapping(value="/generate-license-key.html")
	public @ResponseBody String generateLicenseKey(@RequestParam(required=false) String mac,
													@RequestParam(required=false) String expireDate,
													HttpServletRequest request) {
		VelocityContext context = new VelocityContext();
		if(request.getMethod().toUpperCase().equals("GET")) {
			context.put("errorNo", "-1");
		} else {
			context.put("errorNo", "0");
			if(StringUtils.isEmpty(mac) || StringUtils.isEmpty(expireDate)) {
				context.put("errorNo", "-2");
				context.put("errorInfo", "mac和到期日期不能为空！");
			} else {
				try {
					String licenseKey = SpiderServerAuthServiceImpl.generateLicenseKey(mac, expireDate);
					context.put("licenseKey", licenseKey);
				} catch (SpiderException e) {
					context.put("errorInfo", e.getMessage());
				}
			}
		}
		return VelocityUtils.mergeTemplate(request,"vm/generate-license-key.vm",context);
	}
}
