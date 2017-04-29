/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.monitor.pojo;

public class BusiThreadInfo {
	private int pendingTaskCount;
	private int runnableThreadCount;
	private int blockedThreadCount;
	private int waitingThreadCount;
	private int timedWaitThreadCount;
	private int newThreadCount;
	private int maxConcurrent;
	public int getPendingTaskCount() {
		return pendingTaskCount;
	}
	public void setPendingTaskCount(int pendingTaskCount) {
		this.pendingTaskCount = pendingTaskCount;
	}
	public int getRunnableThreadCount() {
		return runnableThreadCount;
	}
	public void setRunnableThreadCount(int runnableThreadCount) {
		this.runnableThreadCount = runnableThreadCount;
	}
	public int getBlockedThreadCount() {
		return blockedThreadCount;
	}
	public void setBlockedThreadCount(int blockedThreadCount) {
		this.blockedThreadCount = blockedThreadCount;
	}
	public int getWaitingThreadCount() {
		return waitingThreadCount;
	}
	public void setWaitingThreadCount(int waitingThreadCount) {
		this.waitingThreadCount = waitingThreadCount;
	}
	public int getTimedWaitThreadCount() {
		return timedWaitThreadCount;
	}
	public void setTimedWaitThreadCount(int timedWaitThreadCount) {
		this.timedWaitThreadCount = timedWaitThreadCount;
	}
	public int getNewThreadCount() {
		return newThreadCount;
	}
	public void setNewThreadCount(int newThreadCount) {
		this.newThreadCount = newThreadCount;
	}
	public int getMaxConcurrent() {
		return maxConcurrent;
	}
	public void setMaxConcurrent(int maxConcurrent) {
		this.maxConcurrent = maxConcurrent;
	}
}
