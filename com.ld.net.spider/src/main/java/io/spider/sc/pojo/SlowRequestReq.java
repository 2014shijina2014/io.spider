/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;
import io.spider.pojo.SpiderRequest;

import java.util.List;

public class SlowRequestReq extends NodeInfo {
	private List<SpiderRequest> spiderRequests;

	private String remoteAddress;
	private String beginDate;
	private String endDate;
	private int elapsedMsTime;
	
	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	public String getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public List<SpiderRequest> getSpiderRequests() {
		return spiderRequests;
	}

	public void setSpiderRequests(List<SpiderRequest> spiderRequests) {
		this.spiderRequests = spiderRequests;
	}

	public int getElapsedMsTime() {
		return elapsedMsTime;
	}

	public void setElapsedMsTime(int elapsedMsTime) {
		this.elapsedMsTime = elapsedMsTime;
	}
}
