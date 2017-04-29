/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefectionUtils {
	static final Logger logger = LoggerFactory.getLogger(RefectionUtils.class);
	
	public static Field getField(Class clz,String fieldName) {
		Field field = null;
		Class tmpClz = clz;
		while (field == null && tmpClz != Object.class) {
			try {
				field = tmpClz.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {
				tmpClz = ((Class)tmpClz.getGenericSuperclass());
			}
		}
		if (field == null) {
			logger.warn(clz.getCanonicalName() + " has no field " + fieldName + ", caller will resolve it !");
		}
		return field;
	}
}
