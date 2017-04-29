/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.pojo;

import io.spider.meta.SpiderErrorNoConstant;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * spider 通信中间件
 * spider内部管理功能响应基类
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderBaseResp {
	private String errorNo = SpiderErrorNoConstant.ERROR_NO_SUCCESS;
	private String errorInfo = SpiderErrorNoConstant.ERROR_INFO_SUCCESS;
	private String cause;
	
	public SpiderBaseResp() {
		super();
	}
	
	public SpiderBaseResp(String errorNo) {
		super();
		this.errorNo = errorNo;
		this.errorInfo = SpiderErrorNoConstant.getErrorInfo(errorNo);
	}
	
	public SpiderBaseResp(String errorNo, String errorInfo) {
		super();
		this.errorNo = errorNo;
		this.errorInfo = errorInfo;
	}
	public String getErrorNo() {
		return errorNo;
	}
	public void setErrorNo(String errorNo) {
		this.errorNo = errorNo;
		this.errorInfo = SpiderErrorNoConstant.getErrorInfo(errorNo);
	}
	public String getErrorInfo() {
		return errorInfo;
	}
	public void setErrorInfo(String errorInfo) {
		this.errorInfo = errorInfo;
	}
	
	@JsonIgnore
	public boolean isSuccess() {
		return "000".equals(errorNo);
	}

	public String getCause() {
		return cause;
	}

	public void setCause(String cause) {
		this.cause = cause;
	}

	@Override
	public String toString() {
		return "SpiderBaseResp [errorNo=" + errorNo + ", errorInfo="
				+ errorInfo + ", cause=" + cause + "]";
	}
}
