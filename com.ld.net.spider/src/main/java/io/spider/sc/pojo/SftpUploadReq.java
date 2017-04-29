package io.spider.sc.pojo;

public class SftpUploadReq {

	private  String hostName;//远程服务器地址  
    private  int    port;//端口  
    private  String userName;//用户名  
    private  String password;//密码  
    private String remoteFilePath; //远程上传路径
    private String remoteFileName; //远程保存文件名
    private String localFileName; //本地待上传的文件路径+文件名
	public String getHostName() {
		return hostName;
	}
	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	public String getRemoteFilePath() {
		return remoteFilePath;
	}
	public void setRemoteFilePath(String remoteFilePath) {
		this.remoteFilePath = remoteFilePath;
	}
	public String getRemoteFileName() {
		return remoteFileName;
	}
	public void setRemoteFileName(String remoteFileName) {
		this.remoteFileName = remoteFileName;
	}
	public String getLocalFileName() {
		return localFileName;
	}
	public void setLocalFileName(String localFileName) {
		this.localFileName = localFileName;
	}
    
    
}
