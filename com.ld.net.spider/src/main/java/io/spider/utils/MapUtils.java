/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

public class MapUtils {
	public static Map<String,String> removeEmpty(Map<String,String> map) {
		Iterator<Map.Entry<String, String>> it = map.entrySet().iterator();  
        while(it.hasNext()){  
            Map.Entry<String, String> entry=it.next();  
            if(StringUtils.isEmpty(entry.getValue().trim())) {
            	it.remove();
            }
        }  
        return map;
	}
}
