package io.spider.utils;

import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHHelper {
	static final Logger logger = LoggerFactory.getLogger(SSHHelper.class);
	/**
	 * 远程 执行命令并返回结果调用过程 是同步的（执行完才会返回）
	 * 
	 * @param host
	 *            主机名
	 * @param user
	 *            用户名
	 * @param psw
	 *            密码
	 * @param port
	 *            端口
	 * @param command
	 *            命令
	 * @return
	 */
	public static String exec(String host, String user, String psw, int port,
			String command) {
		String result = "";
		Session session = null;
		ChannelExec openChannel = null;
		try {
			JSch jsch = new JSch();
			session = jsch.getSession(user, host, port);
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(psw);
			
			session.connect();
			openChannel = (ChannelExec) session.openChannel("exec");
			openChannel.setCommand(command);
			int exitStatus = openChannel.getExitStatus();
			logger.info("命令返回码：" + exitStatus);
			openChannel.connect();
			InputStream in = openChannel.getInputStream();
			BufferedReader reader = new BufferedReader(
					new InputStreamReader(in));
			String buf = null;
			while ((buf = reader.readLine()) != null) {
				result += new String(buf.getBytes("gbk"), "UTF-8") + System.getProperty("line.separator");
			}
		} catch (JSchException | IOException e) {
			logger.error("host:" + host + ", port:" + port + ", user:" + user + ", pwd:" + psw + ", command:" + command + ". 执行出错！",e);
			throw new SpiderException(SpiderErrorNoConstant.ERROR_NO_CONNECT_TO_SSH_SERVER_FAILED,"ssh连接失败",e.toString());
		} finally {
			if (openChannel != null && !openChannel.isClosed()) {
				openChannel.disconnect();
			}
			if (session != null && session.isConnected()) {
				session.disconnect();
			}
		}
		logger.debug("命令[" + command + "]返回值:" + result);
		return result;
	}

	public static void main(String args[]) {
		String exec = exec("192.168.3.131", "root", "james@123", 22,
				"ps aux | grep mysqld | egrep -v 'safe|grep' | awk '{print $6}'");
		System.out.println(exec.trim());
	}
}