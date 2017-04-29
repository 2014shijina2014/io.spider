/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.manage.api;

import io.spider.annotation.Service;
import io.spider.annotation.ServiceModule;
import io.spider.pojo.RouteItemForTcpDump;
import io.spider.pojo.SpiderBaseReq;
import io.spider.pojo.SpiderBaseResp;

import java.util.List;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 这个接口不需要实现,仅提供给客户端使用
 */
@ServiceModule
public interface InterceptManage {
	@Service(desc = "", serviceId = "00000025")
	public SpiderBaseResp addInterceptor(List<RouteItemForTcpDump> interceptor);
	
	@Service(desc = "", serviceId = "00000032")
	public SpiderBaseResp removeInterceptor(SpiderBaseReq req);
	
	@Service(desc = "", serviceId = "00000037")
	public List<String> queryTcpDump(SpiderBaseReq req);
}
