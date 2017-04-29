package com.ld.net.remoting;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 支持Request和Response的服务
 * @author zhangcb
 * @author zjhua modified
 *
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface LDService {
	String subSystemId() default "0"; //增加以兼容spider
	short broadcast() default 0; //增加以兼容spider
}
