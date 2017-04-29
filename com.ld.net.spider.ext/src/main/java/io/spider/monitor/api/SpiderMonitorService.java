/**
 * 
 */
package io.spider.monitor.api;

import io.spider.annotation.Service;
import io.spider.annotation.ServiceModule;
import io.spider.monitor.pojo.BusiThreadInfo;
import io.spider.monitor.pojo.DynRouteCache;
import io.spider.monitor.pojo.Metric;
import io.spider.pojo.Cluster;
import io.spider.pojo.RouteItem;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.SourceWorkNode;
import io.spider.pojo.SpiderRequest;
import io.spider.sc.pojo.MyInfo;

import java.util.List;

/**
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@ServiceModule
public interface SpiderMonitorService {
	@Service(desc = "查询路由信息", serviceId = "00000001")
	public abstract List<RouteItem> queryRoutes();
		
	@Service(desc = "查询下游服务器", serviceId = "00000002")
	public abstract List<Cluster> queryClusters();

	@Service(desc = "查询客户端连接", serviceId = "00000003")
	public abstract List<SourceWorkNode> queryClients();
	
	@Service(desc = "查询发布的服务", serviceId = "00000004")
	public abstract List<ServiceDefinition> queryExports();
	
	@Service(desc = "查询代理的服务", serviceId = "00000005")
	public abstract List<ServiceDefinition> queryProxies();
	
	@Service(desc = "查询当前节点概述信息", serviceId = "00000006")
	public abstract MyInfo queryMyinfo();
	
	@Service(desc = "查询动态路由缓存", serviceId = "00000007")
	public abstract List<DynRouteCache> queryDynRouteCaches();
	
	@Service(desc = "查询服务运行时性能指标", serviceId = "00000014")
	public abstract List<Metric> queryMetrics();
	
	@Service(desc = "查询待处理请求队列", serviceId = "00000015")
	public abstract List<SpiderRequest> queryReliableQueuedRequests();
	
	@Service(desc = "清空服务运行时性能指标", serviceId = "00000016")
	public abstract void resetMetrics();
	
	@Service(desc = "查询服务器业务线程请求积压", serviceId = "00000036")
	public abstract BusiThreadInfo queryBusiThreadInfo();
}