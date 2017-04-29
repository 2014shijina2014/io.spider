package com.ld.core.pack;

import java.math.BigDecimal;
import java.util.HashMap;

public class LDDataType {
	public final static char STRING = 'S';
	public final static char INT = 'I';
	public final static char DOUBLE = 'D';
	
	public final  static char DECIMAL = 'B';
	
	public final  static char LONG = 'L';
	public final  static char BYTE_ARRAY = 'R';
	public final  static char CHAR = 'C';
	public final  static char UNKNOW = 'N';
	
	static HashMap<Class, Character> clsToTypeMap = new HashMap<Class, Character>();
	static {
		clsToTypeMap.put(int.class, INT);
		clsToTypeMap.put(Integer.class, INT);
		clsToTypeMap.put(long.class, LONG);
		clsToTypeMap.put(Long.class, LONG);
		clsToTypeMap.put(byte[].class, BYTE_ARRAY);
		clsToTypeMap.put(byte.class, CHAR);
		clsToTypeMap.put(Character.class, CHAR);
		clsToTypeMap.put(char.class, CHAR);
		clsToTypeMap.put(String.class,STRING);
		clsToTypeMap.put(double.class, DECIMAL);
		clsToTypeMap.put(Double.class, DECIMAL);
		clsToTypeMap.put(Float.class, DECIMAL);
		clsToTypeMap.put(BigDecimal.class, DECIMAL);
	}
	
	
	public static char getType(Class<?> cls){
		Character ret = clsToTypeMap.get(cls);
		if(ret == null){
			return UNKNOW;
		} else {
			return ret;
		}
	}
}
