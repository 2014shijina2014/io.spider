/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;

import org.apache.commons.lang3.StringUtils;

public class CRCUtils {
	public static String getCRC32Value(String content, String charset) {
		CRC32 crc32 = new CRC32();
		try {
			crc32.update(content.getBytes(charset));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String crc32Value = String.valueOf(crc32.getValue());
		if(crc32Value.length() < 10) {
			crc32Value = StringUtils.leftPad(crc32Value, 10, '0');
		} else {
			crc32Value = crc32Value.substring(0, 10);
		}
		return crc32Value;
	}
	
	public static long getCRC32Value(String content) {
		CRC32 crc32 = new CRC32();
		crc32.update(content.getBytes(StandardCharsets.UTF_8));
		return crc32.getValue();
	}

	public static String getCRC32Value(byte[] msgBody) {
		CRC32 crc32 = new CRC32();
		crc32.update(msgBody);
		String crc32Value = String.valueOf(crc32.getValue());
		if(crc32Value.length() < 10) {
			crc32Value = StringUtils.leftPad(crc32Value, 10, '0');
		} else {
			crc32Value = crc32Value.substring(0, 10);
		}
		return crc32Value;
	}
}
