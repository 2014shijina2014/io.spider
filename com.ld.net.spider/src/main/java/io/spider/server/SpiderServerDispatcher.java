/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.server;

import static io.spider.meta.SpiderOtherMetaConstant.ADDRESS_AND_PORT_SEP;
import static io.spider.meta.SpiderOtherMetaConstant.BIZ_ERROR_NO_AND_INFO_SEP;
import static io.spider.meta.SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN;
import static io.spider.meta.SpiderOtherMetaConstant.DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
import static io.spider.meta.SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT;
import static io.spider.meta.SpiderOtherMetaConstant.SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP;
import static io.spider.meta.SpiderPacketHead.getReqSpiderPacket;
import static io.spider.meta.SpiderPacketHead.getRespSpiderPacket;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN;
import static io.spider.meta.SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.spider.BeanManagerHelper;
import io.spider.SpiderRouter;
import io.spider.channel.SocketHelper;
import io.spider.exception.SpiderException;
import io.spider.filter.AfterFilter;
import io.spider.filter.BeforeFilter;
import io.spider.meta.ISpiderHead;
import io.spider.meta.SpidePacketVarHeadTagName;
import io.spider.meta.SpiderBizHead;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.meta.SpiderPacketPosConstant;
import io.spider.meta.SpiderServiceIdConstant;
import io.spider.parser.RouteParser;
import io.spider.plugin.PluginProcessor;
import io.spider.plugin.SpiderPacketPluginReq;
import io.spider.plugin.SpiderPacketPluginResp;
import io.spider.plugin.SpiderPluginTemplate;
import io.spider.pojo.BroadcastResult;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ParallelBaseRespTemplate;
import io.spider.pojo.RouteItemForTcpDump;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.pojo.SpiderBaseResp;
import io.spider.pojo.SpiderRequest;
import io.spider.pojo.TcpDumpContainer;
import io.spider.pojo.WorkNode;
import io.spider.stat.ServiceStatHelper;
import io.spider.utils.AES128Utils;
import io.spider.utils.CRCUtils;
import io.spider.utils.JsonUtils;
import io.spider.utils.ThreadUtils;
import io.spider.utils.UUIDUtils;
import io.spider.worker.ReliableWorkerThread;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.ld.core.pack.utils.CastUtils;
import com.ld.net.remoting.RemotingException;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class SpiderServerDispatcher {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static String localAddress = SpiderOtherMetaConstant.HOSTNAME; 
	
	static {
		try {
			String ip = InetAddress.getByName(localAddress).getHostAddress();
			localAddress = localAddress + "/" + ip; 
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		localAddress = localAddress + ADDRESS_AND_PORT_SEP + GlobalConfig.port;
	}
	
	public static int execute(Channel channel, InetSocketAddress address,String spiderReqPacket,StringBuilder spiderRespPacketSB) {
		String retJsonStr = "";
		String tmpParamJsonStr = "";
		
		if(spiderReqPacket.length() < SpiderPacketPosConstant.SPIDER_FIXED_PACKET_HEAD_SIZE) {
			try {
				spiderRespPacketSB.append(SpiderErrorNoConstant.ERROR_NO_PACKET_LESS_THAN_MIN_SIZE);
				return DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
			} catch (Exception e) {
				logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
			}
		}
 		SpiderPacketHead packetHead = new SpiderPacketHead();
		StringBuilder spiderPacketBodySB = new StringBuilder();
		@SuppressWarnings("unused")
		boolean success = SpiderPacketHead.splitSpiderPacket(spiderReqPacket,packetHead,spiderPacketBodySB);
		String spiderPacketBody = spiderPacketBodySB.toString();
		/*if(!success) {
			retJsonStr = getErrorSpiderHead(serviceDefinition,
					GlobalConfig.encrypt,
					SpiderErrorNoConstant.ERROR_NO_CRC32_CHECK_FAIL,
					SpiderPacketPosConstant.ERROR_INFO_CRC32_CHECK_FAIL,
					rpcMsgId);
			return getSpiderPacket(serviceId,rpcMsgId,retJsonStr);
		}
		*/
		final ServiceDefinition serviceDef = ServiceDefinitionContainer.getService(packetHead.getServiceId());
		if(serviceDef == null) {
			if(!(packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_HEARTBEAT) || packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_PROTOCOL))) {
				logger.warn("没有找到服务[" + packetHead.getServiceId() + "]的定义,如果是spider自身服务,则忽略,否则请检查spider配置文件发布包配置!");
			}
			if(!packetHead.getServiceId().startsWith(SpiderServiceIdConstant.SPIDER_INTERNAL_SERVICE_ID_PREFIX)) {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SERVICE_NOT_DEFINED);
				spiderRespPacketSB.append(getRespSpiderPacket(packetHead, localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
				return DISPATCHER_RET_CODE_RETURN;
			}
		}
		
		try {
			if(!packetHead.getCrc32().equals(CRCUtils.getCRC32Value(spiderPacketBody,GlobalConfig.charset))) {
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CRC32_CHECK_FAIL);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
				}
			}
			
			try {
				if(GlobalConfig.encrypt) 
					tmpParamJsonStr = AES128Utils.aesDecrypt(spiderPacketBody);
				else 
					tmpParamJsonStr = spiderPacketBody;
			} catch (Exception e1) {
				logger.error("spider请求正文解密失败！", e1);
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DECRYPT_FAIL);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
				}
			}
			
			if(GlobalConfig.status < SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_STARTED) {
				logger.warn(MessageFormat.format("spider runtime is initializing or shutdownning, please wait and try later. current service id is {0},request is {1}.",serviceDef.getServiceId(),spiderPacketBody));
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_SPIDER_IS_INITIALIZING);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
				}
			}
			
			if(SpiderServiceIdConstant.internalServices.contains(packetHead.getServiceId())) {
				if(GlobalConfig.dev) {
					logger.debug("spider内部管理服务" + packetHead.getServiceId() + ".");
				}
				if(packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_PROTOCOL)) {
					if (SpiderServerAuthServiceImpl.verifyProtocol(tmpParamJsonStr)) {
						if(GlobalConfig.anonymous) {
							GlobalConfig.addSourceWorkNode(address.getAddress().getHostAddress(), address.getPort(),channel);
							spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK) : SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK));
						} else {
							spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE2_AUTH) : SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE2_AUTH));
						}
						return DISPATCHER_RET_CODE_RETURN;
					} else {
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION;
					}
//				} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_REVERSE_REGISTER)) {
//					//反向注册,服务器连接从正常的A(客户端)->B(服务器)模式的基础上,增加B(服务器)->A(客户端)->B(服务器)的连接建立模式
//					if((tmpParamJsonStr.indexOf(ADDRESS_AND_PORT_SEPARATOR)<=0) || (tmpParamJsonStr.indexOf(CLUSTERNAME_AND_ADDRESS_SEPARATOR)<=0)) {
//						packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_INVALID_PACKET);
//						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEPARATOR + SpiderOtherMetaConstant.NOTHING));
//						return DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
//					} else {
//						try {
//							String clusterName = tmpParamJsonStr.split(CLUSTERNAME_AND_ADDRESS_SEPARATOR)[0];
//							String ip = tmpParamJsonStr.split(CLUSTERNAME_AND_ADDRESS_SEPARATOR)[1].split(ADDRESS_AND_PORT_SEPARATOR)[0];
//							int port = Integer.parseInt(tmpParamJsonStr.split(CLUSTERNAME_AND_ADDRESS_SEPARATOR)[1].split(ADDRESS_AND_PORT_SEPARATOR)[1]);
//							//不必提前建立连接,心跳线程和业务线程会运行时自动建立
//							GlobalConfig.getClusters().putIfAbsent(clusterName, new Cluster());
//							GlobalConfig.getCluster(clusterName).addWorkNode(new WorkNode(ip,port,false));
//							logger.info(MessageFormat.format("收到反向注册请求{0},将在第一次心跳和请求运行时建立相应的连接通道！",tmpParamJsonStr));
//							// 目标节点不进行认证, 因为基本上仅仅用于广播场景
//							GlobalConfig.addSourceWorkNode(address.getAddress().getHostAddress(), address.getPort(),channel);
//							spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK) : SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK));
//							return DISPATCHER_RET_CODE_RETURN;
//						} catch (Exception e) {
//							logger.warn(MessageFormat.format("反向注册服务器{0}失败！",tmpParamJsonStr),e);
//							return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION;
//						}
//					}
				} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_AUTH)) {
					if(SpiderServerAuthServiceImpl.verifyLicense(tmpParamJsonStr)) {
						GlobalConfig.addSourceWorkNode(address.getAddress().getHostAddress(), address.getPort(),channel);
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK) : SpiderOtherMetaConstant.SPIDER_INTERNAL_MSG_HANDSHAKE_ACK));
						logger.info("[" + address.toString() + "]在可信模式下认证通过");
						return DISPATCHER_RET_CODE_RETURN;
					} else {
						logger.error("可信模式下认证不通过: [" + packetHead.toString() + "," + tmpParamJsonStr + "]");
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION;
					}
				} else if ( packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT)
						|| packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_STOP_INTERCEPT)
						|| packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_HEARTBEAT)
						|| packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT_PULL) ) {
					if(!GlobalConfig.existsSourceWorkNode(address.getAddress().getHostAddress(), address.getPort())) {
						try {
							packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_UNAUTH_ACCESS);
							spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
							return DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
						} catch (Exception e) {
							logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
						}
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
					} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_HEARTBEAT)) {
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,spiderPacketBody));
						return DISPATCHER_RET_CODE_RETURN;
					} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT_PULL)) {
						List<String> tcpDumps = new ArrayList<String>();
						String msg;
						if(TcpDumpContainer.tcpDumps.get(channel) != null) { 
							while((msg = TcpDumpContainer.tcpDumps.get(channel).poll()) != null) {
								tcpDumps.add(msg);
							}
						}
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,JsonUtils.toJson(tcpDumps)));
						return DISPATCHER_RET_CODE_RETURN;
					} else if (packetHead.getServiceId().equals(SpiderServiceIdConstant.SERVICE_ID_STOP_INTERCEPT)) {
						if(GlobalConfig.isInterceptExist(channel)) {
							GlobalConfig.tcpDumpClients.remove(channel);
							if(TcpDumpContainer.tcpDumps.containsKey(channel)) {
								TcpDumpContainer.tcpDumps.remove(channel);
							}
						}
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,JsonUtils.toJson(new SpiderBaseResp())));
						return DISPATCHER_RET_CODE_RETURN;
					} else {
						if(GlobalConfig.tcpdump) {
							if(GlobalConfig.isInterceptExist(channel)) {
								GlobalConfig.tcpDumpClients.remove(channel);
							}
							GlobalConfig.addInterceptClient(channel, tmpParamJsonStr);
							spiderRespPacketSB.append(getRespSpiderPacket(packetHead,JsonUtils.toJson(new SpiderBaseResp())));
							return DISPATCHER_RET_CODE_RETURN;
						} else {
							try {
								packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_TCP_DUMP_IS_DISABLED);
								spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
								return DISPATCHER_RET_CODE_RETURN;
							} catch (Exception e) {
								logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
							}
						}
					}
				}
			} else {
				//这一段仅作为服务中心角色时会用到, 服务中心发送给托管服务器消息返回的请求,这里必须提前处理掉,否则走正常逻辑会导致转发时因为clusterName为null而出现异常
				//同时必须收到消息后及时把消息放置到队列中,否则客户端会一直等待超时
				if (GlobalConfig.isServiceCenter) {
					if(packetHead.getMsgType() == SpiderOtherMetaConstant.MSG_TYPE_RESP) {
						try {
							SpiderRouter.spiderMultiplex.get(packetHead.getRpcMsgId()).put(spiderReqPacket);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
					}
				}
				
				if(!GlobalConfig.existsSourceWorkNode(address.getAddress().getHostAddress(), address.getPort())) {
					try {
						packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_UNAUTH_ACCESS);
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
						return DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
					} catch (Exception e) {
						logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
					}
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
				}
			}
			
			//对于broadcast的请求,直接转发所有下游workNode,不等待响应,也不返回给上游响应
			if(serviceDef.getBroadcast() > 0) {
				if(serviceDef.getBroadcast() == 1) {
					//无响应广播
					dispatch2LocalService(serviceDef,packetHead,spiderPacketBody,spiderRespPacketSB);
					for(Cluster cluster : GlobalConfig.getClusters().values()) {
//						// 广播服务被动调用时, 仅发送给非反向注册节点, 这样可以保证不会陷入死循环
//						if(cluster.isNeedReverseRegister()) {
//							continue;
//						}
						for(WorkNode workNode : cluster.getWorkNodes().values()) {
							SocketHelper.writeMessage(workNode.getRandomChannel(), spiderReqPacket);	
						}	
					}
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
				} else {
					//等待响应的广播请求
					final String spiderPacketBodyForBroadcast = spiderPacketBody;
					
					int retCode = dispatch2LocalService(serviceDef,packetHead,spiderPacketBodyForBroadcast,spiderRespPacketSB);
					
					String retPacket = spiderRespPacketSB.toString();
					if(retCode == DISPATCHER_RET_CODE_RETURN) {
						BroadcastResult broadcastResult = new BroadcastResult();
						broadcastResult.setServer(true);
						broadcastResult.setClusterName(GlobalConfig.clusterName);
						broadcastResult.setIp(((InetSocketAddress)channel.localAddress()).getHostString());
						broadcastResult.setPort(((InetSocketAddress)channel.localAddress()).getPort());
						
						String retBiz = retPacket.substring(SpiderPacketPosConstant.SPIDER_FIXED_PACKET_HEAD_SIZE);
						String errorNo = retPacket.substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
						if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_SUCCESS)) {
							if(GlobalConfig.encrypt) {
								try {
									retBiz = AES128Utils.aesDecrypt(retBiz);
									BroadcastResult tmpResult = JsonUtils.json2Object(retBiz,BroadcastResult.class);
									broadcastResult.setData(tmpResult.getData());
								} catch (Exception e) {
									broadcastResult.setErrorNo(SpiderErrorNoConstant.ERROR_NO_DECRYPT_FAIL);
									broadcastResult.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_DECRYPT_FAIL);
									broadcastResult.setCause(e.getMessage());
									logger.error("",e);
								}
							} else {
								BroadcastResult tmpResult = JsonUtils.json2Object(retBiz,BroadcastResult.class);
								broadcastResult.setData(tmpResult.getData());
							}
						} else {
							/**
							 * 本地的执行失败不影响远程执行,故无需return
							 */
							broadcastResult.setErrorNo(errorNo);
							broadcastResult.setErrorInfo(SpiderErrorNoConstant.getErrorInfo(errorNo));
						}
						
						for(final Cluster cluster : GlobalConfig.getClusters().values()) {
//							if(cluster.isNeedReverseRegister()) {
//								continue;
//							}
							for(final WorkNode workNode : cluster.getWorkNodes().values()) {
								final SpiderPacketHead tmpPacketHead = packetHead.copy();
								tmpPacketHead.setRpcMsgId(UUIDUtils.uuid());
								broadcastResult.getChildren().add(call(cluster,workNode,tmpPacketHead,spiderPacketBodyForBroadcast));
							}
						}
						logger.debug(packetHead.toString() + "的广播请求已执行完成");
						retJsonStr = (GlobalConfig.encrypt ? AES128Utils.aesEncrypt(JsonUtils.toJson(broadcastResult)) : JsonUtils.toJson(broadcastResult));
						spiderRespPacketSB.delete(0, spiderRespPacketSB.length()).append(getRespSpiderPacket(packetHead,retJsonStr));
						return DISPATCHER_RET_CODE_RETURN;
					} else {
						return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
					}
				}
			}
			
			if(GlobalConfig.tcpdump) {
				boolean abort = false;
				for(Entry<Channel, CopyOnWriteArrayList<RouteItemForTcpDump>> interceptor : GlobalConfig.tcpDumpClients.entrySet()) {
					for(RouteItemForTcpDump routeItem : interceptor.getValue()) {
						if (!abort && routeItem.getAction() == 2) {
							abort = true;
						}
						if (Pattern.matches(routeItem.getServiceId().replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), packetHead.getServiceId().trim())
								&& Pattern.matches(routeItem.getCompanyId().replace("*", "\\S+").replace("?", "\\S"), packetHead.getCompanyId().trim() == null ? "*" : packetHead.getCompanyId().trim())
								&& Pattern.matches(routeItem.getAppVersion().replace("*", "\\S+").replace("?", "\\S"), packetHead.getAppVersion().trim() == null ? "*" : packetHead.getAppVersion().trim())
								&& Pattern.matches(routeItem.getSubSystemId().replace("*", "[0-9]+").replace("?", "[0-9]"), ServiceDefinitionContainer.getService(packetHead.getServiceId()).getSubSystemId())
								&& Pattern.matches(routeItem.getSystemId().replace("*", "\\S+").replace("?", "\\S"),  packetHead.getSystemId().trim() == null ? "*" : packetHead.getSystemId().trim()) ){
							String tmpParam = spiderReqPacket.substring(SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET,SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET + packetHead.getPacketHeadSize()) + tmpParamJsonStr;
							// push: tcpdump主推模式, pull： tcpdump查询模式
							if(GlobalConfig.tcpDumpMode.equals(SpiderOtherMetaConstant.TCP_DUMP_MODE_PUSH)) {
								/**
								 * 20161125更新
								 * 这个直接用原有的packetHead可能就不合适了, 因为引入了插件之后, 是需要把packetHead传递给插件的, 插件可能据此进行处理
								 */
								SpiderPacketHead dumpPushHead = packetHead.copy()
																.setServiceId(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT_PUSH)
																.setRpcMsgId(UUIDUtils.uuid());
								ChannelFuture ch = SocketHelper.writeMessage(interceptor.getKey(), getReqSpiderPacket(dumpPushHead, tmpParam));
								if(ch == null) {
									logger.info(MessageFormat.format("到抓包客户端{0}的连接断开！", ((InetSocketAddress)interceptor.getKey().remoteAddress()).toString()));
									try {
										interceptor.getKey().deregister().sync();
										interceptor.getKey().disconnect().sync();
										interceptor.getKey().close().sync();
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									GlobalConfig.tcpDumpClients.remove(interceptor.getKey());
								}
							} else {
								TcpDumpContainer.tcpDumps.putIfAbsent(interceptor.getKey(), new LinkedBlockingQueue<String>(100000));
								while(!TcpDumpContainer.tcpDumps.get(interceptor.getKey()).offer(tmpParam)) {
									TcpDumpContainer.tcpDumps.get(interceptor.getKey()).poll();
								}
							}
							break;
						}
					}
				}
				if (abort) {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_REQUEST_ABORTED);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				}
			}
			/**
			 * 插件处理
			 */
			if(GlobalConfig.plugins.size() == 0) {
				//NOP
			} else {
				SpiderPacketPluginReq req = new SpiderPacketPluginReq();
				req.setSpiderPacketHead(packetHead);
				req.setDecryptRequestBody(tmpParamJsonStr);
				SpiderPacketPluginResp resp = PluginProcessor.doService(req);
				if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_GO_ON) {
					packetHead = resp.getSpiderPacketHead();
					spiderPacketBody = GlobalConfig.encrypt ? AES128Utils.aesEncrypt(resp.getDecryptRespBody()) : resp.getDecryptRespBody();
				} else if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_PARALLEL) {
					// 重新计算报文头放在插件中, 这样就可以确保插件对主环境没有API层面的污染, 因为返回值的时候, 使用的是原报文头,而不是插件可能修改过的报文头
					// 这里需要使用线程池发请求,因为不能确定将在本地处理还是会发到远程处理
					if(GlobalConfig.reliable) {
						logger.warn("可信模式不建议并行运行: " + JsonUtils.toJson(packetHead));
						//可信模式,返回请求已收到的响应包,落地后立刻返回
						for(Entry<SpiderPacketHead,String> entry : resp.getDecryptRespBodies().entrySet()) {
							saveSpiderRequestForReliableMode(entry.getKey(),entry.getValue(),spiderRespPacketSB);
							if(entry.getKey().getErrorNo().equals(SpiderErrorNoConstant.ERROR_NO_REQUEST_SAVE_FAILED)) {
								packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_REQUEST_SAVE_FAILED);
								break;
							}
						}
						return DISPATCHER_RET_CODE_RETURN;
					}
					ExecutorService exc = ThreadUtils.getFixedThreadPool(Integer.parseInt(packetHead.getSpiderOpts().get(SpidePacketVarHeadTagName.PARALLEL_DEGREE)));
					List<Future<String>> futures = new ArrayList<Future<String>>();
					for(final Entry<SpiderPacketHead,String> entry : resp.getDecryptRespBodies().entrySet()) {
						Future<String> future = exc.submit(new Callable<String>() {
							@Override
							public String call() {
								StringBuilder sb = new StringBuilder();
								int retCode = dispatch(serviceDef,entry.getKey(),entry.getValue(),sb);
								if(SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL == retCode) {
									logger.error("并行执行发生不可预期的致命异常");
									throw new RuntimeException("并行执行发生不可预期的致命异常");
								}
								return sb.toString();
							}
						});
						futures.add(future);
					}
					List<ParallelBaseRespTemplate> parallelBaseRespList = new ArrayList<ParallelBaseRespTemplate>();
					for (Future<String> future : futures) {
						ParallelBaseRespTemplate parallelBaseResp = new ParallelBaseRespTemplate();
						parallelBaseRespList.add(parallelBaseResp);
						try {
							String result = future.get();
							String errorNo = result.substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
							parallelBaseResp.setErrorNo(errorNo);
							parallelBaseResp.setErrorInfo(SpiderErrorNoConstant.getErrorInfo(errorNo));
							if(errorNo.equals(SpiderErrorNoConstant.ERROR_NO_SUCCESS)) {
								try {
									int packetBodyBegin = Integer.valueOf(result.substring(SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET, SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET + SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN));
									result = result.substring(packetBodyBegin);
									JsonNode parallelBaseRespNode = SpiderPluginTemplate.mapper.readTree(result);
									parallelBaseResp.setClusterName(parallelBaseRespNode.findValue("clusterName").asText());
									parallelBaseResp.setIp(parallelBaseRespNode.findValue("ip").asText());
									parallelBaseResp.setPort(Integer.parseInt(parallelBaseRespNode.findValue("port").asText()));
									JsonNode dataNode = parallelBaseRespNode.get(0).get("data");
									parallelBaseResp.setData(dataNode);								
								} catch (IOException e) {
									logger.error("jackson解析并行执行返回值出错：",result);
									parallelBaseResp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_PARSE_JSON_STRING_FAILED);
								}
							}
						} catch (InterruptedException | ExecutionException e) {
							logger.error("",e);
							parallelBaseResp.setErrorNo(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION);
						}
					}
					retJsonStr = (GlobalConfig.encrypt ? AES128Utils.aesEncrypt(JsonUtils.toJson(parallelBaseRespList)) : JsonUtils.toJson(parallelBaseRespList));
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,retJsonStr));
					return DISPATCHER_RET_CODE_RETURN;
				} else if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION) {
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_CLOSE_CONNECTION;
				} else if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL) {
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
				} else if(resp.getDispatchRetCode() == SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP) {
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_NOP;
				} else if(resp.getDispatchRetCode() == DISPATCHER_RET_CODE_RETURN) {
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(resp.getDecryptRespBody()) : resp.getDecryptRespBody()));
					return DISPATCHER_RET_CODE_RETURN;
				} else if(resp.getDispatchRetCode() == DISPATCHER_RET_CODE_RETURN_AND_CLOSE) {
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,GlobalConfig.encrypt ? AES128Utils.aesEncrypt(resp.getDecryptRespBody()) : resp.getDecryptRespBody()));
					return DISPATCHER_RET_CODE_RETURN_AND_CLOSE;
				} else {
					return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
				}
			}
			// 非可信模式业务调度
			if(GlobalConfig.reliable == false) {
				int retCode = dispatch(serviceDef,packetHead,spiderPacketBody,spiderRespPacketSB);
				if(GlobalConfig.tcpdump) {
					for(Entry<Channel, CopyOnWriteArrayList<RouteItemForTcpDump>> interceptor : GlobalConfig.tcpDumpClients.entrySet()) {
						for(RouteItemForTcpDump routeItem : interceptor.getValue()) {
							if (Pattern.matches(routeItem.getServiceId().replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), packetHead.getServiceId().trim())
									&& Pattern.matches(routeItem.getCompanyId().replace("*", "\\S+").replace("?", "\\S"), packetHead.getCompanyId().trim() == null ? "*" : packetHead.getCompanyId().trim())
									&& Pattern.matches(routeItem.getAppVersion().replace("*", "\\S+").replace("?", "\\S"), packetHead.getAppVersion().trim() == null ? "*" : packetHead.getAppVersion().trim())
									&& Pattern.matches(routeItem.getSubSystemId().replace("*", "[0-9]+").replace("?", "[0-9]"), ServiceDefinitionContainer.getService(packetHead.getServiceId()).getSubSystemId())
									&& Pattern.matches(routeItem.getSystemId().replace("*", "\\S+").replace("?", "\\S"),  packetHead.getSystemId().trim() == null ? "*" : packetHead.getSystemId().trim()) ){
								String tmpParam = spiderRespPacketSB.toString();
								if (GlobalConfig.encrypt) {
									try {
										tmpParam = tmpParam.substring(SPIDER_PACKET_HEAD_SIZE_OFFSET, SPIDER_PACKET_HEAD_SIZE_LEN) + AES128Utils.aesDecrypt(tmpParam.substring(SPIDER_PACKET_HEAD_SIZE_LEN));
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
								// push: tcpdump主推模式, pull： tcpdump查询模式
								if(GlobalConfig.tcpDumpMode.equals(SpiderOtherMetaConstant.TCP_DUMP_MODE_PUSH)) {
									SpiderPacketHead dumpPushHead = packetHead.copy()
																	.setServiceId(SpiderServiceIdConstant.SERVICE_ID_INTERCEPT_PUSH)
																	.setRpcMsgId(UUIDUtils.uuid());
									ChannelFuture ch = SocketHelper.writeMessage(interceptor.getKey(), getReqSpiderPacket(dumpPushHead, tmpParam));
									if(ch == null) {
										logger.info(MessageFormat.format("到抓包客户端{0}的连接断开！", ((InetSocketAddress)interceptor.getKey().remoteAddress()).toString()));
										try {
											interceptor.getKey().deregister().sync();
											interceptor.getKey().disconnect().sync();
											interceptor.getKey().close().sync();
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										GlobalConfig.tcpDumpClients.remove(interceptor.getKey());
									}
								} else {
									TcpDumpContainer.tcpDumps.putIfAbsent(interceptor.getKey(), new LinkedBlockingQueue<String>(100000));
									while(!TcpDumpContainer.tcpDumps.get(interceptor.getKey()).offer(tmpParam)) {
										TcpDumpContainer.tcpDumps.get(interceptor.getKey()).poll();
									}
								}
								break;
							}
						}
					}
				}
				return retCode;
			} else {
				//可信模式,返回请求已收到的响应包,落地后立刻返回
				return saveSpiderRequestForReliableMode(packetHead,spiderPacketBody,spiderRespPacketSB);
			}
		} catch (Exception e) {
			logger.error("发生不可预知异常",e);
			try {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION);
				spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + e.getMessage()));
				return DISPATCHER_RET_CODE_RETURN;
			} catch (Exception e1) {
				logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
			}
		}
		return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
	}
	
	private static BroadcastResult call(final Cluster cluster,final WorkNode workNode,final SpiderPacketHead tmpPacketHead,final String spiderPacketBodyForBroadcast) {
		BroadcastResult result = new BroadcastResult();
		result.setServer(true);
		result.setClusterName(cluster.getClusterName());
		result.setIp(workNode.getAddress());
		result.setPort(workNode.getPort());
		try {
			String retPacket = SpiderRouter.call(tmpPacketHead, 
												spiderPacketBodyForBroadcast, 
												workNode.getRandomChannel(),
												120000);
			if(retPacket == null) {
				result.setErrorNo(SpiderErrorNoConstant.ERROR_NO_TIMEOUT);
				result.setErrorInfo(SpiderErrorNoConstant.ERROR_INFO_TIMEOUT);
				return result;
			}
			String retBiz = retPacket.substring(Integer.valueOf(retPacket.substring(SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET,SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_OFFSET + SpiderPacketPosConstant.SPIDER_PACKET_HEAD_SIZE_LEN)));
			String errorNo = retPacket.substring(SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET, SpiderPacketPosConstant.SPIDER_ERROR_NO_OFFSET + SpiderPacketPosConstant.SPIDER_ERROR_NO_LEN);
			if (errorNo.equals(SpiderErrorNoConstant.ERROR_NO_SUCCESS)) {
				if(GlobalConfig.encrypt) {
					retBiz = AES128Utils.aesDecrypt(retBiz);
				}
				
				result = JsonUtils.json2Object(retBiz,BroadcastResult.class);
				return result;
			}
			String errorServer = "lack error server info";
			String cause = "";
			if (retBiz.contains(";")) {
				errorServer = retBiz.split(";")[0];
				if(StringUtils.removeEnd(retBiz, ";").contains(";")) {
					cause = StringUtils.removeEnd(retBiz, ";").split(";")[1];
				}
			}
			result.setErrorNo(errorNo);
			result.setErrorInfo(MessageFormat.format(SpiderErrorNoConstant.getErrorInfo(errorNo), cause, errorServer));
		} catch (SpiderException e) {
			result.setErrorNo(e.getErrorNo());
			result.setErrorInfo(e.getErrorInfo());
			result.setCause(e.getDetail());
		} catch (Exception e) {
			result.setCause(e.getMessage());
		} 
		return result;
	}
	
	/**
	 * @param packetHead
	 * @param spiderPacketBody
	 * @param spiderRespPacketSB
	 * @return
	 */
	private static int saveSpiderRequestForReliableMode(
			SpiderPacketHead packetHead, String spiderPacketBody,
			StringBuilder spiderRespPacketSB) {
		try {
			SpiderRequest req = new SpiderRequest();
			req.setRequestBody(spiderPacketBody);
			req.setSpiderPacketHead(packetHead);
			if(saveSpiderRequest(req)) {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_REQUEST_RECEIVED);
				spiderRespPacketSB.append(getRespSpiderPacket(packetHead,SpiderOtherMetaConstant.NOTHING));
			} else {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_REQUEST_SAVE_FAILED);
				spiderRespPacketSB.append(getRespSpiderPacket(packetHead,SpiderOtherMetaConstant.NOTHING));
			}
		} catch (Exception e) {
			logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
		}
		return DISPATCHER_RET_CODE_RETURN;
	}

	public static int dispatch(ServiceDefinition service,SpiderPacketHead packetHead,String spiderPacketBody,StringBuilder spiderRespPacketSB) {
		if(GlobalConfig.dev) {
			logger.debug((GlobalConfig.reliable == true ? "[可信]" : "[常规]") + "模式下开始执行请求[" + packetHead.toString() + "]");
			logger.debug(service == null ? packetHead.getServiceId() + "'s ServiceDefinition instance is null." : service.toString());
		}
		if (service.getClusterName() != null && service.getClusterName().equals(SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE)) { //调用本地服务
			return dispatch2LocalService(service,packetHead,spiderPacketBody,spiderRespPacketSB);
		} else {
			// 正常转发调用spider服务,充当AR的角色
			try {
				//前置过滤器
				if (GlobalConfig.beforeFilters.size() > 0) {
					StringBuilder sb = new StringBuilder(spiderPacketBody);
					for(BeforeFilter beforeFilter : GlobalConfig.beforeFilters) {
						beforeFilter.doFilter(packetHead, new StringBuilder(sb));
					}
					spiderPacketBody = sb.toString();
				}
				
				if(GlobalConfig.isDynamicRouteEnable) {
					RouteParser.calcDynamicRoute(packetHead, service);
				}
				if(StringUtils.isEmpty(service.getClusterName())) {
					logger.error("service " + service.getServiceId() + "'s route is empty, cannot forward!");
					try {
						packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CANNOT_FORWARD);
						spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
						return DISPATCHER_RET_CODE_RETURN;
					} catch (Exception e1) {
						logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
					}
				}
				if(GlobalConfig.dev) {
					logger.debug("正常转发调用spider服务:" + packetHead.toString() + ",目标节点：" + service.getClusterName());
				}
				String retPacket = SpiderRouter.call(packetHead, spiderPacketBody,service);
				if(retPacket != null) {
					//后置过滤器
					if (GlobalConfig.beforeFilters.size() > 0) {
						StringBuilder sb = new StringBuilder(spiderPacketBody);
						StringBuilder sbRet = new StringBuilder(spiderPacketBody);
						for(AfterFilter afterFilter : GlobalConfig.afterFilters) {
							afterFilter.doFilter(packetHead, sb,sbRet);
						}
						retPacket = sbRet.toString();
					}
					spiderRespPacketSB.append(retPacket);
				} else {
					//调用服务发生超时
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_TIMEOUT);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
				}
				return DISPATCHER_RET_CODE_RETURN;
			} catch (Exception e) {
				logger.error("正常转发调用spider服务失败！", e);
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_FORWARD_FAILED);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e1) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
				}
			}
		}
		return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
	}

	/**
	 * @return
	 */
	private static int dispatch2LocalService(ServiceDefinition service,SpiderPacketHead packetHead,String spiderPacketBody,StringBuilder spiderRespPacketSB) {
		Object bean = null;
		String retJsonStr = "";
		try {
//			if(service.getBroadcast() > 0 && !service.getServiceId().startsWith(SpiderServiceIdConstant.SPIDER_INTERNAL_SERVICE_ID_PREFIX)) {
//				bean = BeanManagerHelper.getBean(Introspector.decapitalize(service.getClz().getSimpleName()), service.getClz());
//			} else {
				bean = BeanManagerHelper.getBean(service.getClz());
//			}
		} catch (BeansException e) {
			logger.error("can not found bean implement or bean definition is duplicate, bean type [" + service.getClz().getCanonicalName() + "],this problem usually occured in situation where <context:component-scan base-package=\"path2class\" /> shall put in root web application xml, but now put in mvc web application xml." + e);
			try {
				packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CANNOT_FOUND_SERVICE_IMPLEMENT);
				spiderRespPacketSB.append(getRespSpiderPacket(packetHead,retJsonStr));
			} catch (Exception e1) {
				logger.error("生成Spider错误响应失败！永远不会发生此异常！", e);
			}
			return DISPATCHER_RET_CODE_RETURN;
		}
		if(GlobalConfig.dev) {
			logger.debug("本节点处理spider请求：" + packetHead.toString());
		}
		try {
			String tmpParamJsonStr = "";
			if(GlobalConfig.encrypt) 
				tmpParamJsonStr = AES128Utils.aesDecrypt(spiderPacketBody);
			else 
				tmpParamJsonStr = spiderPacketBody;
			
			// 保护无参服务, 对于使用java自动注入客户端编写的服务, 因为RpcServiceProxy做了保护, 不会发生
			// 对于第三方远程调用的, 可能出现此情况, 增加额外兼容性
			Object retObj = null;
			Object param = null;
			long beg = 0;
			/**
			 * 参数数量应该大于1, 而非0, 这样参数平铺或者对象可以在一个运行时同时支持
			 */
			if(GlobalConfig.supportPlainParams && service.paramTypes.length > 1) {
				param = JsonUtils.json2Object(tmpParamJsonStr, HashMap.class);
				beg = System.currentTimeMillis();
				//前置过滤器
				for(BeforeFilter beforeFilter : GlobalConfig.beforeFilters) {
					beforeFilter.doFilter(packetHead, param);
				}
				// map->枚举
				Object[] params = new Object[service.paramNames.size()];
				int index = 0;
				for(String paramName : service.paramNames) {
					if (service.paramTypes[index].getType() == String.class) {
						params[index] = CastUtils.toString(((HashMap<String, Object>)param).get(paramName));
					} else if (service.paramTypes[index].getType() == Integer.class) {
						params[index] = CastUtils.toInteger(((HashMap<String, Object>)param).get(paramName));
					} else if (service.paramTypes[index].getType() == BigDecimal.class) {
						params[index] = CastUtils.toBigDecimal(((HashMap<String, Object>)param).get(paramName));
					} else if (service.paramTypes[index].getType() == BigInteger.class) {
						params[index] = CastUtils.toInteger(((HashMap<String, Object>)param).get(paramName));
					} else if (service.paramTypes[index].getType() == Long.class) {
						params[index] = CastUtils.toLong(((HashMap<String, Object>)param).get(paramName));
					} else {
						logger.error(paramName + "的类型为:" + service.paramTypes[index].getType() + ",暂不受支持!");
					}
					index++;
				}
				
				retObj = service.getMethod().invoke(bean, params);
				
				//后置过滤器
				for(AfterFilter afterFilter : GlobalConfig.afterFilters) {
					afterFilter.doFilter(packetHead, param,retObj);
				}
			} else {
				param = service.getParamType() == null ? null : JsonUtils.json2Object(tmpParamJsonStr, service.getParamType().getType());
				//设置SpiderBizHead默认值为空
				//20170204 1.0.7-RELEASE版本增加
				if(param instanceof SpiderBizHead) {
					if(((SpiderBizHead)param).getCompanyId().equals(SpiderOtherMetaConstant.DEFAULT_COMPANY_ID)) {
						((SpiderBizHead)param).setCompanyId("");
					}
				} else {
					if (param instanceof ISpiderHead) {
						if(((ISpiderHead)param).getHead().getCompanyId().equals(SpiderOtherMetaConstant.DEFAULT_COMPANY_ID)) {
							((ISpiderHead)param).getHead().setCompanyId("");
						}
					}
				}
				beg = System.currentTimeMillis();
				if(param != null) {
					//前置过滤器
					for(BeforeFilter beforeFilter : GlobalConfig.beforeFilters) {
						beforeFilter.doFilter(packetHead, param);
					}
					retObj = service.getMethod().invoke(bean, param);
				} else {
					retObj = service.getMethod().invoke(bean);
				}
				
				//后置过滤器
				for(AfterFilter afterFilter : GlobalConfig.afterFilters) {
					afterFilter.doFilter(packetHead, param,retObj);
				}
			}
			
			retJsonStr = (GlobalConfig.encrypt ? AES128Utils.aesEncrypt(JsonUtils.toJson(retObj)) : JsonUtils.toJson(retObj));
			long end = System.currentTimeMillis();
			if(end-beg >= GlobalConfig.slowLongTime) {
				ServiceStatHelper.writeSlowRequest(beg,end,packetHead,spiderPacketBody,SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE);
			}
			ServiceStatHelper.putStat(Thread.currentThread().getName(),packetHead.getServiceId(),end-beg);
			spiderRespPacketSB.append(getRespSpiderPacket(packetHead,retJsonStr));
			return DISPATCHER_RET_CODE_RETURN;
		} catch (Exception e) {
			if (e.getCause() instanceof SpiderException || e instanceof SpiderException) {
				SpiderException se = null;
				if (e.getCause() instanceof SpiderException) {
					se = (SpiderException)e.getCause();
				} else {
					se = (SpiderException)e;
				}
				String errorNo = se.getErrorNo();
				if(!GlobalConfig.suppressErrorNoList.containsKey(errorNo)) {
					logger.warn("调用服务" + service.getServiceId() + "发生未捕获SpiderException异常！", e);
				}
				try {
					packetHead.setErrorNo(errorNo);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + se.getDetail()));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e1) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
				}
			} else if (e.getCause() instanceof RemotingException || e instanceof RemotingException) {
				RemotingException re = null;
				if (e.getCause() instanceof RemotingException) {
					re = (RemotingException)e.getCause();
				} else {
					re = (RemotingException)e;
				}
				String errorNo = re.getCode();
				if(!GlobalConfig.suppressErrorNoList.containsKey(errorNo)) {
					/**
					 * 对于业务异常, 只返回错误信息, 不打印错误堆栈
					 */
					logger.warn("调用服务" + service.getServiceId() + "发生未捕获RemotingException业务异常！");
					logger.warn("错误码：" + errorNo + ", 错误描述：" + re.getMessage());
				}
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_BIZ_EXCEPTION);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + errorNo + BIZ_ERROR_NO_AND_INFO_SEP + re.getMessage()));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e1) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
				}
			} else if (e != null 
					&& ((e.getMessage() != null && e.getMessage().contains("CannotGetJdbcConnectionException")) || 
							(e.getCause() != null && e.getCause().getMessage() != null && e.getCause().getMessage().contains("CannotGetJdbcConnectionException")))) {
				logger.error("调用服务" + service.getServiceId() + "发生无法获取数据库连接异常！", e);
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_CANNOT_GET_JDBC_CONN);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + SpiderOtherMetaConstant.NOTHING));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e1) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
				}
			} else { 
				logger.error("调用服务" + service.getServiceId() + "发生未知运行时异常，确保服务捕获了异常！", e);
				try {
					packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION);
					spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP + (e.getCause() == null ? e.getMessage() : e.getCause().getMessage())));
					return DISPATCHER_RET_CODE_RETURN;
				} catch (Exception e1) {
					logger.error("生成Spider错误响应失败！永远不会发生此异常！", e1);
				}
			}
		}
		/**
		 * 20170308 修改, 任何业务异常都不会导致连接异常而关闭, 返回999而非断开连接
		 */
		// return SpiderOtherMetaConstant.DISPATCHER_RET_CODE_FATAL;
		packetHead.setErrorNo(SpiderErrorNoConstant.ERROR_NO_UNEXPECT_EXCEPTION);
		spiderRespPacketSB.append(getRespSpiderPacket(packetHead,localAddress + SOCKET_ENDPOINT_AND_ERROR_CAUSE_SEP));
		return DISPATCHER_RET_CODE_RETURN;
	}

	public static boolean saveSpiderRequest(SpiderRequest req) {
		StringRedisTemplate remoteRedisTemplate = null;
		if(GlobalConfig.ha) {
			remoteRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_REMOTE_REDIS_TEMPLATE, StringRedisTemplate.class);
			try {
				remoteRedisTemplate.opsForValue().set(ReliableWorkerThread.getRemotePersistKey(GlobalConfig.nodeId,REQ_PROCESS_STATUS_INIT,req.getRequestId()), JsonUtils.toJson(req));
			} catch (Exception e) {
				logger.error("可信模式下保存到远程持久化存储失败！",e);
				return false;
			}
		}
		
		StringRedisTemplate localRedisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE,StringRedisTemplate.class);
		try {
			localRedisTemplate.opsForValue().set(ReliableWorkerThread.getLocalPersistKey(REQ_PROCESS_STATUS_INIT,req.getRequestId()), JsonUtils.toJson(req));
		} catch (Exception e) {
			logger.error("可信模式下保存到本地持久化存储失败！",e);
			if(GlobalConfig.ha) {
				try {
					remoteRedisTemplate.delete(ReliableWorkerThread.getRemotePersistKey(GlobalConfig.nodeId,REQ_PROCESS_STATUS_INIT,req.getRequestId()));
				} catch (Exception e1) {
					logger.error("可信模式下本地持久化存储失败时回滚远程持久化存储失败！",e1);
				}
			}
			return false;
		}
		
		boolean isSuccess = false;
		
		try {
			isSuccess = GlobalConfig.requestQueues.offer(req, GlobalConfig.timeout, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			isSuccess = false;
			e.printStackTrace();
		}
		
		if(isSuccess) {
			//NOP
		} else {
			try {
				localRedisTemplate.delete(ReliableWorkerThread.getLocalPersistKey(REQ_PROCESS_STATUS_INIT,req.getRequestId()));
				if(GlobalConfig.ha) {
					remoteRedisTemplate.delete(ReliableWorkerThread.getRemotePersistKey(GlobalConfig.nodeId,REQ_PROCESS_STATUS_INIT,req.getRequestId()));
				}
			} catch (Exception e) {
				logger.error("可信模式下回滚本地/远程持久化存储失败！",e);
			}
		}
		return isSuccess;
	}
}