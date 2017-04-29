/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.client.pojo;

import java.util.ArrayList;
import java.util.List;

import io.spider.pojo.SpiderBaseResp;
import io.spider.pojo.SpiderRequest;

public class SpiderRequestResp extends SpiderBaseResp {
	private List<SpiderRequest> list = new ArrayList<SpiderRequest>();
	
	public SpiderRequestResp(String errorNo,String errorInfo) {
		super(errorNo,errorInfo);
	}
	
	public SpiderRequestResp(String errorNo) {
		super(errorNo);
	}
	
	public void addSpiderRequest(SpiderRequest req) {
		list.add(req);
	}

	public SpiderRequestResp() {
		super();
	}

	public List<SpiderRequest> getList() {
		return list;
	}

	public void setList(List<SpiderRequest> list) {
		this.list = list;
	}
}
