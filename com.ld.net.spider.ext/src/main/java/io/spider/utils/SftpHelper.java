/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;
import io.spider.utils.Base64Util;

import java.io.File;  
import java.io.FileInputStream;  
import java.io.FileOutputStream;  
import java.io.IOException;  
import java.io.InputStream;  
import java.util.Properties;  
   

import com.jcraft.jsch.Channel;  
import com.jcraft.jsch.ChannelSftp;  
import com.jcraft.jsch.JSch;  
import com.jcraft.jsch.JSchException;  
import com.jcraft.jsch.Session;  
import com.jcraft.jsch.SftpException;
/**
 * 
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SftpHelper {  
    private  JSch jSch = null;  
    private  ChannelSftp sftp = null;//sftp主服务  
    private  Channel channel = null;  
    private  Session session = null;  
      
    private  String hostName;//远程服务器地址  
    private  int    port;//端口  
    private  String userName;//用户名  
    private  String password;//密码  
    public SftpHelper(String hostName, int port, String userName,  
           String password){  
           this.hostName=hostName;  
           this.port=port;  
           this.userName=userName;  
           this.password=password;  
    }  
   
    /** 
     * 连接登陆远程服务器 
     * @return 
     */  
    public boolean connect() throws Exception{  
        try {  
            jSch= new JSch();  
            session = jSch.getSession(userName, hostName, port);  
            session.setPassword(Base64Util.getFromBase64(password));  
              
            session.setConfig(this.getSshConfig());  
            session.connect();  
   
            channel = session.openChannel("sftp");  
            channel.connect();  
   
            sftp = (ChannelSftp) channel;  
            System.out.println("登陆成功:"+sftp.getServerVersion());  
              
        } catch (JSchException e) {  
            System.err.println("SSH方式连接FTP服务器时有JSchException异常!");  
            System.err.println(e.getMessage());  
            throw e;  
        }  
        return true;  
    }  
    /** 
     * 关闭连接 
     * @throws Exception 
     */  
    private void  disconnect() throws Exception {  
        try{  
            if (sftp.isConnected()) {  
                sftp.disconnect();  
            }  
            if (channel.isConnected()) {  
                channel.disconnect();  
            }  
            if (session.isConnected()) {  
                session.disconnect();  
            }  
        }catch(Exception e){  
            throw e;  
        }  
     }  
       
  
    /** 
     * 获取服务配置 
     * @return 
     */  
    private Properties getSshConfig()throws Exception{  
        Properties sshConfig=null;  
        try{  
            sshConfig = new Properties();  
            sshConfig.put("StrictHostKeyChecking", "no");  
              
        }catch(Exception e){  
            throw e;  
        }  
        return sshConfig;  
    }  
    /** 
     * 下载远程sftp服务器文件 
     * @param remotePath 
     * @param remoteFilename 
     * @param localFilename 
     * @return 
     */  
    public  boolean downloadFile(String remotePath,  
            String remoteFilename,String localFilename)throws SftpException,  
            IOException, Exception{  
        FileOutputStream output = null;  
        boolean success = false;  
        try {  
            if (null != remotePath && remotePath.trim() != "") {  
                sftp.cd(remotePath);  
            }  
   
            File localFile = new File(localFilename);  
            //有文件和下载文件重名  
            if (localFile.exists()) {  
                System.err.println("文件: " + localFilename + " 已经存在!");  
                return success;  
            }  
            output = new FileOutputStream(localFile);  
            sftp.get(remoteFilename, output);  
            success = true;  
            System.out.println("成功接收文件,本地路径："+localFilename);  
        } catch (SftpException e) {  
            System.err.println("接收文件时有SftpException异常!");  
            System.err.println(e.getMessage());  
            return success;  
        } catch (IOException e) {  
            System.err.println("接收文件时有I/O异常!");  
            System.err.println(e.getMessage());  
            return success;  
        } finally {  
            try {  
                if (null != output) {  
                    output.close();  
                }  
                //关闭连接  
                disconnect();  
            } catch (IOException e) {  
                System.err.println("关闭文件时出错!");  
                System.err.println(e.getMessage());  
            }  
        }  
        return success;  
    }  
    /** 
     * 上传文件至远程sftp服务器 
     * @param remotePath 
     * @param remoteFilename 
     * @param localFileName 
     * @return 
     */  
    public  boolean uploadFile(String remotePath,  
            String remoteFilename, String localFileName)throws SftpException,Exception  {  
        boolean success = false;  
        FileInputStream fis=null;  
        try {  
            // 更改服务器目录  
            if (null != remotePath && remotePath.trim() != "") {  
                sftp.cd(remotePath);  
            }  
            File localFile = new File(localFileName);  
            fis = new FileInputStream(localFile);  
            // 发送文件  
            sftp.put(fis, remoteFilename);  
            success = true;  
            System.out.println("成功发送文件,本地路径："+localFileName);  
        } catch (SftpException e) {  
            System.err.println("发送文件时有SftpException异常!");  
            e.printStackTrace();  
            System.err.println(e.getMessage());  
            throw e;  
        } catch (Exception e) {  
            System.err.println("发送文件时有异常!");  
            System.err.println(e.getMessage());  
            throw e;  
        } finally {  
            try {  
                if (null != fis) {  
                    fis.close();  
                }  
                //关闭连接  
                disconnect();  
            } catch (IOException e) {  
                System.err.println("关闭文件时出错!");  
                System.err.println(e.getMessage());  
            }  
        }  
        return success;  
    }  
   
     /** 
     * 上传文件至远程sftp服务器 
     * @param remotePath 
     * @param remoteFilename 
     * @param input 
     * @return 
     */  
    public  boolean uploadFile(String remotePath,  
            String remoteFilename, InputStream input)throws SftpException,Exception  {  
        boolean success = false;  
        try {  
            // 更改服务器目录  
            if (null != remotePath && remotePath.trim() != "") {  
                sftp.cd(remotePath);  
            }  
   
            // 发送文件  
            sftp.put(input, remoteFilename);  
            success = true;  
        } catch (SftpException e) {  
            System.err.println("发送文件时有SftpException异常!");  
            e.printStackTrace();  
            System.err.println(e.getMessage());  
            throw e;  
        } catch (Exception e) {  
            System.err.println("发送文件时有异常!");  
            System.err.println(e.getMessage());  
            throw e;  
        } finally {  
            try {  
                if (null != input) {  
                    input.close();  
                }  
                //关闭连接  
                disconnect();  
            } catch (IOException e) {  
                System.err.println("关闭文件时出错!");  
                System.err.println(e.getMessage());  
            }  
      
        }  
        return success;  
    }  
    /** 
     * 删除远程文件 
     * @param remotePath 
     * @param remoteFilename 
     * @return 
     * @throws Exception  
     */  
    public  boolean deleteFile(String remotePath, String remoteFilename) throws Exception {  
        boolean success = false;  
        try {  
            // 更改服务器目录  
            if (null != remotePath && remotePath.trim() != "") {  
                sftp.cd(remotePath);  
            }  
   
            // 删除文件  
            sftp.rm(remoteFilename);  
            System.err.println("删除远程文件"+remoteFilename+"成功!");  
            success = true;  
        } catch (SftpException e) {  
            System.err.println("删除文件时有SftpException异常!");  
            e.printStackTrace();  
            System.err.println(e.getMessage());  
            return success;  
        } catch (Exception e) {  
            System.err.println("删除文件时有异常!");  
            System.err.println(e.getMessage());  
            return success;  
        } finally {  
            //关闭连接  
            disconnect();  
        }  
        return success;  
    }  
  
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
    /** 
     *测试方法 
     * 
     */  
    public static void main(String [] args){  
        try{  
            SftpHelper  sftp=new SftpHelper("172.18.30.60",22,"root","Ld123123");  
            sftp.connect();  
//          sftp.downloadFile("//", "test.txt", "D://work//test.txt");  
          sftp.uploadFile("/root", "com.ld.net.spider-1.0.3-SNAPSHOT.jar", "E:/ldpp/trunk/code/spider/com.ld.net.spider/target/com.ld.net.spider-1.0.3-SNAPSHOT.jar");  
//          sftp.deleteFile("//", "test.txt");  
          sftp.disconnect();
        }catch(Exception e){  
            System.out.println("异常信息：" + e.getMessage());  
        }  
    }  
}