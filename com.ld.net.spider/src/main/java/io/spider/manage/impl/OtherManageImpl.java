/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.manage.impl;

import io.spider.manage.api.OtherManage;
import io.spider.pojo.BroadcastResult;
import io.spider.pojo.SpiderBaseReq;

import org.springframework.stereotype.Service;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Service
public class OtherManageImpl implements OtherManage {

	/* (non-Javadoc)
	 * @see com.ld.net.spider.manage.api.OtherManage#queryNodeBaseInfoForBroadcast(com.ld.net.spider.sc.pojo.SpiderBaseReq)
	 */
	@Override
	public BroadcastResult queryNodeBaseInfoForBroadcast(SpiderBaseReq req) {
		BroadcastResult result = new BroadcastResult();
		return result;
	}
}
