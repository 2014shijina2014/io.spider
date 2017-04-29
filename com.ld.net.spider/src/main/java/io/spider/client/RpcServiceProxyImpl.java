/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.client;

import static io.spider.meta.SpiderOtherMetaConstant.BIZ_ERROR_NO_AND_INFO_SEP;
import static io.spider.meta.SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE;
import static io.spider.meta.SpiderOtherMetaConstant.SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP;
import io.netty.channel.Channel;
import io.spider.SpiderRouter;
import io.spider.channel.SocketHelper;
import io.spider.exception.SpiderException;
import io.spider.filter.AfterFilter;
import io.spider.filter.BeforeFilter;
import io.spider.meta.ISpiderHead;
import io.spider.meta.SpiderBizHead;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderPacketPosConstant;
import io.spider.parser.RouteParser;
import io.spider.pojo.BroadcastResult;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.pojo.WorkNode;
import io.spider.utils.AES128Utils;
import io.spider.utils.JsonUtils;
import io.spider.utils.UUIDUtils;

import java.lang.reflect.ParameterizedType;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteAccessor;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * @since 1.0.8 支持callback
 */

public class RpcServiceProxyImpl extends RemoteAccessor implements InitializingBean,MethodInterceptor,FactoryBean<Object> {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private Object serviceProxy;
	
	@Override
	public Object invoke(final MethodInvocation invocation) throws Throwable {
		final ServiceDefinition serviceDef = ServiceDefinitionContainer.getServiceByMethodName(invocation.getMethod().toString());
		if(GlobalConfig.status < SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_STARTING) {
			logger.warn(MessageFormat.format("spider runtime is initializing, please wait and try later. current service id is {0},request is {1}",serviceDef.getServiceId(),JsonUtils.toJson(invocation.getArguments()[0])));
			throw new SpiderException("",SpiderErrorNoConstant.ERROR_NO_SPIDER_IS_INITIALIZING,
					SpiderErrorNoConstant.ERROR_INFO_SPIDER_IS_INITIALIZING);
		}
		if(GlobalConfig.dev) {
			logger.debug("calling proxy class: " + invocation.getMethod().getDeclaringClass());
			logger.debug("calling proxy method: " + invocation.getMethod().getName());
		}
		if(serviceDef == null) {
			throw new SpiderException("",SpiderErrorNoConstant.ERROR_NO_SERVICE_NOT_DEFINED,
					SpiderErrorNoConstant.ERROR_INFO_SERVICE_NOT_DEFINED,"");
		}
		if(GlobalConfig.dev) {
			logger.debug("spider service id: [" + serviceDef.getServiceId() + "]");
		}
		Object[] args = invocation.getArguments();
		if(GlobalConfig.dev) {
			logger.debug("spider service args count:" + args.length);
		}
		if(args.length > 1 && !GlobalConfig.supportPlainParams) {
			throw new SpiderException(serviceDef.getServiceId(),SpiderErrorNoConstant.ERROR_NO_ONLY_SUPPORT_ONE_OBJECT_PARAM,
					SpiderErrorNoConstant.ERROR_INFO_ONLY_SUPPORT_ONE_OBJECT_PARAM,"");
		}
		
		SpiderBizHead head = null;
		if(args[0] instanceof SpiderBizHead) {
			head = ((SpiderBizHead)args[0]);
			/**
			 * 在调用端注入, 最小化服务端负载
			 */
			head.setServiceId(serviceDef.getServiceId());
		} else {
			if (args[0] instanceof ISpiderHead) {
				head = ((ISpiderHead)args[0]).getHead();
				head.setServiceId(serviceDef.getServiceId());
			}
		}
		
		if (head == null) {
			head = new SpiderBizHead();
		}
		
		if(StringUtils.isEmpty(head.getAppVersion())) {
			head.setAppVersion(GlobalConfig.appVersion);
		}
		final SpiderPacketHead packetHead = new SpiderPacketHead(head);
		Object param = args[0];
		/**
		 * 参数数量应该大于1, 而非0, 这样参数平铺或者对象可以在一个运行时同时支持
		 */
		if(GlobalConfig.supportPlainParams && serviceDef.paramNames.size() > 1) {
			// 枚举->map
			HashMap<String,Object> ldparam = new HashMap<String,Object>();
			for(int i=0;i<args.length;i++) {
				ldparam.put(serviceDef.paramNames.get(i), args[i]);
			}
			param = ldparam;
		}
		//前置过滤器
		for(BeforeFilter beforeFilter : GlobalConfig.beforeFilters) {
			beforeFilter.doFilter(packetHead, param);
		}
		
		final String arg;
		
		if(GlobalConfig.encrypt) {
			arg = AES128Utils.aesEncrypt(JsonUtils.toJson(param));
		} else {
			arg = JsonUtils.toJson(param);
		}
		
		packetHead.setServiceId(serviceDef.getServiceId());
		packetHead.setRpcMsgId(UUIDUtils.uuid());
		
		//对于broadcast的请求,直接转发所有下游workNode,不等待响应,立刻返回null
		if(serviceDef.getBroadcast() > 0) {
			/**
			 * 主动调用的时候, 主要是要达到同时调用本地和远程的目标, 前提是先实现为按名称注入的方式, 这个是实现NP作为发起者调用广播服务的关键
			 * 而这一调整又涉及到所有依赖注入可能的失败
			 */
			if(serviceDef.getBroadcast() == 1) {
				/**
				 * 广播但不等待响应
				 */
				for(Cluster cluster : GlobalConfig.getClusters().values()) {
//					if(GlobalConfig.broadcastMode == 1) {
//						if(cluster.isNeedReverseRegister()) {
//							// 非反向注册节点
//							continue;
//						}
//					} else if (GlobalConfig.broadcastMode == 2) {
//						//所有节点
//						//NOP
//					} else if (GlobalConfig.broadcastMode == 3) {
//						if(!cluster.isNeedReverseRegister()) {
//							continue;
//						}
//					}
					for(WorkNode workNode : cluster.getWorkNodes().values()) {
						SocketHelper.writeMessage(workNode.getRandomChannel(), SpiderPacketHead.getReqSpiderPacket(packetHead, arg));	
					}	
				}
				return null;
			} else {
				/**
				 * 广播并等待响应
				 */
				BroadcastResult broadcastResult = new BroadcastResult();
				if(GlobalConfig.isServer) {
					broadcastResult.setPort(GlobalConfig.port);
				}
				broadcastResult.setClusterName(GlobalConfig.clusterName);
				broadcastResult.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
				broadcastResult.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_SUCCESS);
				
				for(final Cluster cluster : GlobalConfig.getClusters().values()) {
//					if(GlobalConfig.broadcastMode == 1) {
//						if(cluster.isNeedReverseRegister()) {
//							continue;
//						}
//					} else if (GlobalConfig.broadcastMode == 2) {
//						//所有节点
//						//NOP
//					} else if (GlobalConfig.broadcastMode == 3) {
//						if(!cluster.isNeedReverseRegister()) {
//							continue;
//						}
//					}
					for(final WorkNode workNode : cluster.getWorkNodes().values()) {
						Channel channel = workNode.getRandomChannel();
						if(channel != null && StringUtils.isEmpty(broadcastResult.getIp())) {
							broadcastResult.setIp(((InetSocketAddress)channel.localAddress()).getHostString());
						} else if (channel == null) {
							/**
							 * 这里必须判断channel是否为null,否则在callService的时候调用call的时候就会导致调用了动态计算目标节点的逻辑,导致错误
							 */
							BroadcastResult tmpBroadcast = new BroadcastResult();
							tmpBroadcast.setClusterName(cluster.getClusterName());
							tmpBroadcast.setIp(workNode.getAddress());
							tmpBroadcast.setServer(true);
							tmpBroadcast.setPort(workNode.getPort());
							tmpBroadcast.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER);
							tmpBroadcast.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_DISCONNECT_FROM_SERVER);
							broadcastResult.getChildren().add(tmpBroadcast);
							continue;
						}
						final SpiderPacketHead tmpPacketHead = packetHead.copy();
						tmpPacketHead.setRpcMsgId(UUIDUtils.uuid());
						if(GlobalConfig.dev) {
							logger.debug(MessageFormat.format("preparing to call spider service [{0}] {1} ...",
									serviceDef.getServiceId(),
									invocation.getMethod().toString(),
									arg));
						}
						broadcastResult.getChildren().add(call(cluster,workNode,arg,serviceDef,tmpPacketHead));
					}
				}
				return broadcastResult;
			}
		}
		
		//判断是否为动态路由
		if(GlobalConfig.isDynamicRouteEnable) {
			RouteParser.calcDynamicRoute(packetHead,serviceDef);
		}
		if(GlobalConfig.dev) {
			logger.debug("spider service target server: " + serviceDef.getClusterName());
		}
		
		if (serviceDef.getClusterName().equals(NODE_NAME_LOCALSERVICE)) {
			throw new SpiderException(serviceDef.getServiceId(),SpiderErrorNoConstant.ERROR_NO_CANNOT_FOUND_SERVICE_IMPLEMENT,
					SpiderErrorNoConstant.ERROR_INFO_CANNOT_FOUND_SERVICE_IMPLEMENT,
					"spider service [" + serviceDef.getServiceId() + "]'s route is set to local, but there is no implemation or export path is incorrect!");
		} else if (serviceDef.getClusterName().equals("")) {
			throw new SpiderException(serviceDef.getServiceId(),SpiderErrorNoConstant.ERROR_NO_CANNOT_FORWARD,
					SpiderErrorNoConstant.ERROR_INFO_CANNOT_FORWARD,
					"spider service [" + serviceDef.getServiceId() + "] is set with incorrect route,who's clusterName is null, cannot forward!");
		} else {
			return callService(serviceDef,arg,null,packetHead);
		}  
	}
	
	private BroadcastResult call(final Cluster cluster,
								final WorkNode workNode,
								final String arg,
								final ServiceDefinition serviceDef,
								final SpiderPacketHead tmpPacketHead) {
		BroadcastResult tmpBroadcast = new BroadcastResult();
		tmpBroadcast.setClusterName(cluster.getClusterName());
		tmpBroadcast.setIp(workNode.getAddress());
		tmpBroadcast.setServer(true);
		tmpBroadcast.setPort(workNode.getPort());
		try {
			tmpBroadcast = (BroadcastResult) callService(serviceDef,arg,workNode.getRandomChannel(),tmpPacketHead);
		} catch (SpiderException e) {
			if(e.getErrorNo().equals(SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER)
					|| e.getErrorNo().equals(SpiderErrorNoConstant.ERROR_NO_TIMEOUT)) {
				tmpBroadcast.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER);
				tmpBroadcast.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_DISCONNECT_FROM_SERVER);
			}
		} catch (Exception e) {
			tmpBroadcast.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DISCONNECT_FROM_SERVER);
			tmpBroadcast.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_UNEXPECT_EXCEPTION);
			tmpBroadcast.setCause(e.getLocalizedMessage());
		}
		return tmpBroadcast;
	}

	/**
	 * 
	 * @param serviceDef
	 * @param arg
	 * @param channel
	 * @param packetHead
	 * @return
	 * @throws Exception
	 */
	public Object callService(ServiceDefinition serviceDef,
							String arg,
							Channel channel,
							SpiderPacketHead packetHead) throws Exception {
		//正常操作
		String retPacket;
		if(channel == null) {
			retPacket = SpiderRouter.call(packetHead, arg, serviceDef);
		} else {
			//绝大部分情况下,LdPack模式下会走,也有可能spider自身管理功能
			retPacket = SpiderRouter.call(packetHead, arg, channel,serviceDef.getTimeout());
		}
		if(retPacket == null) {
			throw new SpiderException(serviceDef.getServiceId(),SpiderErrorNoConstant.ERROR_NO_TIMEOUT,
					SpiderErrorNoConstant.getErrorInfo(SpiderErrorNoConstant.ERROR_NO_TIMEOUT),"调用服务[" + packetHead.getServiceId() + "]超时,"
					+ "该服务超时时间设置为" + serviceDef.getTimeout() + "毫秒,请检查本机和服务器日志！");
		}
		String retBiz = retPacket.substring(Integer.valueOf(retPacket.substring(SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET,SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET + SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN)));
		String errorNo = retPacket.substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
		if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_SUCCESS)) {
			if(GlobalConfig.encrypt) {
				retBiz = AES128Utils.aesDecrypt(retBiz);
			}
			
			//回写ServiceDefinition,便于快速判断是否泛型
			Object retObj = null;
			if(serviceDef.getRetType() instanceof ParameterizedType){
				ParameterizedType type = (ParameterizedType) serviceDef.getRetType();
				Class cls = (Class) type.getRawType();
				if(List.class.isAssignableFrom(cls)) {
					//泛型list反序列化
					retObj = JsonUtils.json2ListAppointed(retBiz,(Class)((ParameterizedType)serviceDef.getRetType()).getActualTypeArguments()[0]);
				} else {
					//泛型map反序列化
					retObj = JsonUtils.json2Object(retBiz,(Class)((ParameterizedType)serviceDef.getRetType()).getRawType());
				}
			} else {
				//POJO反序列化
				retObj = JsonUtils.json2Object(retBiz,(Class)serviceDef.getRetType());
			}
			//后置过滤器
			for(AfterFilter afterFilter : GlobalConfig.afterFilters) {
				afterFilter.doFilter(packetHead, arg,retObj);
			}
			return retObj;
		}
		if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_REQUEST_RECEIVED)) {
			return null;
		}
		String errorServer = "lack error server info";
		String cause = "";
		if (retBiz.contains(SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP)) {
			errorServer = retBiz.split(SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP)[0];
			if(StringUtils.removeEnd(retBiz, ";").contains(SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP)) {
				cause = StringUtils.removeEnd(retBiz, ";").split(SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP)[1];
			}
		}
		String errorInfo = SpiderErrorNoConstant.getErrorInfo(errorNo);
		if(errorInfo == null) {
			errorInfo = cause + ". " + SpiderErrorNoConstant.ERROR_SERVER_LABEL + errorServer;
		} else if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_BIZ_EXCEPTION)) {
			errorNo = cause.split(BIZ_ERROR_NO_AND_INFO_SEP)[0];
			errorInfo = cause.split(BIZ_ERROR_NO_AND_INFO_SEP).length == 2 ? cause.split(BIZ_ERROR_NO_AND_INFO_SEP)[1] : "";
		} else {
			errorInfo = MessageFormat.format(SpiderErrorNoConstant.getErrorInfo(errorNo),errorServer,cause);
		}
		throw new SpiderException(serviceDef.getServiceId(),errorNo,errorInfo);
	}

	@Override
	public Object getObject() throws Exception {
		return serviceProxy;
	}

	@Override
	public Class<?> getObjectType() {
		return super.getServiceInterface();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.serviceProxy = new ProxyFactory(super.getServiceInterface(),this).getProxy();
	}
}
