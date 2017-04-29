package io.spider.stat;

import io.spider.meta.SpiderPacketHead;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SlowRequest extends SpiderPacketHead {
	String msgBody;
	String remoteAddress;
	long beg;
	long end;
	long elapseMs;
	public SlowRequest(SpiderPacketHead head, String msgBody,
			String remoteAddress, long beg, long end, long elapseMs) {
		super(head);
		this.msgBody = msgBody;
		this.remoteAddress = remoteAddress;
		this.beg = beg;
		this.end = end;
		this.elapseMs = elapseMs;
	}
}
