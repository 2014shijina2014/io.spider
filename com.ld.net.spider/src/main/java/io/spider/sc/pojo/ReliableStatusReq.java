/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.pojo;

public class ReliableStatusReq extends NodeInfo {
	private boolean reliableStatus;

	public boolean getReliableStatus() {
		return reliableStatus;
	}

	public void setReliableStatus(boolean reliableStatus) {
		this.reliableStatus = reliableStatus;
	}
}
