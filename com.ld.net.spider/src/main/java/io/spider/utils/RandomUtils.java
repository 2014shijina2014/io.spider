/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.util.Random;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class RandomUtils {
	static Random ran = new Random();
	public static int getRandomInt() {
		return ran.nextInt();
	}
	
	public static double getRandomDouble() {
		return ran.nextDouble();
	}
}
