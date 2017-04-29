/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.utils;

import java.util.UUID;

public class UUIDUtils {
	public static String uuid() {
		return UUID.randomUUID().toString().trim().replaceAll("-", "");
	}
}
