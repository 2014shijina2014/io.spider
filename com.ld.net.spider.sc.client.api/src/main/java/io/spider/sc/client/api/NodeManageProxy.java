/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.client.api;

import io.spider.annotation.Service;
import io.spider.annotation.ServiceModule;
import io.spider.pojo.SpiderBaseResp;
import io.spider.sc.pojo.ClusterReq;
import io.spider.sc.pojo.RouteItemReq;
import io.spider.sc.pojo.SftpUploadReq;
import io.spider.sc.pojo.ShellExecuteResp;
import io.spider.sc.pojo.ShellReq;
import io.spider.sc.pojo.WorkNodeReq;
/**
 * 这里的服务称之为spider节点自身的管理服务更加合适,当客户端通过spider协议而非HTTP协议进来时,纯天然可用,而不依赖于集中服务管理中心,任何管理客户端都可以
 * 该接口对于受管服务器,只能作为发布的服务,不能作为代理的服务
 * 
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@ServiceModule
public interface NodeManageProxy {
	@Service(desc = "新增服务器节点", serviceId = "00000008")
	public SpiderBaseResp addWorkNode(WorkNodeReq req);
	
	@Service(desc = "新增服务器集群", serviceId = "00000009")
	public SpiderBaseResp addCluster(ClusterReq req);
	
	@Service(desc = "删除服务器节点", serviceId = "00000012")
	public SpiderBaseResp removeWorkNode(WorkNodeReq req);
	
	@Service(desc = "删除服务器集群", serviceId = "00000013")
	public SpiderBaseResp removeCluster(String clusterName);
	
	@Service(desc = "新增路由", serviceId = "00000010")
	public SpiderBaseResp addRouteItem(RouteItemReq req);
	
	@Service(desc = "查询已完成请求", serviceId = "00000022")
	public SpiderBaseResp queryFinishedRequest(int count);
	
	@Service(desc = "查询并删除已完成请求", serviceId = "00000023")
	public SpiderBaseResp queryAndDeleteFinishedRequest(int count);
	
	@Service(desc = "执行shell命令", serviceId = "00000029",timeout=30000)
	public ShellExecuteResp executeShell(ShellReq cmds);
	
	@Service(desc = "执行sftp文件上传", serviceId = "00000030",timeout=30000)
	public SpiderBaseResp executeSftpUpload(SftpUploadReq req);
}
