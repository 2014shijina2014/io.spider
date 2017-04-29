package com.ld.core.pack;

public class LDData {
	protected char type;
	protected Object value;
	
	public char getType() {
		return type;
	}

	public void setType(char type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public LDData(char type,Object value) {
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.format("[Data(type=%s,value=%s)]",type,value);
	}
}
