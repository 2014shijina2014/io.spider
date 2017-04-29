/**
 * 
 */
package com.ld.net.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface LDParamType {
	char cLDDataType();
}
