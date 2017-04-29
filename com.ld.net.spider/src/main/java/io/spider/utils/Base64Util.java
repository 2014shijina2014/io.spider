package io.spider.utils;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

public class Base64Util {
	// 加密  
    public static String getBase64(String str) {  
        byte[] b = null;  
        String s = null;  
        try {  
            b = str.getBytes("utf-8");  
        } catch (UnsupportedEncodingException e) {  
            e.printStackTrace();  
        }  
        if (b != null) {  
            s = Base64.encodeBase64String(b);  
        }  
        return s;  
    }  
  
    // 解密  
    public static String getFromBase64(String s) {  
        byte[] b = null;  
        String result = null;  
        if (s != null) {  
            try {  
                b = Base64.decodeBase64(s);  
                result = new String(b, "utf-8");  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
        return result;  
    } 
    
	// 加密  
    public static String getBase64(byte[] str) {  
        String s = null;  

        if (str != null) {  
            s = Base64.encodeBase64String(str);  
        }  
        return s;  
    }  
  
    // 解密  
    public static String getFromBase64(byte[] s) {  
        byte[] b = null;  
        String result = null;  
        if (s != null) {  
            try {  
                b = Base64.decodeBase64(s);  
                result = new String(b, "utf-8");  
            } catch (Exception e) {  
                e.printStackTrace();  
            }  
        }  
        return result;  
    }
}
