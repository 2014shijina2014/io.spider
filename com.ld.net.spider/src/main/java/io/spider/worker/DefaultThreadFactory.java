package io.spider.worker;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class DefaultThreadFactory implements ThreadFactory {

	private static final AtomicInteger poolId = new AtomicInteger();

    private final AtomicInteger nextId = new AtomicInteger();
    private final String prefix;
    
    public DefaultThreadFactory(String poolName) {
		super();
		if (poolName == null) {
            throw new NullPointerException("poolName");
        }
		this.prefix = poolName + "-" + poolId.getAndIncrement() + "-";
	}
    
	/* (non-Javadoc)
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, prefix + nextId.incrementAndGet());
		return t;
	}

}
