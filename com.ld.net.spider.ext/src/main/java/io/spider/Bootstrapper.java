/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider;

import io.netty.channel.Channel;
import io.spider.channel.SocketClientHelper;
import io.spider.channel.SocketServerHelper;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.parser.ConfigParser;
import io.spider.pojo.Cluster;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.SpiderRequest;
import io.spider.pojo.WorkNode;
import io.spider.utils.JsonUtils;
import io.spider.worker.ConfigWatcher;
import io.spider.worker.ForceRecoverDispatcherThread;
import io.spider.worker.ReliableDispatcherThread;
import io.spider.worker.SpiderHeart;
import io.spider.worker.SpiderMonitorTimer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.Set;

import org.dom4j.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

@Service
public class Bootstrapper implements InitializingBean {
	
	@Autowired
	private BeanManagerHelper beanManagerHelper;
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static boolean isPortUsing(String host,int port) throws UnknownHostException{  
        boolean flag = false;  
        InetAddress theAddress = InetAddress.getByName(host);  
        try {  
            Socket socket = new Socket(theAddress,port); 
            flag = true;
            socket.close();
        } catch (IOException e) {  
            //连接不上是正常的 
        	// NOP
        } 
        return flag;  
    }
	
	public void start() throws Exception {
    	GlobalConfig.status = SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_STARTING;
    	
    	Document spiderDoc = ConfigParser.load();
		ConfigParser.parse(spiderDoc);
		
		//服务器模式下判断端口是否被占用,如果被占用则不允许启动
		if(GlobalConfig.isServer) {
			if(isPortUsing("localhost",GlobalConfig.port)) {
				logger.error("端口[" + GlobalConfig.port + "]已经被占用,linux下请使用lsof -i:" + GlobalConfig.port + "获取占用该端口的进程号！");
				System.exit(-1);
			}
		}
		
		connect2UpServers();
		
		if(GlobalConfig.isServer && GlobalConfig.reliable && !GlobalConfig.isServiceCenter) {
			BeanManagerHelper.createRedisConnBean();
			
			//启动redis连接,并读取全部未处理完的记录
			restoreRequest();
			
			//启动调度线程
			if(GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_FROM_REMOTE) {
				Thread recoveryThread = new Thread(new ForceRecoverDispatcherThread(),SpiderOtherMetaConstant.THREAD_NAME_RELIABLE_DISPATCHER_THREAD);
				recoveryThread.start();
				logger.warn("spider核心以forceRecovery=1强制恢复启动模式启动,spider服务端将不可用,为最小化数据不一致性和损坏,在全部待处理请求执行完成后,spider将自动正常退出.");
				GlobalConfig.status = SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_FORCE_RECOVER;
			} else {
				Thread reliableDispatcherThread = new Thread(new ReliableDispatcherThread(),SpiderOtherMetaConstant.THREAD_NAME_RELIABLE_DISPATCHER_THREAD);
				reliableDispatcherThread.start();
			}
		}
		
		if(GlobalConfig.isServer) {
			if (GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL) {
				SocketServerHelper.startSpiderServer();
				GlobalConfig.status = SpiderOtherMetaConstant.SPIDER_KERNEL_STATUS_STARTED;
			} else {
				//NOP
			}
		}
		
		if(!GlobalConfig.isServiceCenter) {
			register2ServiceCenter();
			Thread spiderHeartThread = new Thread(new SpiderHeart());
			spiderHeartThread.setDaemon(true);
			spiderHeartThread.start();
		}
		
		Thread configWatcherThread = new Thread(new ConfigWatcher(),"ConfigWatcher");
		configWatcherThread.setDaemon(true);
		configWatcherThread.start();
    }

	/**
	 * 从本地或远程redis恢复待处理请求到jvm queue供业务处理,以阻塞模式恢复确保请求的执行先进先出
	 */
	private void restoreRequest() {
		StringRedisTemplate redisTemplate = null;
		String keyPrefix = "";
		if(GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL) {
			keyPrefix = SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT;
			redisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE, StringRedisTemplate.class);
		} else if(GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_FROM_REMOTE) {
			keyPrefix = GlobalConfig.nodeId + "_" + SpiderOtherMetaConstant.REQ_PROCESS_STATUS_INIT;
			redisTemplate = BeanManagerHelper.getBean(SpiderOtherMetaConstant.BEAN_NAME_REMOTE_REDIS_TEMPLATE, StringRedisTemplate.class);
		} else {
			logger.error("暂不支持forceRecovery除0和1以外的取值！程序退出！");
			System.exit(-1);
		}
		
		Set<String> initReqKeys = redisTemplate.keys(keyPrefix + "*");
		if(GlobalConfig.dev) {
			logger.info(StringUtils.collectionToCommaDelimitedString(initReqKeys));
		}
		
		for(String req : redisTemplate.opsForValue().multiGet(initReqKeys)) {
			try {
				GlobalConfig.requestQueues.put((SpiderRequest) JsonUtils.json2Object(req, SpiderRequest.class));
			} catch (InterruptedException e) {
				logger.error("从" + (GlobalConfig.forceRecovery == SpiderOtherMetaConstant.SPIDER_RECOVER_LEVEL_NORMAL ? "本地" : "远程") + "存储恢复待处理请求失败！",e);
				System.exit(-1);
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		String jarFilePath = this.getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
		jarFilePath = java.net.URLDecoder.decode(jarFilePath, "UTF-8");
		logger.info("启动程序目录：" + jarFilePath);
		start();
	}

	private void register2ServiceCenter() {
		//SpiderMonitorTimer 自己会判断是否cloud模式, 并以此决定是否建立到sc的连接, 不过dump to local是独立的
		if(!GlobalConfig.isServiceCenter/* && GlobalConfig.isCloud*/) {
			Thread spiderMonitorThread = new Thread(new SpiderMonitorTimer());
			spiderMonitorThread.setDaemon(true);
			spiderMonitorThread.start();
		}
	}

	private void connect2UpServers() {
		logger.info(MessageFormat.format("初始启动创建连接至下游服务器,目标服务器集群数量:{0}!",GlobalConfig.getClusters().size()));
		for(Cluster server : GlobalConfig.getClusters().values()) {
			logger.info("开始创建至下游服务器[" + server.getClusterName() + "]的连接");
			for(WorkNode member : server.getWorkNodes().values()) {
				for (int i=0;i<member.getConnectionSize();i++) {
					Channel channel = SocketClientHelper.createChannel(member.getAddress(),member.getPort(),member.isSsl());
					if(channel == null) {
						logger.info("创建至下游服务器节点[" + member.getAddress() + ":" + member.getPort() + "]" + server.getClusterName() + "的连接失败");
						member.setConnected(false);
						continue;
					}
					member.addChannel(Thread.currentThread().getName(),channel);
					logger.info("创建至下游服务器节点[" + server.getClusterName() + "/" + member.getAddress() + ":" + member.getPort() + "]" + "的连接" + i + "成功");
				}
			}
		}
	}
}
