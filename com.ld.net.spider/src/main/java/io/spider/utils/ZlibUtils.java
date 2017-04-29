/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;

import com.jcraft.jzlib.JZlib;
import com.jcraft.jzlib.ZInputStream;
import com.jcraft.jzlib.ZOutputStream;

/**
 * 
 * spider 通信中间件
 * 
 * @author zhjh256@163.com
 * @see{@link http://www.cnblogs.com/zhjh256
 * @version 1.0.5版本开始支持应用级zlib加密而不是使用netty自带的过滤器机制以最大化性能, 原来在demo下windows测试没有问题
 *          1116测试linux的时候发现两边不一致
 */
public class ZlibUtils {

	public static String compress(String data) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZOutputStream zOut = new ZOutputStream(out, JZlib.Z_BEST_COMPRESSION);
			ObjectOutputStream objOut = new ObjectOutputStream(zOut);
			objOut.writeObject(data);
			zOut.close();
			return Base64.encodeBase64String(out.toByteArray());   
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String decompress(String data) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(
					Base64.decodeBase64(data));
			ZInputStream zIn = new ZInputStream(in);
			ObjectInputStream objIn = new ObjectInputStream(zIn);
			return (String) objIn.readObject(); 
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) throws UnsupportedEncodingException {
		String compressed = compress("0000007901qqqq.qq.qqqqqqqq0000002171779ad0dfdb4a4f9ac4be18ef3a78080837403181000drpcpqq");
		System.out.println(compressed.length());
		System.out.println(decompress(compressed));
		System.out.println(System.currentTimeMillis());
		compressed = compress("兼容性要求（参考OS/浏览器市场份额调查报告http://www.jiangweishan.com/article/marketData.htmlhttp://www.jiangweishan.com/article/marketData.htmlhttp://www.jiangweishan.com/article/marketData.html|http://www.jiangweishan.com/article/marketData2016.html）：兼容性要求（参考OS/浏览器市场份额调查报告http://www.jiangweishan.com/article/marketData.html|http://www.jiangweishan.com/article/marketData2016.html）：");
		System.out.println(decompress(compressed));
	}
}
