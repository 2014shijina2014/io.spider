/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.stat;

import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.meta.SpiderPacketHead;
import io.spider.pojo.GlobalConfig;
import io.spider.utils.DateUtils;
import io.spider.utils.JsonUtils;
import io.spider.utils.Obj2MapUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;

/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */

public class ServiceStatHelper {
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	static ReentrantLock lock = new ReentrantLock(); 
	
	static MongoClient mongoClient;
	static DB database;
	static DBCollection collection;

	public static void putStat(String threadName, String serviceId, long elapsedMsTime) {
		ServiceStatContainer.putStat(threadName, serviceId, elapsedMsTime);
	}

	public static void writeSlowRequest(long beg, long end,
			SpiderPacketHead head, String msgBody, String remoteAddress) {
		ServiceStatContainer.slowRequests.add(new SlowRequest(head, remoteAddress, remoteAddress, beg, end, end-beg));
		try {
			if(ServiceStatContainer.slowRequests.size() > 200) {
				if (lock.tryLock() || lock.tryLock(1, TimeUnit.MICROSECONDS)) {
					if(ServiceStatContainer.slowRequests.size() > 200) {
						if (GlobalConfig.logOutput.equals(SpiderOtherMetaConstant.LOG_OUTPUT_FILE)) {
							String logDir = System.getenv("SPIDER_LOG");
							if (logDir == null) {
								File dir = new File("/tmp/spider/stat/" + GlobalConfig.clusterName + "_" + DateUtils.SDF_DATE.format(new Date()));
								logDir = dir.getAbsolutePath();
								if (!dir.exists() || !dir.isDirectory()) {
									dir.mkdirs();
								}
							}
							File file = new File(logDir + "/slow.log");
							if (!file.exists()) {
								try {
									file.createNewFile();
								} catch (IOException e) {
									logger.error("创建慢请求文件" + logDir + "/slow.log" + "失败",e);
								}
							}
							
							try {
								BufferedWriter output = new BufferedWriter(new FileWriter(file,true));
								SlowRequest req = ServiceStatContainer.slowRequests.poll();
								while (req != null) {
									output.write(JsonUtils.toJson(req) + System.lineSeparator());
									req = ServiceStatContainer.slowRequests.poll();
								}
								output.close();
							} catch (IOException e) {
								logger.error("写慢请求到文件" + logDir + "/slow.log" + "失败",e);
							}
						} else {
							// Mongodb 3 driver
//							List<Document> docs = new ArrayList<Document>();
//							SlowRequest req = ServiceStatContainer.slowRequests.poll();
//							while (req != null) {
//								docs.add(new Document(Obj2MapUtil.convert2Map(req)));
//								req = ServiceStatContainer.slowRequests.poll();
//							}
//							if (mongoClient == null) {
//								mongoClient = new MongoClient(new MongoClientURI("mongodb://" + GlobalConfig.mongoURI));
//								logger.info("已建立到mongodb://" + GlobalConfig.mongoURI + "的连接.");
//							}
//							if (database == null) {
//								database = mongoClient.getDatabase(GlobalConfig.mongoDb);
//							}
//							if (collection == null) {
//								collection = database.getCollection(GlobalConfig.slowCol);
//							}
//							collection.insertMany(docs);
							// Mongodb 2 driver
							if (mongoClient == null) {
								try {
									MongoClientOptions.Builder builder = MongoClientOptions.builder().maxWaitTime(100);
									mongoClient = new MongoClient(new MongoClientURI("mongodb://" + GlobalConfig.mongoURI,builder));
								} catch (UnknownHostException e) {
									e.printStackTrace();
								}
								logger.info("已建立到mongodb://" + GlobalConfig.mongoURI + "的连接.");
							}
							if (database == null) {
								database = mongoClient.getDB(GlobalConfig.mongoDb);
							}
							if (collection == null) {
								collection = database.getCollection(GlobalConfig.slowCol);
							}
							SlowRequest req = ServiceStatContainer.slowRequests.poll();
							BulkWriteOperation builder = collection.initializeOrderedBulkOperation();
							
							while (req != null) {
								builder.insert(new BasicDBObject(Obj2MapUtil.convert2Map(req)));
								req = ServiceStatContainer.slowRequests.poll();
							}
							builder.execute();
						}
					}
				}
			}
		} catch (InterruptedException e) {
			logger.info("1微秒内未拿到写慢日志请求锁, 对业务没有任何副作用,仅提示。");
			// e.printStackTrace();
		} finally {
			if (lock.isHeldByCurrentThread()) {
				lock.unlock();
			}
		}
	}
}
