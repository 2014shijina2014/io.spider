/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.sc.client.impl;

import io.spider.BeanManagerHelper;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.mx.SpiderManageServiceImpl;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.SpiderBaseResp;
import io.spider.pojo.SpiderRequest;
import io.spider.sc.client.api.NodeManageProxy;
import io.spider.sc.client.pojo.SpiderRequestResp;
import io.spider.sc.pojo.ClusterReq;
import io.spider.sc.pojo.RouteItemReq;
import io.spider.sc.pojo.SftpUploadReq;
import io.spider.sc.pojo.ShellExecuteResp;
import io.spider.sc.pojo.ShellReq;
import io.spider.sc.pojo.WorkNodeReq;
import io.spider.utils.Base64Util;
import io.spider.utils.JsonUtils;
import io.spider.utils.SSHHelper;
import io.spider.utils.SftpHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.jcraft.jsch.SftpException;
/**
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
@Service
public class NodeManageProxyImpl implements NodeManageProxy {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	@Autowired
	private SpiderManageServiceImpl spiderManageServiceImpl;

	@Override
	public SpiderBaseResp addWorkNode(WorkNodeReq req) {
		logger.info("收到来自服务中心发送的增加节点请求:[" + JsonUtils.toJson(req) + "]");
		return spiderManageServiceImpl.addWorkNode(req);
	}

	@Override
	public SpiderBaseResp addCluster(ClusterReq req) {
		logger.info("收到来自服务中心发送的增加集群请求:[" + JsonUtils.toJson(req) + "]");
		return spiderManageServiceImpl.addCluster(req);
	}

	@Override
	public SpiderBaseResp removeWorkNode(WorkNodeReq req) {
		logger.info("收到来自服务中心发送的删除节点请求:[" + JsonUtils.toJson(req) + "]");
		return spiderManageServiceImpl.removeWorkNode(req);
	}

	@Override
	public SpiderBaseResp removeCluster(String clusterName) {
		logger.info("收到来自服务中心发送的删除集群请求:[" + clusterName + "]");
		return spiderManageServiceImpl.removeCluster(clusterName);
	}

	@Override
	public SpiderRequestResp queryAndDeleteFinishedRequest(int count) {
		Set<String> initReqKeys = null;
		Set<String> remoteInitReqKeys = null;
		SpiderRequestResp resp = queryFinishedRequest(count,initReqKeys,remoteInitReqKeys);
		
		if(resp.isSuccess()) {
			StringRedisTemplate localRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE,StringRedisTemplate.class);
			localRedisTemplate.delete(initReqKeys);
			
			if(GlobalConfig.ha) {
				StringRedisTemplate remoteRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_REMOTE_REDIS_TEMPLATE, StringRedisTemplate.class);
				try {
					remoteRedisTemplate.delete(remoteInitReqKeys);
				} catch (Exception e) {
					logger.error("可信模式下删除持久化存储失败！",e);
					resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DELETE_FINISHED_REQUEST_FAILED);
					resp.getList().clear();
					return resp;
				}
			}
		}
		return resp;
	}

	@Override
	public SpiderBaseResp addRouteItem(RouteItemReq req) {
		logger.info("收到来自服务中心发送的增加路由请求:[" + JsonUtils.toJson(req) + "]");
		return spiderManageServiceImpl.addRouteItem(req);
	}

	@Override
	public SpiderRequestResp queryFinishedRequest(int count) {
		Set<String> initReqKeys = null;
		Set<String> remoteInitReqKeys = null;
		SpiderRequestResp resp = queryFinishedRequest(count,initReqKeys,remoteInitReqKeys);
		return resp;
	}
	
	private SpiderRequestResp queryFinishedRequest(int count,Set<String> initReqKeys,Set<String> remoteInitReqKeys) {
		SpiderRequestResp resp = new SpiderRequestResp();
		if(!GlobalConfig.reliable) {
			resp = new SpiderRequestResp(SpiderErrorNoConstant.ERROR_NO_NONRELIABLE_HASNO_PERSISTENT_REQUEST);
			return resp;
		}
		
		StringRedisTemplate localRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE,StringRedisTemplate.class);
		initReqKeys = new HashSet<String>();
		remoteInitReqKeys = new HashSet<String>();
		try {
			Set<String> tmpInitReqKeys = localRedisTemplate.keys(SpiderOtherMetaConstant.REQ_PROCESS_STATUS_FINISH + "*");
			if(tmpInitReqKeys.size() > count) {
				for(int i=0;i<count;i++) {
					initReqKeys.add((String)tmpInitReqKeys.toArray()[i]);
				}
			} else {
				initReqKeys = tmpInitReqKeys;
			}
			
			if(GlobalConfig.ha) {
				for(String key : initReqKeys) {
					remoteInitReqKeys.add(GlobalConfig.nodeId + "_" + key);
				}
			}
			
			for(String req : localRedisTemplate.opsForValue().multiGet(initReqKeys)) {
				resp.addSpiderRequest(JsonUtils.json2Object(req, SpiderRequest.class));
			}
		} catch (Exception e) {
			logger.error("可信模式下查询本地持久化存储失败！",e);
			resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_QUERY_FINISHED_REQUEST_FAILED);
			resp.getList().clear();
			return resp;
		}
		return resp;
	}

	@Override
	public ShellExecuteResp executeShell(ShellReq req) {
		ShellExecuteResp resp = new ShellExecuteResp();
		logger.info("收到执行shell命令的请求, shell命令：" + req.getCmd());
		/*
		 * Properties prop = System.getProperties();
			String os = prop.getProperty("os.name");
			if(os.toLowerCase().indexOf("win") > 0) {
				resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_WINDOWS_IS_UNSUPPORT);
				return resp;
			}
		*/
		if(req.getExecMode() == 1) {
	        List<String> processList = new ArrayList<String>();  
	        try {  
	        	String[] command = {"/bin/sh", "-c", req.getCmd()};
	        	Process process = Runtime.getRuntime().exec(command);  
	            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));  
	            String line = "";
	            while ((line = input.readLine()) != null) {  
	                processList.add(line);  
	            }  
	            input.close();
	            resp.setResults(processList);
	        } catch (IOException e) {  
	            logger.error("",e);
	            resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SHELL_EXECUTE_FAILED);
	            resp.setCause(e.getMessage());
	        }
		} else if(req.getExecMode() == 2) {
			if(req.isCheckProxyNode()) {
				if (!req.getProxyNode().equals(GlobalConfig.clusterName)) {
					resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CURRENT_NODE_ISNOT_PROXY_NODE);
					resp.setCause("当前节点名: " + GlobalConfig.clusterName + ", 设置的监控代理节点名: " + req.getProxyNode());
					return resp;
				}
			}
			if(StringUtils.isEmpty(req.getDestIp()) || StringUtils.isEmpty(req.getUsername()) || StringUtils.isEmpty(Base64Util.getFromBase64(req.getBase64Pwd()))) {
				resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SHELL_EXECUTE_FAILED);
				resp.setCause("主机名或用户名或密码为空!");
			} else {
				try {
					String ret = SSHHelper.exec(req.getDestIp(), req.getUsername(), Base64Util.getFromBase64(req.getBase64Pwd()), req.getPort(), req.getCmd());
					resp.setResults(Arrays.asList(ret.split(System.getProperty("line.separator"))));
				} catch (SpiderException e) {
					resp.setErrorNo(e.getErrorNo());
					resp.setErrorInfo(e.getErrorInfo());
					resp.setCause(e.getDetail());
				}
			}
		}
        return resp;
	}

	@Override
	public SpiderBaseResp executeSftpUpload(SftpUploadReq req) {
		SpiderBaseResp resp = new SpiderBaseResp();
		try {
			SftpHelper helper = new SftpHelper(req.getHostName(),req.getPort(),req.getUserName(),req.getPassword());
			helper.connect();
			helper.uploadFile(req.getRemoteFilePath(), req.getRemoteFileName(), req.getLocalFileName());
		} catch (Exception e) {
			logger.error("sftp上传文件失败！",e);
			resp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SFTP_UPLOAD);
			resp.setErrorInfo(e.getMessage());
		}
		return resp;
	}
	
	public static void main(String[] arg){
		SftpUploadReq req = new SftpUploadReq();
		req.setHostName("172.18.30.193");
		req.setLocalFileName("d:/jaxen-1.1.6.jar");
		req.setRemoteFilePath("/platform");
		req.setRemoteFileName("jaxen-1.1.6.jar");
		req.setUserName("root");
		req.setPassword("123456");
		req.setPort(22);
		NodeManageProxyImpl proxy = new NodeManageProxyImpl();
		proxy.executeSftpUpload(req);
	}
}
