/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.stat;

import io.spider.pojo.ServiceDefinition;
import io.spider.pojo.ServiceDefinitionContainer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ServiceStatContainer {
	private static final Map<String,Map<String,ServiceStat>> threadServiceStatMap = new ConcurrentHashMap<String,Map<String,ServiceStat>>();
	
	static final ConcurrentLinkedQueue<SlowRequest> slowRequests = new ConcurrentLinkedQueue<SlowRequest>();
	
	public static void putStat(String threadName,String serviceId,long elapsedMsTime) {
		try {
			if (threadServiceStatMap.containsKey(threadName)) {
				if (threadServiceStatMap.get(threadName).containsKey(serviceId)) {
					threadServiceStatMap.get(threadName).get(serviceId).autoInc(elapsedMsTime);
				} else {
					ServiceStat stat = new ServiceStat(serviceId);
					stat.autoInc(elapsedMsTime);
					threadServiceStatMap.get(threadName).put(serviceId, stat);
				}
			} else {
				ServiceStat stat = new ServiceStat(serviceId);
				stat.autoInc(elapsedMsTime);
				threadServiceStatMap.put(threadName, new ConcurrentHashMap<String,ServiceStat>());
				threadServiceStatMap.get(threadName).put(serviceId, stat);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static List<ServiceStat> groupByServiceId() {
		List<ServiceStat> results = new ArrayList<ServiceStat>();
		Map<String,ServiceStat> sumServiceStat = new HashMap<String,ServiceStat>();
		for(ServiceDefinition sd : ServiceDefinitionContainer.getAllService().values()) {
			sumServiceStat.put(sd.getServiceId(), new ServiceStat(sd.getServiceId()));
		}
		for(Map<String,ServiceStat> threadServiceStats : threadServiceStatMap.values()) {
			for(ServiceStat serviceStat : threadServiceStats.values()) {
				if(sumServiceStat.containsKey(serviceStat.getServiceId())) {
					sumServiceStat.get(serviceStat.getServiceId()).addServiceStat(serviceStat);
				}
			}
		}
		results.addAll(sumServiceStat.values());
		return results;
	}
	
	public static List<ServiceStat> groupByServiceId(boolean filterZero) {
		List<ServiceStat> results = groupByServiceId();
		Iterator<ServiceStat> iter = results.iterator();
		while(iter.hasNext()) {
			ServiceStat stat = iter.next();
			if(stat.getExecCount() == 0) {
				iter.remove();
			}
		}
		return results;
	}


	public static void resetMetrics() {
		for(String threads : threadServiceStatMap.keySet()) {
			threadServiceStatMap.put(threads, new ConcurrentHashMap<String,ServiceStat>());
		}
	}
}
