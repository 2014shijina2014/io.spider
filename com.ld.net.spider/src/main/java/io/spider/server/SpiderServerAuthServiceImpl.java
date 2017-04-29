/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.server;

import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.ByteOrderUtils;
import io.spider.utils.CRCUtils;
import io.spider.utils.DateUtils;
import io.spider.utils.UUIDUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 1.0.5版本开始支持自定义插件可替换模式实现方式
 */
public class SpiderServerAuthServiceImpl {

	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static boolean verifyProtocol(String decryptedSpiderPacketBody) {
		if(decryptedSpiderPacketBody.equals(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE1_MSG)) {
			return true;
		}
		return false;
	}

	public static boolean verifyLicense(String decryptedSpiderLicenseKey) {
		if(GlobalConfig.dev) {
			logger.debug("license: " + decryptedSpiderLicenseKey);
		}
		String clientMac = decryptedSpiderLicenseKey.substring(0,12).toLowerCase();
		byte[] licenseKey = Base64.decodeBase64(StringUtils.reverse(decryptedSpiderLicenseKey.substring(12)));
		if (licenseKey.length != 50) {
			logger.error("license文件长度必须为50字节！");
			return false;
		}
		ByteArrayInputStream in = new ByteArrayInputStream(licenseKey);
		byte[] randomStr = new byte[12];
		in.read(randomStr , 0, 12);
		byte[] expireTs = new byte[18];
		in.read(expireTs ,0,18);
		byte[] mac = new byte[12];
		in.read(mac , 0, 12);
		byte[] crc32 = new byte[8];
		in.read(crc32 , 0, 8);
		long crc32Attach = ByteOrderUtils.byteArrayToLong(crc32);
		long crc32Compute = CRCUtils.getCRC32Value(new String(randomStr) + new String(expireTs) + new String(mac).toLowerCase());
		if (new String(mac).toLowerCase().equals(clientMac) 
				&& crc32Attach == crc32Compute 
				&& System.currentTimeMillis() < Long.valueOf(new String(expireTs))) {
			return true;
		} else {
			logger.error("licenseKey中Mac:" + new String(mac).toLowerCase() + ", 实际Mac:" + clientMac);
			logger.error("licenseKey中crc32:" + crc32Attach + ", 实际计算crc32:" + crc32Compute);
			logger.error("licenseKey中过期日期:" + Long.valueOf(new String(expireTs)) + ", 当前日期:" + System.currentTimeMillis());
			return false;
		}
	}
	
	// (12位随机字符+18位过期日期时间戳+10位MAC)+大端序MD5
	public static String generateLicenseKey(String mac,String expireDate) {
		try {
			long ts = DateUtils.SDF_DATE_NUM.parse(expireDate).getTime();
			String expireTs = StringUtils.leftPad(String.valueOf(ts), 18, '0');
			String randomStr = StringUtils.leftPad(UUIDUtils.uuid().substring(0, Math.min(12, UUIDUtils.uuid().length())), 12, '0');
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			out.write(randomStr.getBytes());
			out.write(expireTs.getBytes());
			out.write(mac.replaceAll("-", "").replaceAll(":", "").toLowerCase().getBytes());
			long crc32 = CRCUtils.getCRC32Value(randomStr + expireTs + mac.replaceAll("-", "").replaceAll(":", "").toLowerCase());
			if(GlobalConfig.dev) {
				logger.debug(String.valueOf(crc32));
			}
			out.write(ByteOrderUtils.long2byte(crc32));
			return StringUtils.reverse(Base64.encodeBase64String(out.toByteArray()));
		} catch (ParseException | IOException e) {
			throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_GENERATE_LICENSE_KEY_FAILED,
									MessageFormat.format(SpiderErrorNoConstant.ERROR_INFO_GENERATE_LICENSE_KEY_FAILED,e.getMessage()));
		}
	}
	
//	public static void main(String[] args) throws SocketException {
//		String mac = NetworkUtils.getLocalMac(NetworkUtils.getIPByPrefix("172.18.30"));
//		String res = generateLicenseKey(mac,"20170101");
//		System.out.println(mac);
//		System.out.println(res);
//		System.out.println(verifyLicense("F48E38ECABEE" + res));
//	}
}
