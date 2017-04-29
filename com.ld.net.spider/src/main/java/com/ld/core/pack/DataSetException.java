package com.ld.core.pack;

public class DataSetException extends RuntimeException {
	
	public DataSetException(String msg) {
		super(msg);
	}
	
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
