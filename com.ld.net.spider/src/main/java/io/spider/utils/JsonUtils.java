/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;


/**
 *  Json工具类类，扩展封装Jackson
 *  @author zjhua
 *  @version 1.0.0
 *  20161107使用2.x jackson重写
 *  TODO 在后续版本中, 将支持list优化, 对于超过配置值的list列表,将解析为二维数组以提高效率
 */
public class JsonUtils {

    private static final Logger log = Logger.getLogger(JsonUtils.class);

    final static ObjectMapper objectMapper;

    /**
     * 是否打印美观格式
     */
    static boolean isPretty = false;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector());
        objectMapper.setDateFormat(DateUtils.SDF_DATETIME_NUM);
    }

    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    
	@SuppressWarnings("unchecked")
	public static <T> T json2GenericObject(String jsonString, TypeReference<T> tr) {

        if (jsonString == null || "".equals(jsonString)) {
            return null;
        } else {
            try {
                return (T) objectMapper.readValue(jsonString, tr);
            } catch (Exception e) {
                log.warn("json error:" + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Java对象转Json字符串
     *
     * @param object Java对象，可以是对象，数组，List,Map等
     * @return json 字符串
     */
    public static String toJson(Object object) {
        String jsonString = "";
        try {
            if (isPretty) {
                jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
            } else {
                jsonString = objectMapper.writeValueAsString(object);
            }
        } catch (Exception e) {
            log.warn("json error:" + e.getMessage());
        }
        return jsonString;
    }

    /**
     * Json字符串转Java对象
     *
     * @param jsonString
     * @param c
     * @return
     */
    public static <T> T json2Object(String jsonString, Class<T> c) {
        if (jsonString == null || "".equals(jsonString)) {
            return null;
        } else {
            try {
                return objectMapper.readValue(jsonString, c);
            } catch (Exception e) {
                log.error("json deserialize error,json string:" + jsonString + ". class:" + c.getCanonicalName(),e);
                throw new RuntimeException(e);
            }
        }
    }
    
    public static <T> List<T> json2ListAppointed(String content, Class<T> clazz){
    	JavaType valueType = objectMapper.getTypeFactory().constructParametricType(ArrayList.class, clazz);
    	try {
			return objectMapper.readValue(content, valueType);
		} catch (JsonParseException e) {
			log.error("json error:" + e.getMessage());
		} catch (JsonMappingException e) {
			log.error("json error:" + e.getMessage());
		} catch (IOException e) {
			log.error("json error:" + e.getMessage());
		}
    	return null;
    }
    
    public static <T> CopyOnWriteArrayList<T> json2ConcurrentListAppointed(String content, Class<T> clazz){
    	JavaType valueType = objectMapper.getTypeFactory().constructParametricType(CopyOnWriteArrayList.class, clazz);
    	try {
			return objectMapper.readValue(content, valueType);
		} catch (JsonParseException e) {
			log.error("json error:" + e.getMessage());
		} catch (JsonMappingException e) {
			log.error("json error:" + e.getMessage());
		} catch (IOException e) {
			log.error("json error:" + e.getMessage());
		}
    	return null;
    }

    public static String getNodeJson(String content, String nodeName){
		try {
			JsonNode nodes = objectMapper.readTree(content);
			String itemsJson = nodes.get(nodeName).toString(); 
			return itemsJson;
		} catch (JsonProcessingException e) {
			log.error("json error:" + e.getMessage());
		} catch (IOException e) {
			log.error("json error:" + e.getMessage());
		}
		return null;
    }

	/**
	 * 功能说明: 使用字段名作为json字符串中键值策略.<br>
	 * 注意事项: jackjson默认策略是直接从get\set方法去掉前缀后,取接下来的大写字母(如果存在连续大写,则一并转换)转换成小写.<br>
	 * 例如,实际字段名为aBc,按照标准JavaBean规范,对应存在getABc\setABc方法,这时下面的defaultName是abc.<br>
	 */
	static class UseFieldNameStrategy extends PropertyNamingStrategy {

		/**
		 * 
		 */
		private static final long serialVersionUID = -5588393318420205518L;

		@Override
		public String nameForGetterMethod(MapperConfig<?> config,
				AnnotatedMethod method, String defaultName) {
			return nameForMethod(method, defaultName, "get");
		}

		@Override
		public String nameForSetterMethod(MapperConfig<?> config,
				AnnotatedMethod method, String defaultName) {
			return nameForMethod(method, defaultName, "set");
		}

		private String nameForMethod(AnnotatedMethod method,
				String defaultName, String methodNamePrefix) {
			String fieldName = method.getName().replaceFirst(methodNamePrefix,
					"");
			if (fieldName.length() >= 2) {
				StringBuilder result = new StringBuilder(fieldName);
				// 如果第1个是大写并且第2个也是大写,则第一个转为小写
				if (isUpperCase(result.charAt(0))
						&& isUpperCase(result.charAt(1))) {
					result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
					defaultName = result.toString();
				}
			}
			return defaultName;
		}

		private boolean isUpperCase(char c) {
			return Character.isUpperCase(c);
		}
	}
	
//	public static void main(String[] args) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
//		Field[] fields = RouteItem.class.getDeclaredFields();
//		Constructor[] cons = RouteItem.class.getConstructors();
//		System.out.println(System.currentTimeMillis());
//		for(int i=0;i<100000;i++) {
//			RouteItem item = (RouteItem) cons[1].newInstance(new Object[] {"a","a","a","a","a","a","a","a","a","a","a","a","a","a","a","a"});
//		}
//		System.out.println(System.currentTimeMillis());
//		
//		Class clz = RouteItem.class;
//		System.out.println(System.currentTimeMillis());
//		for(int f=0;f<100000;f++) {
//			for(int i=0;i<fields.length;i++) {
//				RouteItem item = (RouteItem) clz.newInstance();
//				fields[i].setAccessible(true);
//				fields[i].set(item, "a");
//			}
//		}
//		System.out.println(System.currentTimeMillis());
//	}
}