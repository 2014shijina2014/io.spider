package io.spider.pojo;

import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * 用户会话校验, 常用于AR/NB层, 不做密码和权限校验, 通过插件的方式实现, 预留
 * 1、是否登录
 * 2、是否允许多点登录
 * 3、超时时间
 */
public class Users {
	public static final ConcurrentHashMap<String,ConcurrentHashMap<String,User>> loginUsers = new ConcurrentHashMap<String,ConcurrentHashMap<String,User>>();
	
	public static boolean addUser(User user) {
		return loginUsers.putIfAbsent(user.getUid(), new ConcurrentHashMap<String,User>()).put(user.getMac(), user) != null;
	}
	
	public static boolean isUserValid(User user) {
		if(loginUsers.get(user.getUid()) != null) {
			if (loginUsers.get(user.getUid()).get(user.getMac()).notTimeout()) {
				return true;
			} else {
				loginUsers.get(user.getUid()).remove(user.getMac());
				return false;
			}
		}
		return false;
	}
	
	public static class User {
		private String uid;
		private String mac;
		private Timestamp lastAccessTime = new Timestamp(System.currentTimeMillis());
		
		public String getUid() {
			return uid;
		}
		/**
		 * @return
		 */
		public boolean notTimeout() {
			return this.lastAccessTime.after(new Timestamp(System.currentTimeMillis() - GlobalConfig.sessionTimeout * 1000)); 
		}
		public void setUid(String uid) {
			this.uid = uid;
		}
		public String getMac() {
			return mac;
		}
		public void setMac(String mac) {
			this.mac = mac;
		}
		public Timestamp getLastAccessTime() {
			return lastAccessTime;
		}
		public void setLastAccessTime(Timestamp lastAccessTime) {
			this.lastAccessTime = lastAccessTime;
		}
		
		public String getKey() {
			return uid + ":" + mac;
		}
	}
}
