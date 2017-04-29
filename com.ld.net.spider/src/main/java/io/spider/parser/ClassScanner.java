/**
 * Licensed under the Apache License, Version 2.0
 */

package io.spider.parser;

import io.spider.BeanManagerHelper;
import io.spider.annotation.Service;
import io.spider.annotation.ServiceModule;
import io.spider.client.RpcServiceProxyImpl;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketPosConstant;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;
import io.spider.utils.JsonUtils;
import io.spider.utils.RefectionUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import com.ld.core.pack.LDConvert;
import com.ld.net.remoting.LDParam;
import com.ld.net.remoting.LDRequest;
import com.ld.net.remoting.LDService;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public class ClassScanner {
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	private static ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
	private static final String RESOURCE_PATTERN = "/**/*.class";  
	static final String EXPORT = "export";
	static final String PROXY = "proxy";
	private static Set<Class<?>> classSet = new HashSet<Class<?>>();
	
	private static TypeFilter ldTypeFilter = new AnnotationTypeFilter(LDService.class,false,true);
	private static TypeFilter typeFilter = new AnnotationTypeFilter(ServiceModule.class,false,true);
	
	public static void createExportService(String beanPackages) {
		logger.info("preparing to create auto export service, path " + beanPackages + " ...");
		Set<Class<?>> exportClasses = parse(EXPORT,beanPackages);
		// 适配器只能作为ANB使用,不支持提供实际服务
//		if(!GlobalConfig.needLdPackAdapter) {
			addServiceIntoContainer(true,exportClasses);
//		}
	}

	public static void createProxyService(String beanPackages) {
		logger.info("preparing to create auto proxy service, path " + beanPackages + " ...");
		Set<Class<?>> proxyClasses = parse(PROXY,beanPackages);
		addServiceIntoContainer(false,proxyClasses);
	}
	
	public static Set<Class<?>> parse(String serviceType,String beanPackages) {
		List<String> packagesToScan = Arrays.asList(StringUtils.tokenizeToStringArray(beanPackages, SpiderOtherMetaConstant.CONFIG_SEPARATOR));
		try {
			return getClassSet(serviceType,packagesToScan);
		} catch (ClassNotFoundException | IOException e) {
			logger.error("scanning " + serviceType + " package path " + packagesToScan.toArray().toString() + " failed, please check the config.",e);
			System.exit(-1);
		}
		return null;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void addServiceIntoContainer(boolean isExport, Set<Class<?>> serviceClasses) {
		for(Class clz : serviceClasses) {
			String subSystemId = null;
			short broadcast = 0;
			if(GlobalConfig.serviceDefineType.equals(SpiderOtherMetaConstant.SERVICE_DEFINE_TYPE_LD)) {
				LDService ldModuleAnno = (LDService) clz.getAnnotation(LDService.class);
				if(ldModuleAnno != null) {
					subSystemId = ldModuleAnno.subSystemId();
					broadcast = ldModuleAnno.broadcast();
				} else {
					ServiceModule moduleAnno = (ServiceModule) clz.getAnnotation(ServiceModule.class);
					if(moduleAnno != null) {
						subSystemId = moduleAnno.subSystemId();
						broadcast = moduleAnno.broadcast();
					}
				}
			} else {
				ServiceModule moduleAnno = (ServiceModule) clz.getAnnotation(ServiceModule.class);
				if(moduleAnno != null) {
					subSystemId = moduleAnno.subSystemId();
					broadcast = moduleAnno.broadcast();
				}
			}
			
			if(subSystemId == null) {
				continue;
			}
			// 20161130从createProxyService移动下来, 主要是为了获取ServiceModule上的broadcast属性判断根据名字注入还是类型注入
			if(!isExport) {
				BeanManagerHelper.registerBean(SpiderOtherMetaConstant.SPIDER_AUTO_PROXY_SERVICE_PREFIX + clz.getCanonicalName(),broadcast,clz, RpcServiceProxyImpl.class);
				logger.info("created " + SpiderOtherMetaConstant.SPIDER_AUTO_PROXY_SERVICE_PREFIX + clz.getCanonicalName());
			}
			
			for(Method method : Arrays.asList(clz.getDeclaredMethods())) {
				if(!GlobalConfig.serviceDefineType.equals(SpiderOtherMetaConstant.SERVICE_DEFINE_TYPE_LD)) {
					Service serviceAnno = method.getAnnotation(Service.class);
					if (serviceAnno != null) {
						if(method.getParameterTypes().length > 1) {
							logger.error("current version unsupport more than 1 arg, please wrap parameter in object");
							System.exit(-1);
						}
						if(!inServiceFilter(serviceAnno.serviceId(),isExport)) {
							continue;
						}
						ServiceDefinition service = new ServiceDefinition();
						service.setServiceId(org.apache.commons.lang3.StringUtils.rightPad(serviceAnno.serviceId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR))
								.setDesc(serviceAnno.desc())
								.setNeedLog(serviceAnno.needLog())
								.setTimeout(serviceAnno.timeout())
								.setHis(serviceAnno.his())
								.setBatch(serviceAnno.batch())
								.setMethod(method)
								.setClz(clz)
								.setSubSystemId(subSystemId)
								.setRetType(method.getGenericReturnType())
								.setParamTypes(method.getParameters())
								.setExport(isExport)
								.setBroadcast(serviceAnno.broadcast());
						if(isExport) {
							service.setClusterName(SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE);
						} else {
							if(service.getTimeout() == 0) {
								service.setTimeout(GlobalConfig.dev ? 10000000 : GlobalConfig.timeout);
							}
						}
						ServiceDefinitionContainer.addService(org.apache.commons.lang3.StringUtils.rightPad(serviceAnno.serviceId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR), service);
						logger.info("add " + (isExport ? EXPORT : PROXY) + " spider service " + service.toString() + "to container.");
					}
				} else {
					// ld模式下,两种注解都兼容,因为spider内部功能使用标准注解
					Service serviceAnno = method.getAnnotation(Service.class);
					if (serviceAnno != null) {
						if(method.getParameterTypes().length > 1) {
							logger.error("current version unsupport more than 1 arg, please wrap parameter in object");
							System.exit(-1);
						}
						if(!inServiceFilter(serviceAnno.serviceId(),isExport)) {
							continue;
						}
						ServiceDefinition service = new ServiceDefinition();
						service.setServiceId(org.apache.commons.lang3.StringUtils.rightPad(serviceAnno.serviceId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR))
								.setDesc(serviceAnno.desc())
								.setHis(serviceAnno.his())
								.setNeedLog(serviceAnno.needLog())
								.setTimeout(serviceAnno.timeout())
								.setHis(serviceAnno.his())
								.setBatch(serviceAnno.batch())
								.setMethod(method)
								.setClz(clz)
								.setSubSystemId(subSystemId)
								.setRetType(method.getGenericReturnType())
								.setParamTypes(method.getParameters())
								.setExport(isExport)
								.setBroadcast(serviceAnno.broadcast());
						if(isExport) {
							service.setClusterName(SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE);
						} else {
							if(service.getTimeout() == 0) {
								service.setTimeout(GlobalConfig.dev ? 10000000 : GlobalConfig.timeout);
							}
						}
						ServiceDefinitionContainer.addService(org.apache.commons.lang3.StringUtils.rightPad(serviceAnno.serviceId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR), service);
						logger.info("add " + (isExport ? EXPORT : PROXY) + " spider service " + service.toString() + "to container.");
						continue;
					}
					// 结束
					
					LDRequest serviceAnnoLD = method.getAnnotation(LDRequest.class);
					if (serviceAnnoLD != null) {
						if(method.getParameterTypes().length > 1 && !GlobalConfig.supportPlainParams) {
							logger.error("current version unsupport more than 1 arg, please wrap parameter in object");
							System.exit(-1);
						}
						if(!inServiceFilter(serviceAnnoLD.methodId(),isExport)) {
							continue;
						}
						ServiceDefinition service = new ServiceDefinition();
						service.setServiceId(org.apache.commons.lang3.StringUtils.rightPad(serviceAnnoLD.methodId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR))
								.setDesc(serviceAnnoLD.desc())
								.setHis(serviceAnnoLD.his())
								.setBatch(serviceAnnoLD.batch())
								.setNeedLog(serviceAnnoLD.needLog())
								.setTimeout(serviceAnnoLD.timeout())
								.setMethod(method)
								.setClz(clz)
								.setSubSystemId(subSystemId)
								.setRetType(method.getGenericReturnType())
								.setParamTypes(method.getParameters())
								.setExport(isExport)
								.setBroadcast(serviceAnnoLD.broadcast());
						/**
						 * 仅对方法参数也为平铺的方法进行平铺式加载
						 */
						if(GlobalConfig.supportPlainParams && method.getParameters().length > 1) {
							if(Map.class.isAssignableFrom(method.getParameters()[0].getType())) {
								//NOP
							} else {
								logger.info("LdPack兼容模式解析方法[" + method.toString() + "]");
								service.paramConverts = new ArrayList<LDConvert>();
								service.paramNames = new ArrayList<String>();
								for (int i=0;i<method.getParameters().length;i++) {    
									LDParam ldp = method.getParameters()[i].getAnnotation(LDParam.class);
									if(ldp == null) {
										logger.warn("parameter [" + method.getParameters()[i].getName()+ "]'s LDParam Annotation is undefine！");
										continue;
									}
									service.paramConverts.add(LDConvert.getConvert(method.getParameters()[0].getType()));
									service.paramNames.add(ldp.value());
									service.putFieldByLdName(ldp.value(), null);
								}
							}
							if(service.getRetType() instanceof ParameterizedType){
								ParameterizedType type = (ParameterizedType) service.getRetType();
								Class cls = (Class) type.getRawType();
								if(List.class.isAssignableFrom(cls)) {
									service.isList = true;
									service.itemClass = (Class) type.getActualTypeArguments()[0];
								}
							} else {
								service.itemClass = (Class) service.getRetType();
							}
							service.itemGetters = new ArrayList<Method>();
							service.itemNames = new ArrayList<String>();
							if(!Map.class.isAssignableFrom(service.itemClass)){
								try {
									BeanInfo beanInfo = Introspector.getBeanInfo(service.itemClass);
									PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
									for (PropertyDescriptor property : propertyDescriptors) {    
										String key = property.getName();
										if (key.compareToIgnoreCase("class") == 0) { 
											continue;  
										}
										Field f = RefectionUtils.getField(service.itemClass, key);
										if (f == null) {
											// 部分字段定义不符合POJO规范，比如com.ld.net.secuact.SecuritiesAccountData.D_funcl_QueryUniteQuitiesPositionTop.Nav_ratio
											f = RefectionUtils.getField(service.itemClass, key.substring(0,1).toUpperCase() + key.substring(1));
										}
										if (f != null) {
											service.itemNames.add(f.getName());
											service.itemGetters.add(property.getReadMethod());
										} else {
											logger.error(service.itemClass.getCanonicalName() + " has no field " + key);
										}
									}
								} catch (IntrospectionException e) {
									logger.error("parse return value failed",e);
									System.exit(-1);
								}
							}
						}
						
						if(isExport) {
							service.setClusterName(SpiderOtherMetaConstant.NODE_NAME_LOCALSERVICE);
						} else {
							if(service.getTimeout() == 0) {
								service.setTimeout(GlobalConfig.dev ? 10000000 : GlobalConfig.timeout);
							}
						}
						ServiceDefinitionContainer.addService(org.apache.commons.lang3.StringUtils.rightPad(serviceAnnoLD.methodId(), SpiderPacketPosConstant.SPIDER_SERVICE_ID_LEN, SpiderOtherMetaConstant.DEFAULT_SPIDER_PACKET_HEAD_PAD_CHAR), service);
						logger.info("add " + (isExport ? EXPORT : PROXY) + " spider service " + service.toString() + "to container.");
					}
				}
			}
		}
	}

	/**
	 * @param serviceId
	 * @param isExport
	 * @return
	 */
	private static boolean inServiceFilter(String serviceId, boolean isExport) {
		if (isExport) {
			if(GlobalConfig.exportServices == null || GlobalConfig.exportServices.isEmpty()) {
				return true;
			} else {
				for(String filterServiceId : GlobalConfig.exportServices) {
					if(serviceId.equals(filterServiceId) || 
							Pattern.matches(filterServiceId.replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), serviceId)) {
						return true;
					}
				}
			}
			return false;
		} else {
			if(GlobalConfig.proxyServices == null || GlobalConfig.proxyServices.isEmpty()) {
				return true;
			} else {
				for(String filterServiceId : GlobalConfig.proxyServices) {
					if(serviceId.equals(filterServiceId) || 
							Pattern.matches(filterServiceId.replace("*", "[\\s\\S]+").replace("?", "[\\s\\S]"), serviceId)) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * 将符合条件的Bean以Class集合的形式返回
	 * @param serviceType 
	 * 
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static Set<Class<?>> getClassSet(String serviceType, List<String> packagesToScan) throws IOException, ClassNotFoundException {
		logger.info("packagesToScan:" + JsonUtils.toJson(packagesToScan));
		classSet.clear();
		if (!packagesToScan.isEmpty()) {
			for (String pkgPath : packagesToScan) {
				String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
						+ ClassUtils.convertClassNameToResourcePath(pkgPath)
						+ RESOURCE_PATTERN;
				logger.info("pattern:" + pattern);
				Resource[] resources = resourcePatternResolver.getResources(pattern);
				MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resourcePatternResolver);
				for (Resource resource : resources) {
					if (resource.isReadable()) {
						MetadataReader reader = readerFactory.getMetadataReader(resource);
						String className = reader.getClassMetadata().getClassName();
						logger.debug("className:" + className);
						if (matchesEntityTypeFilter(reader, readerFactory)) {
							if(!pkgPath.startsWith("io.spider") && className.startsWith("io.spider")) {
								logger.info("在自动加载服务路径" + pkgPath + "下跳过io.spider下的类" + className + ".");
								continue;
							}
							boolean isBreak = false;
							if(serviceType.equals(EXPORT)) {
								for (String excludePkg : ConfigParser.excludeExportPkgs) {
									if(className.startsWith(excludePkg)) {
										logger.info("跳过排除自动加载的类" + className);
										isBreak = true;
										break;
									}
								}
								if (isBreak) {
									continue;
								}
							} else {
								isBreak = false;
								for (String excludePkg : ConfigParser.excludeProxyPkgs) {
									if(className.startsWith(excludePkg)) {
										logger.info("跳过排除自动加载的类" + className);
										isBreak = true;
										break;
									}
								}
								if (isBreak) {
									continue;
								}
							}
							classSet.add(Class.forName(className));
							logger.info("find auto " + serviceType + " service interface: " + className);
						} else {
							logger.warn(className + "不匹配,理论上应该不会发生!");
						}
					}
				}
			}
		} else {
			logger.warn("packagesToScan is empty!");
		}
		return classSet;
	}
	
	private static boolean matchesEntityTypeFilter(MetadataReader reader, MetadataReaderFactory readerFactory) throws IOException {  
		if(GlobalConfig.serviceDefineType.toLowerCase().equals("ld")) {
			if (ldTypeFilter.match(reader, readerFactory)) {  
	            return true;  
	        }
			if (typeFilter.match(reader, readerFactory)) {  
	            return true;  
	        }
	        return false;
		} else {
			if (typeFilter.match(reader, readerFactory)) {  
	            return true;  
	        }
	        return false;  
		}
    }
}
