/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;

import io.spider.pojo.SpiderBaseResp;

import java.util.List;

public class ShellExecuteResp extends SpiderBaseResp {
	
	public ShellExecuteResp() {
		super();
	}

	public ShellExecuteResp(String errorNo, String errorInfo) {
		super(errorNo, errorInfo);
	}

	public ShellExecuteResp(String errorNo) {
		super(errorNo);
	}

	private List<String> results;

	public List<String> getResults() {
		return results;
	}

	public void setResults(List<String> results) {
		this.results = results;
	}
}
