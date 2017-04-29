package com.ld.core.pack;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.ld.core.pack.utils.CastUtils;

public abstract class LDConvert<T,F> {
	public abstract char getType();
	public abstract LDData convert(T value);
	public abstract T recover(F value);
	
	static HashMap<Class, LDConvert> converts = new HashMap<Class, LDConvert>();
	
	static {
		converts.put(BigDecimal.class, new ToDecimalS());
		converts.put(Date.class, new ToDateTime());
	}
	
	public static void setConvert(Class cls,LDConvert convert) {
		converts.put(cls, convert);
	}
	
	public static LDConvert getConvert(Class cls) {
		return converts.get(cls);
	}
	
	public static class ToDate extends LDConvert<Date,String> {
		
		static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		public LDData convert(Date value) {
			LDData data = new LDData(LDDataType.STRING, null);
			data.value = format.format(value);
			return data;
		}
		
		public char getType() {
			return LDDataType.STRING;
		}
		
		public Date recover(String value) {
			try {
				return format.parse(value);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class ToDateTime extends LDConvert<Date,String> {
		
		static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		public LDData convert(Date value) {
			if(value == null)
				return null;
			LDData data = new LDData(LDDataType.STRING, null);
			data.value = format.format(value);
			return data;
		}
		
		public char getType() {
			return LDDataType.STRING;
		}
		
		public Date recover(String value) {
			if(value == null)
				return null;
			try {
				return format.parse(value);
			} catch (ParseException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
	
	public static class ToDecimalS extends LDConvert<BigDecimal,Object> {
		public LDData convert(BigDecimal value) {
			LDData data = new LDData(LDDataType.STRING,value);
			return data;
		}
		public char getType() {
			return LDDataType.STRING;
		}
		public BigDecimal recover(Object value) {
			if(value == null)
				return null;
			return CastUtils.toBigDecimal(value);
		}
	}
	
}
