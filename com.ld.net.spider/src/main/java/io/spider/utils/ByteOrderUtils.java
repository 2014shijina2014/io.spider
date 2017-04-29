/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.io.UnsupportedEncodingException;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 
 */
public class ByteOrderUtils {
	private int t;
	
	private synchronized int getAndIncr() {
		return t++;
	}
	
	public static void main(String[] args) {
		ByteOrderUtils b = new ByteOrderUtils();
		for(int i=0;i<10000000;i++) {
			b.getAndIncr();
		}
		byte[] ints = new byte[4];
		ints[0]=32;
		ints[1]=31;
		ints[2]=30;
		ints[3]=29;
		System.out.println(byteArrayToInt(ints));
		byte[] bytes = new byte[2];
		bytes[0]=1;
		bytes[1]=0;
		System.out.println(byteArrayToShort(bytes));
		System.out.println(int2byte(538910237)[0] + "," + int2byte(538910237)[1] + "," + int2byte(538910237)[2] + "," + int2byte(538910237)[3]);
		try {
			System.out.println(new String(short2byte((short)77),"UTF-8"));
			System.out.println(new String(short2byte((short)255),"UTF-8"));
			System.out.println(new String(short2byte((short)256),"UTF-8"));
			System.out.println(new String(short2byte((short)777),"UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		System.out.println(short2byte((short)256)[0] + "," + short2byte((short)256)[1]);
		System.out.println(short2byte((short)777)[0] + "," + short2byte((short)777)[1]);
	}
	
	public static String short2netbyte(short res) {
		byte[] b = short2byte(res);
		if (b[0] == 0) {
			return "0" + (char) b[1];
		} else {
			return (char) b[0] + "" + (char) b[1];
		}
	}
	
	public static byte[] short2byte(short res) {  
		byte[] targets = new byte[2];  
		  
		targets[1] = (byte) (res & 0xff);// 最低位   
		targets[0] = (byte) (res >>> 8);// 最高位      
		return targets;
	}

	public static short byteArrayToShort(byte[] b){  
	    byte[] a = new byte[2];  
	    int i = a.length - 1,j = b.length - 1;  
	    for (; i >= 0 ; i--,j--) {//从b的尾部(即int值的低位)开始copy数据  
	        if(j >= 0)  
	            a[i] = b[j];  
	        else  
	            a[i] = 0;//如果b.length不足2,则将高位补0  
	  }  
	    short v0 = (short) ((a[0] & 0xff) << 8);  
	    short v1 = (short) (a[1] & 0xff) ;  
	    return (short) (v0 + v1);  
	}
	
	public static byte[] int2byte(int res) {  
		byte[] targets = new byte[4];  
		  
		targets[3] = (byte) (res & 0xff);// 最低位   
		targets[2] = (byte) ((res >> 8) & 0xff);// 次低位   
		targets[1] = (byte) ((res >> 16) & 0xff);// 次高位   
		targets[0] = (byte) (res >>> 24);// 最高位,无符号右移。   
		return targets;
	}

	public static int byteArrayToInt(byte[] b){  
	    byte[] a = new byte[4];  
	    int i = a.length - 1,j = b.length - 1;  
	    for (; i >= 0 ; i--,j--) {//从b的尾部(即int值的低位)开始copy数据  
	        if(j >= 0)  
	            a[i] = b[j];  
	        else  
	            a[i] = 0;//如果b.length不足4,则将高位补0  
	  }  
	    int v0 = (a[0] & 0xff) << 24;//&0xff将byte值无差异转成int,避免Java自动类型提升后,会保留高位的符号位  
	    int v1 = (a[1] & 0xff) << 16;  
	    int v2 = (a[2] & 0xff) << 8;  
	    int v3 = (a[3] & 0xff) ;  
	    return v0 + v1 + v2 + v3;  
	}
	
	public static byte[] long2byte(long res) {  
		byte[] buffer = new byte[8]; 
	     for (int i = 0; i < 8; i++) {   
	          int offset = 64 - (i + 1) * 8;    
	          buffer[i] = (byte) ((res >> offset) & 0xff); 
	      }
	     return buffer;
	}

	public static long byteArrayToLong(byte[] b){  
		long  values = 0;   
	    for (int i = 0; i < 8; i++) {    
	        values <<= 8; values|= (b[i] & 0xff);   
	    }   
	    return values; 
	}
}
