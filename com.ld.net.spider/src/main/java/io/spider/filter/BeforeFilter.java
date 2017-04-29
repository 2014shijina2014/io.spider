/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.filter;

import io.spider.meta.SpiderPacketHead;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public interface BeforeFilter {
	/**
	 * 如修改了报文头或者参数,应重新计算CRC32值,或者继承AbstractBeforeFilter抽象类
	 * @param head
	 * @param origParam
	 * 一定要确保filter不会发生异常
	 */
	public void doFilter(SpiderPacketHead head,Object origParam);
}
