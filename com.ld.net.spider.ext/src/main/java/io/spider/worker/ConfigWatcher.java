package io.spider.worker;

import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.parser.ConfigParser;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.JsonUtils;

import org.dom4j.Document;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * spider中间件扩展包
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ConfigWatcher implements Runnable {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while (true) {
			try {
				Document configDoc = ConfigParser.load(false);
				if(configDoc != null) {
					Element nodeNameEle = configDoc.getRootElement().element(SpiderConfigName.ELE_NODE_NAME);
					GlobalConfig.slowLongTime = StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SLOW_LONG_TIME)) ? 100 : Integer.parseInt(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SLOW_LONG_TIME));
					boolean debugMode = StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_DEV)) ? false : (nodeNameEle.attributeValue(SpiderConfigName.ATTR_DEV).equals("true") ? true : false);
					if(!GlobalConfig.dev == debugMode) {
						GlobalConfig.dev = debugMode;
						logger.info("spider运行模式更新为" + debugMode + ".");
					}
					if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NOLOGGING_LIST))) {
						//NOP
					} else {
						String[] noLoggingServices = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_NOLOGGING_LIST),SpiderOtherMetaConstant.CONFIG_SEPARATOR);
						for(int i=0;i<noLoggingServices.length;i++) {
							GlobalConfig.noLoggingList.put(noLoggingServices[i], "");
						}
						logger.info("不记日志服务列表更新为:" + JsonUtils.toJson(GlobalConfig.noLoggingList));
					}
					
					if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOGGING_LIST))) {
						//NOP
					} else {
						String[] loggingServices = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_LOGGING_LIST),SpiderOtherMetaConstant.CONFIG_SEPARATOR);
						for(int i=0;i<loggingServices.length;i++) {
							GlobalConfig.loggingList.put(loggingServices[i], "");
						}
						logger.info("记日志服务列表更新为:" + JsonUtils.toJson(GlobalConfig.loggingList));
					}
					
					if (StringUtils.isEmpty(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SUPPRESS_ERROR_NO_LIST))) {
						//NOP
					} else {
						String[] suppressErrorNos = StringUtils.tokenizeToStringArray(nodeNameEle.attributeValue(SpiderConfigName.ATTR_SUPPRESS_ERROR_NO_LIST),SpiderOtherMetaConstant.CONFIG_SEPARATOR);
						for(int i=0;i<suppressErrorNos.length;i++) {
							GlobalConfig.suppressErrorNoList.put(suppressErrorNos[i], "");
						}
						logger.info("不记日志错误列表更新为:" + JsonUtils.toJson(GlobalConfig.suppressErrorNoList));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			try {
				Thread.sleep(60000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
