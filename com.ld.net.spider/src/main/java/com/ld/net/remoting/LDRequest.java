package com.ld.net.remoting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求方法，配合LDSerivce使用
 * @author zhangcb
 *
 * @author zhjh256@163.com
 * change: spider 1.0.0开始增加time, 1.0.3开始增加needLog, 1.0.4开始增加broadcast
 * @see io.spider.annotation.Service
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LDRequest {
	String methodId();
	
	String desc() default "";

	int timeout() default 0;
	
	boolean needLog() default false;
	
	short broadcast() default 0;
	
	boolean his() default false;
	boolean batch() default false;
	
	short bizUserType() default 1;
}
