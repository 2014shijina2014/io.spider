package io.spider.utils;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class StringHelper {
	public static String ifNull(String str,String placeholder) {
		if(str == null) {
			return placeholder;
		}
		return str;
	}
	
	public static String ifEmpty(String str,String placeholder) {
		if (org.apache.commons.lang3.StringUtils.isEmpty(str)) {
			return placeholder;
		} else {
			return str;
		}
	}
	
	public static boolean isEqual(String str,String targetValue) {
		if(str == null) {
			return false;
		}
		if(str.equals(targetValue)) {
			return true;
		}
		return false;
	}

	/**
	 * @param attributeValue
	 * @param defaultTimeout
	 * @return
	 */
	public static int parseInt(String val, int def) {
		if (val == null) {
			return def;
		}
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
			return def;
		}
	}
}
