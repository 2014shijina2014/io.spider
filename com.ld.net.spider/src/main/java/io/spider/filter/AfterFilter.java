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
public interface AfterFilter {
	/**
	 * 如修改了报文头或者返回值,应重新计算CRC32值,在目前的版本中,每次调用SpiderRoute.call的时候都会重新计算CRC32确保肯定不会发生修改，以后的版本考虑NB的高性能很有可能不每次进行计算
	 * 不过具体的过滤器实现不用关心CRC32的计算以及加解密,FilterChainer会统一负责处理
	 * @param head
	 * @param origParam
	 * @param retObj
	 * 一定要确保filter不会发生异常
	 */
	public void doFilter(SpiderPacketHead head,Object origParam,Object retObj);
}
