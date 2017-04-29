/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.mx;

import io.netty.channel.Channel;
import io.spider.channel.SocketClientHelper;
import io.spider.meta.SpiderConfigName;
import io.spider.meta.SpiderErrorNoConstant;
import io.spider.meta.SpiderOtherMetaConstant;
import io.spider.parser.RouteParser;
import io.spider.pojo.Cluster;
import io.spider.pojo.DynamicRouterContainer;
import io.spider.pojo.GlobalConfig;
import io.spider.pojo.RouteItem;
import io.spider.pojo.SpiderBaseResp;
import io.spider.pojo.WorkNode;
import io.spider.sc.pojo.ClusterReq;
import io.spider.sc.pojo.RouteItemReq;
import io.spider.sc.pojo.WorkNodeReq;
import io.spider.utils.DateUtils;
import io.spider.utils.JsonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ResourceUtils;

/**
 * 本地模式下使用,服务中心模式调用本实现的方法,不重复实现.本地模式无条件可用,服务中心看配置
 * @author zhjh256@163.com
 *
 */
@Service
public class SpiderManageServiceImpl {
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public SpiderBaseResp addWorkNode(WorkNodeReq req) {
		if (GlobalConfig.getCluster(req.getClusterName()) == null) {
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_CLUSTER_NOT_EXIST);
		}
		WorkNode member = new WorkNode(req.getIp(), req.getPort(),false);
		
		try {
			File spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			String path = spiderCfgFile.getAbsolutePath() + "." + DateUtils.SDF_DATETIME_NUM.format(new Date());
			SAXReader sr = new SAXReader();
			Document spiderDoc = sr.read(spiderCfgFile); 
	        List list = spiderDoc.selectNodes("spider/" + SpiderConfigName.ELE_PLUGINS + "/" + SpiderConfigName.ELE_PLUGIN);
	        Iterator iter = list.iterator();
	        boolean isSuccess = false;
	        while(iter.hasNext()){
	            Element elemPlugin = (Element)iter.next();
	            if (elemPlugin.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_CHANNEL)) {
	            	Iterator iterCluster = elemPlugin.elements().iterator();
	            	while(iterCluster.hasNext()){
	            		Element elemCluster = (Element)iterCluster.next();
	            		if (elemCluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME).equals(req.getClusterName())) {
	            			Element newWorkNode = elemCluster.addElement(SpiderConfigName.ELE_WORK_NODE);
		            		newWorkNode.addAttribute(SpiderConfigName.ATTR_ADDRESS,member.getAddress());
		            		newWorkNode.addAttribute(SpiderConfigName.ATTR_PORT,String.valueOf(member.getPort()));
		            		isSuccess = true;
		            		break;
	            		}
	            	}
	            	break;
				}
	        }
	        if(!isSuccess) {
	        	return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_CLUSTER_NOT_EXIST);
	        }
	        FileWriter newFile = new FileWriter(new File(path));
            XMLWriter newWriter = new XMLWriter(newFile);
            newWriter.write(spiderDoc);
            newWriter.close();
            
            spiderCfgFile.delete();
            File oldfile=new File(path); 
            File newfile=new File(path.substring(0, path.lastIndexOf(".")));
            oldfile.renameTo(newfile);
		} catch (DocumentException | IOException e1) {
			e1.printStackTrace();
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SPIDER_XML_LOAD_FAILED);
		}
		Channel channel = SocketClientHelper.createChannel(member.getAddress(),member.getPort(),member.isSsl());
		if(channel != null) {
			member.addChannel(Thread.currentThread().getName(),channel);
		} else {
			member.setConnected(false);
		}
		
		GlobalConfig.getCluster(req.getClusterName()).addWorkNode(member);
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
		
	}
	
	public SpiderBaseResp addCluster(ClusterReq req) {
		if(req.getCluster().getWorkNodes().size() == 0) {
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_MUST_HAVE_ONE_MORE_WORKNODE);
		}
		
		Cluster server = new Cluster(req.getCluster().getClusterName());
		
		for(WorkNode workNode : req.getCluster().getWorkNodes().values()) {
			server.addWorkNode(workNode.getAddress(), workNode.getPort(),false);
		}
		
		try {
			File spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			String path = spiderCfgFile.getAbsolutePath() + "." + DateUtils.SDF_DATETIME_NUM.format(new Date());
			SAXReader sr = new SAXReader();
			Document spiderDoc = sr.read(spiderCfgFile); 
	        List list = spiderDoc.selectNodes("spider/" + SpiderConfigName.ELE_PLUGINS + "/" + SpiderConfigName.ELE_PLUGIN);
	        Iterator iter = list.iterator();
	        while(iter.hasNext()){
	            Element elemPlugin = (Element)iter.next();
	            if (elemPlugin.attributeValue(SpiderConfigName.ATTR_PLUGIN_ID).equals(SpiderOtherMetaConstant.PLUGIN_ID_CHANNEL)) {
	            	Iterator iterCluster = elemPlugin.elements().iterator();
	            	while(iterCluster.hasNext()){
	            		Element elemCluster = (Element)iterCluster.next();
	            		if (elemCluster.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME).equals(req.getCluster().getClusterName())) {
	            			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_CLUSTER_EXIST);
	            		}
	            	}
	            	Element newCLuster = elemPlugin.addElement(SpiderConfigName.ELE_CLUSTER);
	            	newCLuster.addAttribute(SpiderConfigName.ATTR_CLUSTER_NAME, req.getCluster().getClusterName());
	            	for(WorkNode workNode : req.getCluster().getWorkNodes().values()) {
	            		Element newWorkNode = newCLuster.addElement(SpiderConfigName.ELE_WORK_NODE);
	            		newWorkNode.addAttribute(SpiderConfigName.ATTR_ADDRESS,workNode.getAddress());
	            		newWorkNode.addAttribute(SpiderConfigName.ATTR_CONNECTION_SIZE,String.valueOf(workNode.getConnectionSize()));
	            		newWorkNode.addAttribute(SpiderConfigName.ATTR_PORT,String.valueOf(workNode.getPort()));
	        		}
	            	break;
				}
	        }
	        
	        FileWriter newFile = new FileWriter(new File(path));
            XMLWriter newWriter = new XMLWriter(newFile);
            newWriter.write(spiderDoc);
            newWriter.close();
            
            spiderCfgFile.delete();
            File oldfile=new File(path); 
            File newfile=new File(path.substring(0, path.lastIndexOf(".")));
            oldfile.renameTo(newfile);
		} catch (DocumentException | IOException e1) {
			e1.printStackTrace();
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SPIDER_XML_LOAD_FAILED);
		}
		
		for(WorkNode member : server.getWorkNodes().values()) {
			Channel channel = SocketClientHelper.createChannel(member.getAddress(),member.getPort(),member.isSsl());
			if(channel != null) {
				member.addChannel(Thread.currentThread().getName(),channel);
			} else {
				member.setConnected(false);
			}
		}
		
		GlobalConfig.addCluster(server);
		
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
	}
	
	/**
	 * 
	 * @param routeItem, 正则格式
	 */
	public SpiderBaseResp addRouteItem(RouteItemReq routeItemReq) {
		RouteItem routeItem = routeItemReq.getRouteItem();
        
        if(GlobalConfig.getCluster(routeItem.getClusterName().trim()) == null) {
        	logger.error(JsonUtils.toJson(routeItemReq));
        	return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_ROUTE_ITEM_MUST_POINT_EXIST_CLUSTERNAME);
        }
		try {
			File spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			String path = spiderCfgFile.getAbsolutePath() + "." + DateUtils.SDF_DATETIME_NUM.format(new Date());
			SAXReader sr = new SAXReader();
			Document spiderDoc = sr.read(spiderCfgFile); 
	        Element routeItemsEle = spiderDoc.getRootElement().element(SpiderConfigName.ELE_ROUTE_ITEMS);
	        Element routeItemEle = routeItemsEle.addElement(SpiderConfigName.ELE_ROUTE_ITEM);
	        
	        routeItemEle.addAttribute(SpiderConfigName.ATTR_SERVICE_ID, routeItem.getServiceId().trim().equals("") ? "*" : routeItem.getServiceId().trim());
	        if(routeItem.getAppVersion().trim().equals("") || routeItem.getAppVersion().trim().equals("*")) {
	        	//NOP
	        } else {
	        	routeItemEle.addAttribute(SpiderConfigName.ATTR_APP_VERSION,routeItem.getAppVersion().trim());
	        }
	        
	        if(routeItem.getSubSystemId().trim().equals("") || routeItem.getSubSystemId().trim().equals("*")) {
	        	//NOP
	        } else {
	        	routeItemEle.addAttribute(SpiderConfigName.ATTR_SUB_SYSTEM_ID,routeItem.getSubSystemId().trim());
	        }
	        
	        if(routeItem.getSystemId().trim().equals("") || routeItem.getSystemId().trim().equals("*")) {
	        	//NOP
	        } else {
	        	routeItemEle.addAttribute(SpiderConfigName.ATTR_SYSTEM_ID,routeItem.getSystemId().trim());
	        }
	        
	        if(routeItem.getCompanyId().trim().equals("") || routeItem.getCompanyId().trim().equals("*")) {
	        	//NOP
	        } else {
	        	routeItemEle.addAttribute(SpiderConfigName.ATTR_COMPANY_ID,routeItem.getCompanyId().trim());
	        }
	        
	        routeItemEle.addAttribute(SpiderConfigName.ATTR_CLUSTER_NAME, routeItem.getClusterName().trim());
	        
	        FileWriter newFile = new FileWriter(new File(path));
            XMLWriter newWriter = new XMLWriter(newFile);
            newWriter.write(spiderDoc);
            newWriter.close();
            
            spiderCfgFile.delete();
            File oldfile=new File(path); 
            File newfile=new File(path.substring(0, path.lastIndexOf(".")));
            oldfile.renameTo(newfile);
            
            RouteParser.parse(routeItemEle);
		} catch (DocumentException | IOException e1) {
			e1.printStackTrace();
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SPIDER_XML_LOAD_FAILED);
		}
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
	}
	
	public SpiderBaseResp removeWorkNode(WorkNodeReq req) {
		if (GlobalConfig.getCluster(req.getClusterName()) == null) {
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_CLUSTER_NOT_EXIST);
		}
		
		if (null == GlobalConfig.getCluster(req.getClusterName()).removeWorkNode(req.getIp(), req.getPort())) {
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_WORKNODE_NOT_EXIST);
		}
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
		
	}
	
	public SpiderBaseResp removeCluster(String clusterName) {
		SpiderBaseResp resp = removeRouteItem(clusterName);
		if(resp.isSuccess()) {
			resp = GlobalConfig.removeCluster(clusterName);
		}
		return resp;
	}
	
	public SpiderBaseResp removeRouteItem(String clusterName) {
		
		try {
			File spiderCfgFile = ResourceUtils.getFile("classpath:spider.xml");
			String path = spiderCfgFile.getAbsolutePath() + "." + DateUtils.SDF_DATETIME_NUM.format(new Date());
			SAXReader sr = new SAXReader();
			Document spiderDoc = sr.read(spiderCfgFile); 
	        List list = spiderDoc.selectNodes("spider/" + SpiderConfigName.ELE_ROUTE_ITEMS + "/" + SpiderConfigName.ELE_ROUTE_ITEM);
	        Iterator iter = list.iterator();
	        while(iter.hasNext()){
	            Element elem = (Element)iter.next();
	            if (elem.attributeValue(SpiderConfigName.ATTR_CLUSTER_NAME).equals(clusterName)) {
					iter.remove();
				}
	        }
	        
	        FileWriter newFile = new FileWriter(new File(path));
            XMLWriter newWriter = new XMLWriter(newFile);
            newWriter.write(spiderDoc);
            newWriter.close();
            
            spiderCfgFile.delete();
            File oldfile=new File(path); 
            File newfile=new File(path.substring(0, path.lastIndexOf(".")));
            oldfile.renameTo(newfile);
            
		} catch (DocumentException | IOException e1) {
			e1.printStackTrace();
			return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SPIDER_XML_LOAD_FAILED);
		}
		
		for (RouteItem routeItem : GlobalConfig.routeItems) {
			if(routeItem.getClusterName().equals(clusterName)) {
				GlobalConfig.routeItems.remove(routeItem);
			}
		}

		DynamicRouterContainer.removeByClusterName(clusterName);
		
		return new SpiderBaseResp(SpiderErrorNoConstant.ERROR_NO_SUCCESS);
	}
}
