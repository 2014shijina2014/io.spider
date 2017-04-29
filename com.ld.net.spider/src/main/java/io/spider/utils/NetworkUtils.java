/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtils {
	static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);
	
	public static InetAddress getIPByPrefix(String ipPrefix) {
		Enumeration<NetworkInterface> allNetInterfaces = null;  
        try {  
            allNetInterfaces = NetworkInterface.getNetworkInterfaces();  
        } catch (java.net.SocketException e) {  
            e.printStackTrace();  
        }  
        InetAddress ip = null;  
        while (allNetInterfaces.hasMoreElements())  
        {  
            NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();  
            //System.out.println(netInterface.getName());  
            Enumeration<InetAddress> addresses = netInterface.getInetAddresses();  
            while (addresses.hasMoreElements())  
            {  
                ip = (InetAddress) addresses.nextElement();  
                if (ip != null && ip instanceof Inet4Address)  
                {  
                    if(ip.getHostAddress().startsWith(ipPrefix)) {
                    	return ip;
                    }
                }  
            }  
        }
        return null;
	}
	
	/**
	 * usage: getLocalMac(getIPByPrefix("172.18.30"))
	 * @param ia
	 * @return
	 * @throws SocketException
	 */
	// 84-EF-18-19-0E-3D 格式
	public static String getLocalMac(InetAddress ia) throws SocketException {
		if(ia == null || NetworkInterface.getByInetAddress(ia) == null) {
			logger.error(ia.getHostAddress());
			return null;
		}
		//获取网卡，获取地址
		byte[] mac = NetworkInterface.getByInetAddress(ia).getHardwareAddress();
		//System.out.println("mac数组长度："+mac.length);
		StringBuffer sb = new StringBuffer("");
		for(int i=0; i<mac.length; i++) {
			if(i!=0) {
				sb.append("-");
			}
			//字节转换为整数
			int temp = mac[i]&0xff;
			String str = Integer.toHexString(temp);
			//System.out.println("每8位:"+str);
			if(str.length()==1) {
				sb.append("0"+str);
			}else {
				sb.append(str);
			}
		}
		logger.debug("local mac:"+sb.toString().toUpperCase());
		return sb.toString().toUpperCase();
	}

	public static void main(String[] args) throws SocketException {
		System.out.println(getLocalMac(getIPByPrefix("172.18.30")));
	}
}
