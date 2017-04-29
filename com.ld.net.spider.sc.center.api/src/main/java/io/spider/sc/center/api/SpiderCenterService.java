/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.center.api;

import com.ld.net.spider.annotation.Service;
import com.ld.net.spider.annotation.ServiceModule;
import com.ld.net.spider.pojo.SpiderBaseResp;
import com.ld.net.spider.sc.pojo.ReliableStatusReq;
import com.ld.net.spider.sc.pojo.RegisterReq;
import com.ld.net.spider.sc.pojo.SlowRequestReq;
import com.ld.net.spider.sc.pojo.StatReq;
@ServiceModule
public interface SpiderCenterService {
	
	@Service(desc = "服务器节点注册", serviceId = "00000017")
	public SpiderBaseResp register(RegisterReq req);
	
	@Service(desc = "上报性能指标", serviceId = "00000018")
	public SpiderBaseResp uploadStat(StatReq req);
	
	@Service(desc = "通知HA持久化存储超时或断开异常", serviceId = "00000027")
	public SpiderBaseResp notifyHaStatus(ReliableStatusReq req);
	
	@Service(desc = "上报慢日志", serviceId = "00000026")
	public SpiderBaseResp uploadSlowRequestLog(SlowRequestReq req);
}
