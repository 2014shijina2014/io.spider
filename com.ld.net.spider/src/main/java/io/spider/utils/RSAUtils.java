package io.spider.utils;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.apache.commons.codec.binary.Base64;

/**
 * spider 通信中间件
 * 
 * @author zhjh256@163.com 
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class RSAUtils {
	/**
	 * 生成公钥和私钥, 一般一次性生成, 存储在文件中进行分发和使用
	 */
	public static void generateKey() {  
        try {  
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");  
            kpg.initialize(1024);  
            KeyPair kp = kpg.genKeyPair();  
            PublicKey pbkey = kp.getPublic();  
            PrivateKey prkey = kp.getPrivate();  
            // 保存公钥  
            FileOutputStream f1 = new FileOutputStream("/tmp/spider/pubkey.dat");  
            ObjectOutputStream b1 = new ObjectOutputStream(f1);  
            b1.writeObject(pbkey);  
            // 保存私钥  
            FileOutputStream f2 = new FileOutputStream("/tmp/spider/privatekey.dat");  
            ObjectOutputStream b2 = new ObjectOutputStream(f2);  
            b2.writeObject(prkey);
            b1.close();
            b2.close();
        } catch (Exception e) {  
        }
    }
	
	/**
	 * 公钥加密, 一般调用者传递明文, 从本地存储读取公钥进行加密
	 * @param plainTxt
	 * @return
	 * @throws Exception
	 */
	public static String pubEncrypt(String plainTxt) throws Exception {  
        String s = Base64.encodeBase64String(plainTxt.getBytes("UTF-8"));  
        // 获取公钥及参数e,n  
        FileInputStream f = new FileInputStream("/tmp/spider/pubkey.dat");  
        ObjectInputStream b = new ObjectInputStream(f);  
        RSAPublicKey pbk = (RSAPublicKey) b.readObject();  
        BigInteger e = pbk.getPublicExponent();  
        BigInteger n = pbk.getModulus();  
        // 获取明文m  
        byte ptext[] = s.getBytes("UTF-8");  
        BigInteger m = new BigInteger(ptext);  
        // 计算密文c  
        BigInteger c = m.modPow(e, n);  
        // 保存密文  
        String ciperTxt = c.toString(); 
        b.close();
        return ciperTxt; 
    }
	
	/**
	 * 私钥解密, 一般调用者传递密文, 从本地存储读取私钥进行解密
	 * @param ciperTxt
	 * @return
	 * @throws Exception
	 */
    public static String privDecrypt(String ciperTxt) throws Exception {  
        BigInteger c = new BigInteger(ciperTxt);  
        // 读取私钥  
        FileInputStream f = new FileInputStream("/tmp/spider/privatekey.dat");  
        ObjectInputStream b = new ObjectInputStream(f);  
        RSAPrivateKey prk = (RSAPrivateKey) b.readObject();  
        BigInteger d = prk.getPrivateExponent();  
        // 获取私钥参数及解密  
        BigInteger n = prk.getModulus();  
        BigInteger m = c.modPow(d, n);  
        // 显示解密结果  
        byte[] mt = m.toByteArray();  
        String plainTxt = new String(Base64.decodeBase64(mt),"UTF-8");
        b.close();
        return plainTxt;
    }  
    public static void main(String args[]) {  
        try {  
            // generateKey();  
            String ciperTxt = pubEncrypt("测试大中华123区");
            System.out.println("公钥加密密文：" + ciperTxt);
            System.out.println("私钥解密：" + privDecrypt(ciperTxt));  
        } catch (Exception e) {  
            System.out.println(e.toString());  
        }  
    }
}