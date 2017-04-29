/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import org.apache.commons.lang3.StringUtils;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 1.0.10 change
 * 变更 SPIDER_MSG_TYPE_LEN 2->1
 * 新增 SPIDER_REQ_TYPE_LEN 1
 * 变更 SPIDER_PACKET_HEAD_SIZE_LEN 8->4
 * SPIDER_FIXED_PACKET_HEAD_SIZE 83->79
 */
public final class SpiderPacketPosConstant {
	public static final int SPIDER_PACKET_HEAD_SIZE_OFFSET = 0;
	public static final int SPIDER_PACKET_HEAD_SIZE_LEN = 4;
	public static final int SPIDER_MSG_TYPE_OFFSET = SPIDER_PACKET_HEAD_SIZE_OFFSET + SPIDER_PACKET_HEAD_SIZE_LEN;
	public static final int SPIDER_MSG_TYPE_LEN = 1;
	public static final int SPIDER_REQ_TYPE_OFFSET = SPIDER_MSG_TYPE_OFFSET + SPIDER_MSG_TYPE_LEN;
	public static final int SPIDER_REQ_TYPE_LEN = 1;
	public static final int SPIDER_SYSTEM_ID_OFFSET = SPIDER_REQ_TYPE_OFFSET + SPIDER_REQ_TYPE_LEN;
	public static final int SPIDER_SYSTEM_ID_LEN = 2;
	public static final int SPIDER_APP_VERSION_OFFSET = SPIDER_SYSTEM_ID_OFFSET + SPIDER_SYSTEM_ID_LEN;
	public static final int SPIDER_APP_VERSION_LEN = 8;
	public static final int SPIDER_COMPANY_ID_OFFSET = SPIDER_APP_VERSION_OFFSET + SPIDER_APP_VERSION_LEN;
	public static final int SPIDER_COMPANY_ID_LEN = 6;
	public static final int SPIDER_SERVICE_ID_OFFSET = SPIDER_COMPANY_ID_OFFSET + SPIDER_COMPANY_ID_LEN;
	public static final int SPIDER_SERVICE_ID_LEN = 12; // 20161205 8位扩展到12位, 资管现有功能号最多可能到12位
	public static final int SPIDER_MSG_ID_OFFSET = SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN;
	public static final int SPIDER_MSG_ID_LEN = 32;
	public static final int SPIDER_CRC32_OFFSET = SPIDER_MSG_ID_OFFSET + SPIDER_MSG_ID_LEN;
	public static final int SPIDER_CRC32_LEN = 10;
	public static final int SPIDER_ERROR_NO_OFFSET = SPIDER_CRC32_OFFSET + SPIDER_CRC32_LEN;
	public static final int SPIDER_ERROR_NO_LEN = 3;
	
	public static final int SPIDER_FIXED_PACKET_HEAD_SIZE = SPIDER_PACKET_HEAD_SIZE_LEN + 
															SPIDER_MSG_TYPE_LEN + 
															SPIDER_REQ_TYPE_LEN + 
															SPIDER_SYSTEM_ID_LEN + 
															SPIDER_APP_VERSION_LEN +
															SPIDER_COMPANY_ID_LEN + 
															SPIDER_SERVICE_ID_LEN + 
															SPIDER_MSG_ID_LEN + 
															SPIDER_CRC32_LEN + 
															SPIDER_ERROR_NO_LEN;
	
	public static String getStringFixedPacketHeadSize() {
		return StringUtils.leftPad(String.valueOf(SPIDER_FIXED_PACKET_HEAD_SIZE), SPIDER_PACKET_HEAD_SIZE_LEN, '0');
	}

	public static String getStringPacketHeadSize(int packetHeadSize) {
		return StringUtils.leftPad(String.valueOf(packetHeadSize), SPIDER_PACKET_HEAD_SIZE_LEN, '0');
	}
}
