package io.spider;

import static io.spider.meta.SpiderErrorNoConstant.ERROR_INFO_ILLEGAL_ARGUMENT;
import static io.spider.meta.SpiderErrorNoConstant.ERROR_INFO_SERVICE_NOT_DEFINED;
import static io.spider.meta.SpiderErrorNoConstant.ERROR_NO_ILLEGAL_ARGUMENT;
import static io.spider.meta.SpiderErrorNoConstant.ERROR_NO_SERVICE_NOT_DEFINED;
import io.spider.exception.SpiderException;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.utils.JsonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 * @since 1.0.8 支持异步callback
 */
public class SpiderClient {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static Object call(String serviceId,Object args) {
		ServiceDefinition service = ServiceDefinitionContainer.getService(serviceId);
		if(service == null) {
			throw new SpiderException(serviceId,ERROR_NO_SERVICE_NOT_DEFINED,ERROR_INFO_SERVICE_NOT_DEFINED);
		}
		try {
			/**
			 * 自动获取远程的bean注入, 防止本地、远程均存在的情况下出错
			 */
			return service.getMethod().invoke(BeanManagerHelper.getBean(SpiderOtherMetaConstant.SPIDER_AUTO_PROXY_SERVICE_PREFIX + service.getClz().getName(),service.getClz()), args);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			if (e.getCause() instanceof SpiderException) {
				if(!GlobalConfig.suppressErrorNoList.containsKey(((SpiderException) e.getCause()).getErrorNo())) {
					logger.error("服务编号" + serviceId + ",错误信息：" + e.getMessage() + "," + e.toString() + "," + e.getCause());
					logger.error("参数：" + JsonUtils.toJson(args));
				}
				throw (SpiderException) e.getCause();
			} else {
				logger.error("",e);
				throw new SpiderException(ERROR_NO_ILLEGAL_ARGUMENT,
										ERROR_INFO_ILLEGAL_ARGUMENT,
										MessageFormat.format("形参个数:{0}, 实参个数:{1}. ", service.getMethod().getParameterCount(),1));
			}
		}
	}
	
	public static Object call(String serviceId,Object[] args,boolean flag) {
		ServiceDefinition service = ServiceDefinitionContainer.getService(serviceId);
		if(service == null) {
			throw new SpiderException(serviceId,ERROR_NO_SERVICE_NOT_DEFINED,ERROR_INFO_SERVICE_NOT_DEFINED);
		}
		try {
			return service.getMethod().invoke(BeanManagerHelper.getBean(service.getClz()), args);
		} catch (IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			if (e.getCause() instanceof SpiderException) {
				if(!GlobalConfig.suppressErrorNoList.containsKey(((SpiderException) e.getCause()).getErrorNo())) {
					logger.error("服务编号" + serviceId + ",错误信息：" + e.getMessage() + "," + e.toString() + "," + e.getCause());
					logger.error("参数：" + JsonUtils.toJson(args));
				}
				throw (SpiderException) e.getCause();
			} else {
				logger.error("",e);
				throw new SpiderException(ERROR_NO_ILLEGAL_ARGUMENT,
										ERROR_INFO_ILLEGAL_ARGUMENT,
										MessageFormat.format("形参个数:{0}, 实参个数:{1}. 形参类型列表:{2}" + System.lineSeparator() + "实参类型列表:{3}", 
															service.getMethod().getParameterCount(),
															args.length,
															JsonUtils.toJson(service.getDisplayParamTypes()),
															JsonUtils.toJson(typeName(args))));
			}
		}
	}
	
	public static List<String> typeName(Object[] objs) {
		List<String> typeNames = new ArrayList<String>();
		for(int i=0;i<objs.length;i++) {
			typeNames.add(objs[i].getClass().getCanonicalName());
		}
		return typeNames;
	}
}
