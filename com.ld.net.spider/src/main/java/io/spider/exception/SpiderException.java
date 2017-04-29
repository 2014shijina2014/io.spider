/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.exception;

import java.text.MessageFormat;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderException extends RuntimeException {
	private String serviceId;
	private String errorNo;
	private String errorInfo;
	private String detail;
	/**
	 * 
	 */
	private static final long serialVersionUID = -414659235251828592L;

	public SpiderException(String errorNo,String errorInfo,Throwable cause) {
		super(MessageFormat.format("errorNo:{0},errorInfo:{1}. cause:{2}",errorNo,errorInfo, cause.getLocalizedMessage()));
		this.errorNo = errorNo;
		this.errorInfo = errorInfo;
		this.detail = cause.getLocalizedMessage();
	}
	
	public SpiderException(String errorNo,String errorInfo) {
		super(MessageFormat.format("errorNo:{0},errorInfo:{1}",errorNo,errorInfo));
		this.errorNo = errorNo;
		this.errorInfo = errorInfo;
	}
	
	public SpiderException(String serviceId,String errorNo,String errorInfo,String desc) {
		super(MessageFormat.format("serviceId:{0},errorNo:{1},errorInfo:{2}. detail:{3}",serviceId,errorNo,errorInfo,desc));
		this.serviceId = serviceId;
		this.errorNo = errorNo;
		this.errorInfo = errorInfo;
		this.detail = desc;
	}
	
	public SpiderException(String serviceId,String errorNo,String errorInfo) {
		super(MessageFormat.format("serviceId:{0},errorNo:{1},errorInfo:{2}",serviceId,errorNo,errorInfo));
		this.serviceId = serviceId;
		this.errorNo = errorNo;
		this.errorInfo = errorInfo;
	}

	public String getErrorNo() {
		return errorNo;
	}

	public void setErrorNo(String errorNo) {
		this.errorNo = errorNo;
	}

	public String getErrorInfo() {
		return errorInfo;
	}

	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}
	
	//兼容原来的RemotingException
	public String getCode() {
		return this.errorNo;
	}

	public String getDetail() {
		return detail;
	}

	public void setDetail(String detail) {
		this.detail = detail;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}
}
