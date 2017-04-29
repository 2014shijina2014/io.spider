/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceModule {
	String subSystemId() default "0";
	short broadcast() default 0;
}
