/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider;

import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.pojo.GlobalConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

@Service
public class BeanManagerHelper implements ApplicationContextAware{
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static ApplicationContext applicationContext;
	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		BeanManagerHelper.applicationContext = applicationContext;
		if(BeanManagerHelper.applicationContext == null) {
			logger.error("applicationContext is null,spider runtime initialize failed!");
			System.exit(-1);
		}
	}

	@SuppressWarnings("rawtypes")
	public static void registerBean(String beanName,int autowireMode,Class serviceInterface,Class bean) {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
	    // 获取bean工厂并转换为DefaultListableBeanFactory
	    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
	    RootBeanDefinition rbd = new RootBeanDefinition(bean, autowireMode == 0 ? AbstractBeanDefinition.AUTOWIRE_BY_TYPE : AbstractBeanDefinition.AUTOWIRE_BY_NAME, true);
	    MutablePropertyValues propertyValues = new MutablePropertyValues();
	    propertyValues.add("serviceInterface", serviceInterface);
	    rbd.setPropertyValues(propertyValues);
	    beanFactory.registerBeanDefinition(beanName, rbd);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object getBean(Class serviceInterface) {
		return applicationContext.getBean(serviceInterface);
	}
	
	/**
	 * 本接口主要用于某服务既提供本地实现、又有远程代理时使用, 一般用于特殊用途，比如广播，并非供普通场景使用
	 * @param beanName
	 * @param requiredType
	 * @param serviceLocationType  1: 本地; 2: 远程;
	 * @return
	 */
	public static <T> T getBean(String beanName,Class<T> requiredType,int serviceLocationType) {
		return applicationContext.getBean(beanName, requiredType);
	}
	
	public static <T> T getBean(String beanName,Class<T> requiredType) {
		return applicationContext.getBean(beanName, requiredType);
	}

	public static void createRedisConnBean() {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
	    // 获取bean工厂并转换为DefaultListableBeanFactory
	    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
	    
		//动态注册连接redis的bean
		RootBeanDefinition rbd = new RootBeanDefinition(JedisConnectionFactory.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, true);
		MutablePropertyValues propertyValues = new MutablePropertyValues();
	    propertyValues.add("hostName", "localhost");
	    propertyValues.add("port", "6379");
	    propertyValues.add("usePool", false);
	    rbd.setPropertyValues(propertyValues);
	    rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
	    beanFactory.registerBeanDefinition("localRedisConnFactory", rbd);
	    
	    propertyValues = new MutablePropertyValues();
	    ConstructorArgumentValues cargs = new ConstructorArgumentValues();
	    cargs.addIndexedArgumentValue(0, applicationContext.getBean("localRedisConnFactory"));
		rbd = new RootBeanDefinition(StringRedisTemplate.class, cargs, propertyValues);
	    rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
	    beanFactory.registerBeanDefinition(SpiderOtherMetaConstant.BEAN_NAME_LOCAL_REDIS_TEMPLATE, rbd);
	    
	    if(GlobalConfig.ha) {
	    	rbd = new RootBeanDefinition(JedisConnectionFactory.class, AbstractBeanDefinition.AUTOWIRE_BY_NAME, true);
			propertyValues = new MutablePropertyValues();
		    propertyValues.add("hostName", GlobalConfig.haRemoteServerAddress[0]);
		    propertyValues.add("port", GlobalConfig.haRemoteServerAddress[1]);
		    propertyValues.add("password", GlobalConfig.remoteServerPassword);
		    propertyValues.add("usePool", false);
		    rbd.setPropertyValues(propertyValues);
		    rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		    beanFactory.registerBeanDefinition("remoteRedisConnFactory", rbd);
		    
		    propertyValues = new MutablePropertyValues();
		    cargs = new ConstructorArgumentValues();
		    cargs.addIndexedArgumentValue(0, applicationContext.getBean("remoteRedisConnFactory"));
			rbd = new RootBeanDefinition(StringRedisTemplate.class, cargs, propertyValues);
		    rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		    beanFactory.registerBeanDefinition(SpiderOtherMetaConstant.BEAN_NAME_REMOTE_REDIS_TEMPLATE, rbd);
	    }
	}

	public static void createNodeProxyForManagedServer() {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
	    // 获取bean工厂并转换为DefaultListableBeanFactory
	    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
	    try {
			RootBeanDefinition rbd = new RootBeanDefinition(BeanManagerHelper.class.getClassLoader().loadClass("com.ld.net.spider.sc.client.impl.NodeManageProxyImpl"), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
			rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		    beanFactory.registerBeanDefinition("nodeManageProxy", rbd);
		} catch (ClassNotFoundException e) {
			logger.error("cloud mode load class com.ld.net.spider.sc.client.impl.NodeManageProxyImpl failed, please ensure the container jar in classpath*!");
		}
	}
	
	public static void createOtherManageBeanForManagedServer() {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
	    // 获取bean工厂并转换为DefaultListableBeanFactory
	    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
	    try {
			RootBeanDefinition rbd = new RootBeanDefinition(BeanManagerHelper.class.getClassLoader().loadClass("com.ld.net.spider.manage.impl.OtherManageImpl"), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
			rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		    beanFactory.registerBeanDefinition("otherManage", rbd);
		} catch (ClassNotFoundException e) {
			logger.error("load class com.ld.net.spider.manage.impl.OtherManageImpl failed, please ensure the container jar in classpath*!");
		}
	}

	/**
	 * 
	 */
	public static void createMonitorServiceForManagedServer() {
		ConfigurableApplicationContext configurableApplicationContext = (ConfigurableApplicationContext) applicationContext;
	    // 获取bean工厂并转换为DefaultListableBeanFactory
	    DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) configurableApplicationContext.getBeanFactory();
	    try {
			RootBeanDefinition rbd = new RootBeanDefinition(BeanManagerHelper.class.getClassLoader().loadClass("com.ld.net.spider.monitor.service.SpiderMonitorServiceImpl"), AbstractBeanDefinition.AUTOWIRE_BY_TYPE, true);
			rbd.setDependencyCheck(AbstractBeanDefinition.DEPENDENCY_CHECK_NONE);
		    beanFactory.registerBeanDefinition("spiderMonitorService", rbd);
		} catch (ClassNotFoundException e) {
			logger.error("load class com.ld.net.spider.monitor.service.SpiderMonitorServiceImpl failed, please ensure the container jar in classpath*!");
		}
	}
}
