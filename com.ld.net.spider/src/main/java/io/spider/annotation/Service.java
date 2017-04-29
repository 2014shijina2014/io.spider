/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.ElementType;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Service {
	String serviceId(); // 服务编号，12位ASCII字符，其中00000000-00000099为spider内部保留，00000100-00000199为服务中心保留
	String desc(); //服务描述
	int timeout() default 0; //超时时间，单位毫秒
	/**
	 * @since 1.0.10
	 * @return
	 */
	boolean needLog() default false; //设置是否记录日志, 需mongodb支持
	short broadcast() default 0;  //设置该请求是否广播，0：不广播；1：广播但无需相应；2：广播并响应
	/** 
	 * @since 1.0.10
	 */
	boolean his() default false; //是否历史查询功能, 路由支持直接根据是否历史查询的标志路由到对应的节点
	/** 
	 * @since 1.0.10
	 */
	boolean batch() default false; //是否批处理, 路由支持直接根据是否批处理比如导数据、清算的标志路由到对应的节点
	/**
	 * 使用该服务的目标用户, 1：C端用户; 2: B端管理; 3: 运维。
	 * 用于潜在的根据业务模式优化，比如确定是否功能权限检查、数据权限检查，暂未使用，预留  
	 * @return
	 */
	short bizUserType() default 1; 
}
