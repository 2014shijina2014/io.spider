/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.client;

import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.AES128Utils;
import io.spider.utils.NetworkUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 1.0.5版本开始支持自定义插件可替换模式实现方式
 */

public class SpiderClientAuthServiceImpl {

	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);

	public static String getLicense(InetAddress address) {
		try {
			InputStream in = SpiderClientAuthServiceImpl.class.getResourceAsStream("/spider.dat");  
	        BufferedReader fr = new BufferedReader(new InputStreamReader(in));  
			BufferedReader bf = new BufferedReader(fr);
			String licenseKey = bf.readLine();
			if (StringUtils.isEmpty(licenseKey)) {
				logger.error("license为空,认证模式下不合法！");
				throw new SpiderException(
						"",
						SpiderErrorNoConstant.ERROR_NO_LICENSE_INCORRECT,
						SpiderErrorNoConstant.ERROR_INFO_LICENSE_INCORRECT);
			}

			String mac = NetworkUtils.getLocalMac(address).toLowerCase();
			if (mac == null) {
				logger.error("MAC地址为空,在认证模式下,客户端license非法！");
				throw new SpiderException(
						"",
						SpiderErrorNoConstant.ERROR_NO_LICENSE_INCORRECT,
						SpiderErrorNoConstant.ERROR_INFO_LICENSE_INCORRECT);
			}
			licenseKey = mac.replaceAll("-", "").replaceAll(":", "") + licenseKey;
			// 因为直接从handler发出去，不经过spider route，所以需要判断 GlobalConfig.encrypt
			if (GlobalConfig.encrypt) {
				try {
					licenseKey = AES128Utils.aesEncrypt(licenseKey);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return licenseKey;
		} catch (IOException e1) {
			throw new SpiderException(
					SpiderErrorNoConstant.ERROR_NO_LICENSE_INCORRECT,
					SpiderErrorNoConstant.ERROR_INFO_LICENSE_INCORRECT,
					e1);
		}
	}
}
