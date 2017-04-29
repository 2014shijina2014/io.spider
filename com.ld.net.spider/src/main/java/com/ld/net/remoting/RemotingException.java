package com.ld.net.remoting;

@SuppressWarnings("serial")
public class RemotingException extends RuntimeException{
	protected String code;
	
	public String getCode() {
		return code;
	}
	
	public RemotingException(String code,String msg) {
		super(msg);
		this.code = code;
	}
	
	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}
}
