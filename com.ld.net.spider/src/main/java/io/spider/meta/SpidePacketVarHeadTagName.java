/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public final class SpidePacketVarHeadTagName {
	private static Map<String,String> tagNameMap = new HashMap<String,String>();
	public static final String ASYNC = "TAG01";  //异步调用标志
	public static final String CALLBACK_SERVICE_ID = "TAG02";  //回调功能号
	
	/**
	 * parallelPlugin相关扩展报文头
	 */
	public static final String PARALLEL_FACTOR_TYPE = "TAG03";
	public static final String PARALLEL_FACTORS = "TAG04";
	public static final String PARALLEL_DEGREE = "TAG05";
	public static final String PARALLEL_FACTOR_DRIVER = "TAG06";
	public static final String PARALLEL_COND_JSON_PATH = "TAG07";
	public static final String MAC = "TAG08";
	public static final String UID = "TAG09";
	
	public static boolean containsTag(String key) {
		return tagNameMap.containsValue(key);
	}
	
	static {
		Field[] fields = SpidePacketVarHeadTagName.class.getFields();
        for( Field field : fields ){
            try {
            	tagNameMap.put(field.getName(), field.get(SpidePacketVarHeadTagName.class).toString());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
        }
	}
}
