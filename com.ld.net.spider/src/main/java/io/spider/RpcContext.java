/**
 * 
 */
package io.spider;

import java.util.HashMap;
import java.util.Map;

/**
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class RpcContext {
	private ThreadLocal<Map<String,String>> tl = new ThreadLocal<Map<String,String>>() {
		public Map<String,String> initialValue() {  
            return new HashMap<String,String>();  
        }
	};
	
	private static final RpcContext rpcContext = new RpcContext();
	
	private RpcContext() {}
	
	public static final RpcContext getInstance() {
		return rpcContext;
	}
	
	public Map<String,String> get() {
		return this.tl.get();
	}
	
	public void set(Map<String,String> t) {
		this.tl.set(t);
	}
	
	public void set(String key,String value) {
		this.tl.get().put(key, value);
	}
}
