/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import io.spider.exception.SpiderException;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.NetworkUtils;

import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import com.ld.net.remoting.LDParam;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderBizHead {
	/**
	 * 调用端注入, 外部非java直接调用时需要自行负责注入
	 * @since 1.0.5
	 * @since 1.0.8 增加mac, uid
	 */
	@LDParam("service_id")
	private String serviceId; //服务号
	@LDParam("system_id")
	private String systemId = SpiderOtherMetaConstant.DEFAULT_SYSTEM_ID; //系统编号
	@LDParam("app_version")
	private String appVersion = SpiderOtherMetaConstant.DEFAULT_APP_VERSION;  //系统版本
	@LDParam("company_id")
	private String companyId = SpiderOtherMetaConstant.DEFAULT_COMPANY_ID;  //机构编号
	@LDParam("source_id")
	private String sourceIp = SpiderOtherMetaConstant.HOSTNAME;  //源系统标识符,默认本机
	@LDParam("dest_ip")
	private String destIp = SpiderOtherMetaConstant.HOSTNAME;  //发生异常系统标识符,默认为最后一个出异常服务器
	
	@LDParam("mac")
	private String mac = "";  // 原始客户端的MAC地址, 转发期间不回去修改
	@LDParam("uid")
	private String uid = ""; // 用户标识符, 转发期间不修改
	
	public void fillMac(String nicName) {
		try {
			mac = NetworkUtils.getLocalMac(NetworkUtils.getIPByPrefix(GlobalConfig.ipPrefix));
			this.addSpiderOpt(SpidePacketVarHeadTagName.MAC, mac);
		} catch (SocketException e) {
			e.printStackTrace();
		}
	}
	
	private Map<String,String> spiderOpts = new HashMap<String,String>();
	
	public void addSpiderOpt(String key,String value) {
		if(SpidePacketVarHeadTagName.containsTag(key)) {
			spiderOpts.put(key, value);
		} else {
			throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_NOT_DEFINED_HEAD_TAG,SpiderErrorNoConstant.ERROR_INFO_NOT_DEFINED_HEAD_TAG,"Spider可变包头必须在SpidePacketVarHeadTagName中已经定义");
		}
	}
	
	public Map<String,String> getSpiderOpts() {
		return spiderOpts;
	}
	
	//spiderOpts不应该直接操作,否则可能会导致运行时失败！！！
	public void setSpiderOpts(Map<String, String> spiderOpts) {
		this.spiderOpts = spiderOpts;
	}
	
	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getSystemId() {
		return systemId;
	}
	public void setSystemId(String systemId) {
		this.systemId = systemId;
	}
	public String getCompanyId() {
		return companyId;
	}
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}
	public String getAppVersion() {
		return appVersion;
	}
	public void setAppVersion(String appVersion) {
		this.appVersion = appVersion;
	}
	public String getSourceIp() {
		return sourceIp;
	}
	public void setSourceIp(String sourceIp) {
		this.sourceIp = sourceIp;
	}
	public String getDestIp() {
		return destIp;
	}
	public void setDestIp(String destIp) {
		this.destIp = destIp;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
		this.addSpiderOpt(SpidePacketVarHeadTagName.UID, uid);
	}
}
