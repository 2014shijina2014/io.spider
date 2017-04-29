/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.api;

import io.spider.sc.center.pojo.ClusterAddManageReq;
import io.spider.sc.center.pojo.ClusterRemoveManageReq;
import io.spider.sc.center.pojo.InternalServiceStat;
import io.spider.sc.center.pojo.RouteItemManageReq;
import io.spider.sc.center.pojo.ServiceStatsReq;
import io.spider.sc.center.pojo.ShellExecuteReq;
import io.spider.sc.center.pojo.WorkNodeManageReq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ld.net.remoting.LDRequest;
import com.ld.net.remoting.LDService;
import io.spider.pojo.SpiderBaseReq;
import io.spider.pojo.ParallelBaseResp;
import io.spider.sc.pojo.MyInfo;
import io.spider.sc.pojo.NodeInfo;

@LDService
public interface NodeManageService {
	
	@LDRequest(methodId = "00000103")
	public Map<String,HashMap<String,String>> listManagedServers(SpiderBaseReq req);
	
	@LDRequest(methodId = "00000106")
	public MyInfo getMyInfo(NodeInfo req);
	
	@LDRequest(methodId = "00000114")
	public List<InternalServiceStat> queryServiceStats(ServiceStatsReq param);
	
	@LDRequest(methodId = "00000108")
	public List<ParallelBaseResp> pushAddWorkNode(WorkNodeManageReq req);
	
	@LDRequest(methodId = "00000109")
	public List<ParallelBaseResp> pushAddCluster(ClusterAddManageReq req);
	
	@LDRequest(methodId = "00000112")
	public List<ParallelBaseResp> pushRemoveWorkNode(WorkNodeManageReq req);
	
	@LDRequest(methodId = "00000113")
	public List<ParallelBaseResp> pushRemoveCluster(ClusterRemoveManageReq req);
	
	@LDRequest(methodId = "00000110")
	public List<ParallelBaseResp> pushAddRouteItem(RouteItemManageReq req);
	
	@LDRequest(methodId = "00000129")
	public List<ParallelBaseResp> executeShell(ShellExecuteReq req);
}
