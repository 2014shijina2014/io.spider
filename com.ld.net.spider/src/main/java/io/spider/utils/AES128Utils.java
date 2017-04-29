/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 1.0.5版本开始支持可配置加密因子以及插件化自定义加密算法
 */
public class AES128Utils {
	/*
	 * 加密用的Key 可以用26个字母和数字组成 此处使用AES-128-CBC加密模式，key需要为16位。
	 */
	private static String sKey = "drpcpqq860916256";
	private static String ivParameter = "abcdef0123456789";

	// 加密
	public static String aesEncrypt(String sSrc) {
		Cipher cipher;
		byte[] encrypted = null;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			encrypted = cipher.doFinal(sSrc.getBytes(StandardCharsets.UTF_8));
			return Base64.encodeBase64String(encrypted);// 此处使用base64做转码。
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			throw new RuntimeException(e);
		}
	}

	// 解密
	public static String aesDecrypt(String sSrc) throws Exception {
		byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes(StandardCharsets.UTF_8));
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
		byte[] encrypted1 = Base64.decodeBase64(sSrc);// 先用base64解密
		byte[] original = cipher.doFinal(encrypted1);
		return new String(original, StandardCharsets.UTF_8);
	}

	public static byte[] aesDecrypt(byte[] msgBody) throws Exception {
		byte[] raw = sKey.getBytes(StandardCharsets.UTF_8);
		SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());
		cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
		byte[] original = cipher.doFinal(msgBody);
		return original;
	}

	public static byte[] aesEncrypt(byte[] bytes) {
		Cipher cipher;
		byte[] encrypted = null;
		try {
			cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			byte[] raw = sKey.getBytes();
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			IvParameterSpec iv = new IvParameterSpec(ivParameter.getBytes());// 使用CBC模式，需要一个向量iv，可增加加密算法的强度
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
			encrypted = cipher.doFinal(bytes);
		} catch (InvalidKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException e) {
			throw new RuntimeException(e);
		}
		return encrypted;
	}
	
	public static void main(String args[]) {
		// 需要加密的字串
		String cSrc = "c定能服务费为肌肤将违法违纪蜂王浆将诶额外if额外你胃口蜂王浆佛额外加分为王菲王菲hi风hihi123";
		System.out.println(cSrc);
		// 加密
		long lStart = System.currentTimeMillis();
		String enString = "";
		long lUseTime;
		try {
			for(int i=0;i<1;i++) {
				enString = AES128Utils.aesEncrypt(cSrc);
			}
			System.out.println("加密后的字串是：" + enString);

			lUseTime = System.currentTimeMillis() - lStart;
			System.out.println("加密耗时：" + lUseTime + "毫秒");
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		// 解密
		lStart = System.currentTimeMillis();
		String DeString;
		try {
			DeString = AES128Utils.aesDecrypt(enString);
			System.out.println("解密后的字串是：" + DeString);
			lUseTime = System.currentTimeMillis() - lStart;
			System.out.println("解密耗时：" + lUseTime + "毫秒");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
