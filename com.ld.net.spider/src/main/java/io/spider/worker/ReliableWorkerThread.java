/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.worker;

import io.spider.BeanManagerHelper;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketPosConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.pojo.SpiderRequest;
import io.spider.server.SpiderServerDispatcher;
import io.spider.utils.JsonUtils;

import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ReliableWorkerThread implements Callable<Boolean> {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private SpiderRequest spiderRequest;
	public ReliableWorkerThread(SpiderRequest spiderRequest) {
		this.spiderRequest = spiderRequest;
	}

	@Override
	public Boolean call() throws Exception {
		String errorNo = "";
		int i=0;
		while (i < 100) {
			StringBuilder spiderResponsePacketSB = new StringBuilder();
			int ret = SpiderServerDispatcher.dispatch(ServiceDefinitionContainer.getService(spiderRequest.getSpiderPacketHead().getServiceId()), spiderRequest.getSpiderPacketHead(), spiderRequest.getRequestBody(), spiderResponsePacketSB);
			if (SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN == ret) {
				errorNo = spiderResponsePacketSB.toString().substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, 
																		SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
				if(errorNo.equals(SpiderErrorNoConstant.ERROR_NO_FORWARD_FAILED)) {
					break;
				}
				spiderRequest.setResultInfo(spiderResponsePacketSB.toString());
			} else {
				logger.error("此路径不会发生！");
			}
			Thread.sleep(500);
			i++;
		}
		return updateReqExecutionStatus(spiderRequest);
	}

	private boolean updateReqExecutionStatus(SpiderRequest req) {
		boolean isSuccess = true;
		StringRedisTemplate remoteRedisTemplate = null;
		if(GlobalConfig.ha) {
			remoteRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_REMOTE_REDIS_TEMPLATE, StringRedisTemplate.class);
		}
		
		if(GlobalConfig.ha) {
			try {
				remoteRedisTemplate.opsForValue().set(getRemotePersistKey(GlobalConfig.nodeId,SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT,req.getRequestId()), JsonUtils.toJson(req));
				remoteRedisTemplate.rename(getRemotePersistKey(GlobalConfig.nodeId,SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT,req.getRequestId()), getRemotePersistKey(GlobalConfig.nodeId,SpiderOtherMetaConstant.REQ_PROCESS_STATUS_FINISH,req.getRequestId()));
			} catch (Exception e) {
				logger.error("可信模式下更新执行结果到远程持久化存储失败！",e);
				logger.error(JsonUtils.toJson(req));
				isSuccess = false;
			}
		}
		
		StringRedisTemplate localRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE,StringRedisTemplate.class);
		try {
			localRedisTemplate.opsForValue().set(getLocalPersistKey(SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT,req.getRequestId()), JsonUtils.toJson(req));
			localRedisTemplate.rename(getLocalPersistKey(SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT,req.getRequestId()), getLocalPersistKey(SpiderOtherMetaConstant.REQ_PROCESS_STATUS_FINISH,req.getRequestId()));
		} catch (Exception e) {
			logger.error("可信模式下更新执行结果到本地持久化存储失败！",e);
			logger.error(JsonUtils.toJson(req));
			isSuccess = false;
		}
		return isSuccess;
	}

	public static String getLocalPersistKey(String reqStatus,String requestId) {
		return reqStatus + "_" + requestId;
	}
	
	public static String getRemotePersistKey(String nodeId,String reqStatus,String requestId) {
		return nodeId + "_" + reqStatus + "_" + requestId;
	}
}
