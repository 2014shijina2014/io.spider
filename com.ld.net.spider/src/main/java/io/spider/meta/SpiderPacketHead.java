/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import static io.spider.meta.SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_APP_VERSION_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_APP_VERSION_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_COMPANY_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_COMPANY_ID_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_CRC32_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_CRC32_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_FIXED_PACKET_HEAD_SIZE;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_ID_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_TYPE_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_MSG_TYPE_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_REQ_TYPE_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_REQ_TYPE_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SERVICE_ID_OFFSET;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SYSTEM_ID_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_SYSTEM_ID_OFFSET;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.CRCUtils;
import io.spider.utils.JsonUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderPacketHead {
	private int packetHeadSize;
	private char msgType;
	private char reqType;
	private String systemId = SpiderOtherMetaConstant.DEFAULT_SYSTEM_ID;
	private String appVersion = SpiderOtherMetaConstant.DEFAULT_APP_VERSION;
	private String companyId = SpiderOtherMetaConstant.DEFAULT_COMPANY_ID;
	private String serviceId;
	private String rpcMsgId;
	private String crc32;
	private String errorNo = SpiderErrorNoConstant.ERROR_NO_SUCCESS;
	private Map<String,String> spiderOpts;
	
	public SpiderPacketHead(SpiderBizHead head) {
		this.systemId = StringUtils.leftPad(head.getSystemId(), SPIDER_SYSTEM_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		this.appVersion = StringUtils.leftPad(head.getAppVersion(), SPIDER_APP_VERSION_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		this.companyId = StringUtils.leftPad(head.getCompanyId(), SPIDER_COMPANY_ID_LEN, DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR);
		this.spiderOpts = head.getSpiderOpts();
	}
	public SpiderPacketHead() {
		//NOP
	}
	/**
	 * @param head
	 */
	public SpiderPacketHead(SpiderPacketHead head) {
		packetHeadSize = head.getPacketHeadSize();
		msgType = head.getMsgType();
		reqType = head.getReqType();
		systemId = head.getSystemId();
		appVersion = head.getAppVersion();
		companyId = head.getCompanyId();
		serviceId = head.getServiceId();
		rpcMsgId = head.getRpcMsgId();
		crc32 = head.getCrc32();
		errorNo = head.getErrorNo();
		spiderOpts = head.getSpiderOpts();
	}
	public int getPacketHeadSize() {
		return packetHeadSize;
	}
	public void setPacketHeadSize(int packetHeadSize) {
		this.packetHeadSize = packetHeadSize;
	}
	public char getMsgType() {
		return msgType;
	}
	public void setMsgType(char msgType) {
		this.msgType = msgType;
	}
	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = StringUtils.leftPad(appVersion, SPIDER_APP_VERSION_LEN, " ");
	}
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	public String getServiceId() {
		return serviceId;
	}
	public SpiderPacketHead setServiceId(String serviceId) {
		this.serviceId = serviceId;
		return this;
	}
	public String getRpcMsgId() {
		return rpcMsgId;
	}
	public SpiderPacketHead setRpcMsgId(String rpcMsgId) {
		this.rpcMsgId = rpcMsgId;
		return this;
	}
	public String getCrc32() {
		return crc32;
	}
	public void setCrc32(String crc32) {
		this.crc32 = crc32;
	}
	public String getRouteInfo(String subSystemId, String serviceId2) {
		return this.getSystemId() + "," + subSystemId + ","  + this.getAppVersion() + ","  + this.companyId + "," + serviceId2;
	}
	public String getErrorNo() {
		return errorNo;
	}
	public void setErrorNo(String errorNo) {
		this.errorNo = errorNo.length() > SPIDER_ERROR_NO_LEN ? errorNo.substring(0, SPIDER_ERROR_NO_LEN) : errorNo;
	}
	@Override
	public String toString() {
		return "SpiderPacketHead [packetHeadSize=" + packetHeadSize
				+ ", msgType=" + msgType + ", systemId=" + systemId
				+ ", appVersion=" + appVersion + ", companyId=" + companyId
				+ ", serviceId=" + serviceId + ", rpcMsgId=" + rpcMsgId
				+ ", crc32=" + crc32 + ", errorNo=" + errorNo + "]";
	}
	public Map<String,String> getSpiderOpts() {
		return spiderOpts;
	}
	public void setSpiderOpts(Map<String,String> spiderOpts) {
		if(spiderOpts != null) {
			this.spiderOpts = spiderOpts;
		}
	}
	
	public SpiderPacketHead copy() {
		SpiderPacketHead packetHead = new SpiderPacketHead();
		packetHead.setPacketHeadSize(packetHeadSize);
		packetHead.setMsgType(msgType);
		packetHead.setReqType(reqType);
		packetHead.setSystemId(systemId);
		packetHead.setAppVersion(appVersion);
		packetHead.setCompanyId(companyId);
		packetHead.setServiceId(serviceId);
		packetHead.setRpcMsgId(rpcMsgId);
		packetHead.setCrc32(crc32);
		packetHead.setErrorNo(errorNo);
		Map<String,String> tmpSpiderOpts = new HashMap<String,String>();
		if(spiderOpts != null) {
			for(Entry<String,String> entry : spiderOpts.entrySet()) {
				tmpSpiderOpts.put(entry.getKey(),entry.getValue());
			}
		}
		packetHead.setSpiderOpts(tmpSpiderOpts);
		return packetHead;
	}
	
	public static boolean splitSpiderPacket(String spiderPacket,SpiderPacketHead packetHead,StringBuilder spiderPacketBody) {
		getPacketHead(spiderPacket,packetHead);
		spiderPacketBody.append(spiderPacket.substring(packetHead.getPacketHeadSize()));
		return true;
	}

	@SuppressWarnings("unchecked")
	public static void getPacketHead(String spiderPacket, SpiderPacketHead packetHead) {
		packetHead.setPacketHeadSize(Integer.valueOf(spiderPacket.substring(SPIDER_PACKET_HEAD_SIZE_OFFSET,SPIDER_PACKET_HEAD_SIZE_LEN)));
		packetHead.setMsgType(spiderPacket.substring(SPIDER_MSG_TYPE_OFFSET, SPIDER_MSG_TYPE_OFFSET + SPIDER_MSG_TYPE_LEN).charAt(0));
		packetHead.setReqType(spiderPacket.substring(SPIDER_REQ_TYPE_OFFSET, SPIDER_REQ_TYPE_OFFSET + SPIDER_REQ_TYPE_LEN).charAt(0));
		packetHead.setSystemId(spiderPacket.substring(SPIDER_SYSTEM_ID_OFFSET, SPIDER_SYSTEM_ID_OFFSET + SPIDER_SYSTEM_ID_LEN));
		packetHead.setAppVersion(spiderPacket.substring(SPIDER_APP_VERSION_OFFSET, SPIDER_APP_VERSION_OFFSET + SPIDER_APP_VERSION_LEN));
		if(packetHead.getAppVersion().equals(SpiderOtherMetaConstant.DEFAULT_APP_VERSION)) {
			packetHead.setAppVersion(GlobalConfig.appVersion);
		}
		packetHead.setCompanyId(spiderPacket.substring(SPIDER_COMPANY_ID_OFFSET, SPIDER_COMPANY_ID_OFFSET + SPIDER_COMPANY_ID_LEN));
		packetHead.setServiceId(spiderPacket.substring(SPIDER_SERVICE_ID_OFFSET, SPIDER_SERVICE_ID_OFFSET + SPIDER_SERVICE_ID_LEN));
		packetHead.setRpcMsgId(spiderPacket.substring(SPIDER_MSG_ID_OFFSET,SPIDER_MSG_ID_OFFSET + SPIDER_MSG_ID_LEN));
		packetHead.setCrc32(spiderPacket.substring(SPIDER_CRC32_OFFSET, SPIDER_CRC32_OFFSET + SPIDER_CRC32_LEN));
		packetHead.setErrorNo(spiderPacket.substring(SPIDER_ERROR_NO_OFFSET, SPIDER_ERROR_NO_OFFSET + SPIDER_ERROR_NO_LEN));
		if(packetHead.getPacketHeadSize() != SPIDER_FIXED_PACKET_HEAD_SIZE) {
			packetHead.setSpiderOpts((Map<String, String>) JsonUtils.json2Object(spiderPacket.substring(SPIDER_FIXED_PACKET_HEAD_SIZE, packetHead.getPacketHeadSize()),java.util.HashMap.class));
		}
	}
	
	public static String getRespSpiderPacketHead(SpiderPacketHead packetHead,byte[] spiderMsgBody) {
		return SpiderPacketPosConstant.getStringFixedPacketHeadSize() + 
				SpiderOtherMetaConstant.MSG_TYPE_RESP + 
				SpiderOtherMetaConstant.REQ_TYPE_BIZ + 
				packetHead.getSystemId() +
				packetHead.getAppVersion() +
				packetHead.getCompanyId() +
				packetHead.getServiceId() + 
				packetHead.getRpcMsgId() + 
				CRCUtils.getCRC32Value(spiderMsgBody) +  
				packetHead.getErrorNo();
	}

	public static String getRespSpiderPacket(SpiderPacketHead packetHead,String spiderMsgBody) {
		return SpiderPacketPosConstant.getStringFixedPacketHeadSize() + 
				SpiderOtherMetaConstant.MSG_TYPE_RESP + 
				SpiderOtherMetaConstant.REQ_TYPE_BIZ + 
				packetHead.getSystemId() +
				packetHead.getAppVersion() +
				packetHead.getCompanyId() +
				packetHead.getServiceId() + 
				packetHead.getRpcMsgId() + 
				CRCUtils.getCRC32Value(spiderMsgBody,GlobalConfig.charset) +  
				packetHead.getErrorNo() +
				spiderMsgBody;
	}
	
	public static String getReqSpiderPacketHead(SpiderPacketHead packetHead, byte[] spiderMsgBody) {
		String packetHeadSize = null;
		String spiderVarHead = "";
		if (packetHead.getSpiderOpts() == null || packetHead.getSpiderOpts().size() == 0) {
			packetHeadSize = SpiderPacketPosConstant.getStringFixedPacketHeadSize();
		} else {
			spiderVarHead = JsonUtils.toJson(packetHead.getSpiderOpts());
			packetHeadSize = SpiderPacketPosConstant.getStringPacketHeadSize(SPIDER_FIXED_PACKET_HEAD_SIZE + spiderVarHead.length());
		}
		return packetHeadSize + 
				SpiderOtherMetaConstant.MSG_TYPE_REQ + 
				SpiderOtherMetaConstant.REQ_TYPE_BIZ + 
				packetHead.getSystemId() +
				packetHead.getAppVersion() +
				packetHead.getCompanyId() +
				packetHead.getServiceId() + 
				packetHead.getRpcMsgId() + 
				CRCUtils.getCRC32Value(spiderMsgBody) + 
				SpiderErrorNoConstant.ERROR_NO_SUCCESS + 
				spiderVarHead;
	}

	public static String getReqSpiderPacket(SpiderPacketHead packetHead, String spiderMsgBody) {
		String packetHeadSize = null;
		String spiderVarHead = "";
		if (packetHead.getSpiderOpts() == null || packetHead.getSpiderOpts().size() == 0) {
			packetHeadSize = SpiderPacketPosConstant.getStringFixedPacketHeadSize();
		} else {
			spiderVarHead = JsonUtils.toJson(packetHead.getSpiderOpts());
			packetHeadSize = SpiderPacketPosConstant.getStringPacketHeadSize(SPIDER_FIXED_PACKET_HEAD_SIZE + spiderVarHead.length());
		}
		return packetHeadSize + 
				SpiderOtherMetaConstant.MSG_TYPE_REQ + 
				SpiderOtherMetaConstant.REQ_TYPE_BIZ + 
				packetHead.getSystemId() +
				packetHead.getAppVersion() +
				packetHead.getCompanyId() +
				packetHead.getServiceId() + 
				packetHead.getRpcMsgId() + 
				CRCUtils.getCRC32Value(spiderMsgBody,GlobalConfig.charset) + 
				SpiderErrorNoConstant.ERROR_NO_SUCCESS + 
				spiderVarHead + 
				spiderMsgBody;
	}
	public char getReqType() {
		return reqType;
	}
	public void setReqType(char reqType) {
		this.reqType = reqType;
	}
}
