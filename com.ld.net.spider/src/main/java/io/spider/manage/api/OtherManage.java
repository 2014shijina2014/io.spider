/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.manage.api;

import io.spider.annotation.Service;
import io.spider.annotation.ServiceModule;
import io.spider.pojo.BroadcastResult;
import io.spider.pojo.SpiderBaseReq;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@ServiceModule
public interface OtherManage {
	@Service(desc = "查询用于广播的节点基本信息", serviceId = "00000034",broadcast=2)
	public BroadcastResult queryNodeBaseInfoForBroadcast(SpiderBaseReq req);
}
