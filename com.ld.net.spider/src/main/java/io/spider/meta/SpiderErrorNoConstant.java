/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.meta;

import io.spider.pojo.GlobalConfig;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * spider 通信中间件
 * @author zhjh256@163.com
 * {@link} http://www.cnblogs.com/zhjh256
 */
public final class SpiderErrorNoConstant {
	
	static final Logger logger = LoggerFactory.getLogger("spider." + GlobalConfig.clusterName);
	
	public static final String ERROR_SERVER_LABEL_EN = "Error first throwed by server: ";
	public static final String ERROR_SERVER_LABEL = "异常首次发生服务器: ";
	
	private static final Map<String,String> errorMap = new ConcurrentHashMap<String,String>();
	
	public static final String ERROR_NO_SUCCESS = "000";
	public static final String ERROR_INFO_SUCCESS_EN = "success";
	public static final String ERROR_INFO_SUCCESS = "执行成功";
	public static final String ERROR_NO_REQUEST_RECEIVED = "010";
	public static final String ERROR_INFO_REQUEST_RECEIVED_EN = "request is received, will execute asap. Error throwed by server: {0}.";
	public static final String ERROR_INFO_REQUEST_RECEIVED = "请求已收到";
	
	public static final String ERROR_NO_CANNOT_FOUND_SERVICE_IMPLEMENT = "001";
	public static final String ERROR_INFO_CANNOT_FOUND_SERVICE_IMPLEMENT_EN = "cannot find spider service implement. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_CANNOT_FOUND_SERVICE_IMPLEMENT = "没有找到服务的实现。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_DISCONNECT_FROM_SERVER = "002";
	public static final String ERROR_INFO_DISCONNECT_FROM_SERVER_EN = "disconnected from spider server {2} or still can't connect. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_DISCONNECT_FROM_SERVER = "与目标服务器[{0}]断开连接。" + ERROR_SERVER_LABEL + "{1}。原因: {2}。";
	public static final String ERROR_NO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION = "003";
	public static final String ERROR_INFO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION_EN = "spider service implement throwed uncatched exception.Error throwed by server: {0}. Caused by: {1}. ";
	public static final String ERROR_INFO_BIZ_SERVICE_THROW_UNCATCHED_EXCEPTION = "服务执行抛出未捕获异常。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_CRC32_CHECK_FAIL = "004";
	public static final String ERROR_INFO_CRC32_CHECK_FAIL_EN = "crc32 verify failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_CRC32_CHECK_FAIL = "CRC32校验失败。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_DECRYPT_FAIL = "005";
	public static final String ERROR_INFO_DECRYPT_FAIL_EN = "decrypt failed, key is incorrect or data was tampered during transmit. Error throwed by server: {0}.";
	public static final String ERROR_INFO_DECRYPT_FAIL = "解密失败。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_UNAUTH_ACCESS = "006";
	public static final String ERROR_INFO_UNAUTH_ACCESS_EN = "access deny, you must pass auth to access this server. Error throwed by server: {0}.";
	public static final String ERROR_INFO_UNAUTH_ACCESS = "未授权访问本服务器。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_SPIDER_IS_INITIALIZING = "007";
	public static final String ERROR_INFO_SPIDER_IS_INITIALIZING_EN = "spider runtime is initializing, plz retry later. Error throwed by server: {0}.";
	public static final String ERROR_INFO_SPIDER_IS_INITIALIZING = "spider正在初始化,请稍后重试。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_CANNOT_FORWARD = "008";
	public static final String ERROR_INFO_CANNOT_FORWARD_EN = "no route matched this service, plz check the config file. Error throwed by server: {0}.";
	public static final String ERROR_INFO_CANNOT_FORWARD = "没有找到与本服务匹配的路由,请检查配置文件! " + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_FORWARD_FAILED = "009";
	public static final String ERROR_INFO_FORWARD_FAILED_EN = "forward spider service failed, plz check target error log. Error throwed by server: {0}.caused by: {1}. ";
	public static final String ERROR_INFO_FORWARD_FAILED = "服务无法转发, 请检查错误日志! " + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_TIMEOUT = "011";
	public static final String ERROR_INFO_TIMEOUT_EN = "call spider service timeout. Error throwed by server: {0}.";
	public static final String ERROR_INFO_TIMEOUT = "调用远程服务超时。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_PACKET_LESS_THAN_MIN_SIZE = "012";
	public static final String ERROR_INFO_PACKET_LESS_THAN_MIN_SIZE_EN = "packet size is less than min size";
	public static final String ERROR_INFO_PACKET_LESS_THAN_MIN_SIZE = "无效的报文长度";
	public static final String ERROR_NO_REQUEST_SAVE_FAILED = "013";
	public static final String ERROR_INFO_REQUEST_SAVE_FAILED_EN = "request save failed, plz check the log and retry. Error throwed by server: {0}.";
	public static final String ERROR_INFO_REQUEST_SAVE_FAILED = "请求保存失败, 请检查服务器日志并重试。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_NOT_DEFINED_HEAD_TAG = "014";
	public static final String ERROR_INFO_NOT_DEFINED_HEAD_TAG_EN = "the head tag is not defined in spider runtime";
	public static final String ERROR_INFO_NOT_DEFINED_HEAD_TAG = "无效的可变报文头标签";
	public static final String ERROR_NO_ONLY_SUPPORT_ONE_OBJECT_PARAM = "015";
	public static final String ERROR_INFO_ONLY_SUPPORT_ONE_OBJECT_PARAM_EN = "currently only support one object param spider service";
	public static final String ERROR_INFO_ONLY_SUPPORT_ONE_OBJECT_PARAM = "默认情况下不支持多个参数平铺";
	public static final String ERROR_NO_SERVICE_NOT_DEFINED = "016";
	public static final String ERROR_INFO_SERVICE_NOT_DEFINED_EN = "service not defined, main reason is serviceProxyPackage/serviceExportPackage is incorrect. Error throwed by server: {0}.";
	public static final String ERROR_INFO_SERVICE_NOT_DEFINED = "没有找到服务的定义,通常是serviceProxyPackage/serviceExportPackage参数设置不正确。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_LICENSE_INCORRECT = "017";
	public static final String ERROR_INFO_LICENSE_INCORRECT_EN = "license key is invalid, plz contact the customer service. Error throwed by server: {0}.";
	public static final String ERROR_INFO_LICENSE_INCORRECT = "授权码无效, 请联系客服。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_REQUEST_ABORTED = "018";
	public static final String ERROR_INFO_REQUEST_ABORTED_EN = "request is aborted due to the interceptor rule.Error throwed by server: {0}. Caused by: {1}. ";
	public static final String ERROR_INFO_REQUEST_ABORTED = "请求被拦截。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_BIZ_EXCEPTION = "888";
	public static final String ERROR_INFO_BIZ_EXCEPTION_EN = "some biz exception catched. Error throwed by server: {0}.";
	public static final String ERROR_INFO_BIZ_EXCEPTION = "发生业务异常。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_UNEXPECT_EXCEPTION = "999";
	public static final String ERROR_INFO_UNEXPECT_EXCEPTION_EN = "unexpect exception occured, plz contact the spider developer. Error throwed by server: {0}.";
	public static final String ERROR_INFO_UNEXPECT_EXCEPTION = "发生非预期的未捕获异常。" + ERROR_SERVER_LABEL + "{0}。";
	
	public static final String ERROR_NO_CLUSTER_NOT_EXIST = "100";
	public static final String ERROR_INFO_CLUSTER_NOT_EXIST_EN = "cluster name not exist. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_CLUSTER_NOT_EXIST = "路由目标服务器名不存在。" + ERROR_SERVER_LABEL + "{0}。";
	public static final String ERROR_NO_WORKNODE_NOT_EXIST = "101";
	public static final String ERROR_INFO_WORKNODE_NOT_EXIST_EN = "worknode not exist. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_WORKNODE_NOT_EXIST = "目标节点不存在。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_MUST_HAVE_ONE_MORE_WORKNODE = "102";
	public static final String ERROR_INFO_MUST_HAVE_ONE_MORE_WORKNODE_EN = "cluster to be add must have at least one worknode. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_MUST_HAVE_ONE_MORE_WORKNODE = "集群至少需要包含一个节点。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_SPIDER_XML_LOAD_FAILED = "103";
	public static final String ERROR_INFO_SPIDER_XML_LOAD_FAILED_EN = "spider.xml cannot find or load failed";
	public static final String ERROR_INFO_SPIDER_XML_LOAD_FAILED = "找不到spider.xml或加载失败";
	public static final String ERROR_NO_CLUSTER_EXIST = "104";
	public static final String ERROR_INFO_CLUSTER_EXIST_EN = "cluster name already exist. Error throwed by server: {0}.Caused by: {0}. ";
	public static final String ERROR_INFO_CLUSTER_EXIST = "集群已存在。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_ROUTE_ITEM_MUST_POINT_EXIST_CLUSTERNAME = "105";
	public static final String ERROR_INFO_ROUTE_ITEM_MUST_POINT_EXIST_CLUSTERNAME_EN = "route item must point to existing clustername,plz check spider.xml. Error throwed by server: {0}.";
	public static final String ERROR_INFO_ROUTE_ITEM_MUST_POINT_EXIST_CLUSTERNAME = "路由条目必须指向存在的集群定义, 请检查spider.xml的定义。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE = "106";
	public static final String ERROR_INFO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE_EN = "cannot change spider config in cloud mode using local restful request. Error throwed by server: {0}.";
	public static final String ERROR_INFO_CANNOT_CHANGE_SPIDER_CONFIG_IN_CLOUD_MODE = "云模式下不能通过本地RESTFUL请求更改Spider配置。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_NONRELIABLE_HASNO_PERSISTENT_REQUEST = "107";
	public static final String ERROR_INFO_NONRELIABLE_HASNO_PERSISTENT_REQUEST_EN = "non-reliable mode has no persistent request. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_NONRELIABLE_HASNO_PERSISTENT_REQUEST = "非可信模式下没有持久化请求。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";

	public static final String ERROR_NO_QUERY_FINISHED_REQUEST_FAILED = "108";
	public static final String ERROR_INFO_QUERY_FINISHED_REQUEST_FAILED_EN = "query finished spider request failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_QUERY_FINISHED_REQUEST_FAILED = "查询已完成请求失败。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";
	public static final String ERROR_NO_DELETE_FINISHED_REQUEST_FAILED = "109";
	public static final String ERROR_INFO_DELETE_FINISHED_REQUEST_FAILED_EN = "delete finished spider request failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_DELETE_FINISHED_REQUEST_FAILED = "删除已完成请求失败。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";

	public static final String ERROR_NO_REGISTER_FAILED = "110";
	public static final String ERROR_INFO_REGISTER_FAILED = "register to service center failed";
	public static final String ERROR_INFO_REGISTER_FAILED_EN = "注册到服务中心失败。";

	public static final String ERROR_NO_MANANGED_NODE_IS_DISCONNECTED = "111";
	public static final String ERROR_INFO_MANANGED_NODE_IS_DISCONNECTED_EN = "managed node is disconnect from service center. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_MANANGED_NODE_IS_DISCONNECTED = "受管节点与服务中心断开连接。" + ERROR_SERVER_LABEL + "{0}。原因: {1}。";

	public static final String ERROR_NO_CURRENT_NODE_ISNOT_PROXY_NODE = "112";
	public static final String ERROR_INFO_CURRENT_NODE_ISNOT_PROXY_NODE_EN = "current node is not proxy node";
	public static final String ERROR_INFO_CURRENT_NODE_ISNOT_PROXY_NODE = "当前节点不是代理节点";
	
	
	public static final String ERROR_NO_UNSUPPORT_OPERATION = "019";
	public static final String ERROR_INFO_UNSUPPORT_OPERATION_EN = "unsupport operation. Error throwed by server: {0}.";
	public static final String ERROR_INFO_UNSUPPORT_OPERATION = "该操作尚不受支持。" + ERROR_SERVER_LABEL + "{0}。";
	
	public static final String ERROR_NO_FILE_SAVE_FAILED = "020";
	public static final String ERROR_INFO_FILE_SAVE_FAILED_EN = "file save failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_FILE_SAVE_FAILED = "文件保存失败。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_SHELL_EXECUTE_FAILED = "021";
	public static final String ERROR_INFO_SHELL_EXECUTE_FAILED_EN = "shell execute failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_SHELL_EXECUTE_FAILED = "shell命令执行失败。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_WINDOWS_IS_UNSUPPORT = "022";
	public static final String ERROR_INFO_WINDOWS_IS_UNSUPPORT_EN = "in current version, execute windows shell is unsupported. Error throwed by server: {0}.";
	public static final String ERROR_INFO_WINDOWS_IS_UNSUPPORT = "在当前的版本中, Windows Shell不受支持。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_FILE_NOT_EXIST = "023";
	public static final String ERROR_INFO_FILE_NOT_EXIST_EN = "file not exist. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_FILE_NOT_EXIST = "文件不存在。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_FILE_RECEIVE_FAILED = "024";
	public static final String ERROR_INFO_FILE_RECEIVE_FAILED_EN = "file receive failed. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_FILE_RECEIVE_FAILED = "文件接收失败。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_UNSUPPORT_ENCODING = "025";
	public static final String ERROR_INFO_UNSUPPORT_ENCODING_EN = "unsupported encoding. Error throwed by server: {0}.Caused by: {1}. ";
	public static final String ERROR_INFO_UNSUPPORT_ENCODING = "不受支持的编码格式。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_CANNOT_CONNECT_TO_SFTP = "026";
	public static final String ERROR_INFO_CANNOT_CONNECT_TO_SFTP_EN = "cannot connect to sftp. Error throwed by server: {0}. Caused by: {1}.";
	public static final String ERROR_INFO_CANNOT_CONNECT_TO_SFTP = "无法连接到SFTP。" + ERROR_SERVER_LABEL + "{0}。";

	public static final String ERROR_NO_PARTIAL_EXECUTE_SUCCESS = "027";
	public static final String ERROR_INFO_PARTIAL_EXECUTE_SUCCESS_EN = "some record execute success, the others are failed (local node maybe also failed or not).";
	public static final String ERROR_INFO_PARTIAL_EXECUTE_SUCCESS = "部分记录执行成功。";

	public static final String ERROR_NO_REQUEST_IS_CANCELLED = "028";
	public static final String ERROR_INFO_REQUEST_IS_CANCELLED_EN = "socket request is cancelled.";
	public static final String ERROR_INFO_REQUEST_IS_CANCELLED = "请求被取消";

	public static final String ERROR_NO_INVALID_PACKET = "029";
	public static final String ERROR_INFO_INVALID_PACKET_EN = "invalid packet";
	public static final String ERROR_INFO_INVALID_PACKET = "无效的报文";

	public static final String ERROR_NO_UNSUPPORT_PLUGIN_RET_CODE = "030";
	public static final String ERROR_INFO_UNSUPPORT_PLUGIN_RET_CODE_EN = "unsupport plugin ret code";
	public static final String ERROR_INFO_UNSUPPORT_PLUGIN_RET_CODE = "未知的插件返回码";

	public static final String ERROR_NO_PARSE_JSON_STRING_FAILED = "031";
	public static final String ERROR_INFO_PARSE_JSON_STRING_FAILED_EN = "parse json string failed";
	public static final String ERROR_INFO_PARSE_JSON_STRING_FAILED = "解析JSON字符串失败";
	
	public static final String ERROR_NO_GENERATE_LICENSE_KEY_FAILED = "032";
	public static final String ERROR_INFO_GENERATE_LICENSE_KEY_FAILED_EN = "generate license key failed. Caused by: {0}.";
	public static final String ERROR_INFO_GENERATE_LICENSE_KEY_FAILED = "生成授权码失败。原因: {0}。";

	public static final String ERROR_NO_TCP_DUMP_IS_DISABLED = "033";
	public static final String ERROR_INFO_TCP_DUMP_IS_DISABLED_EN = "tcp dump is disabled";
	public static final String ERROR_INFO_TCP_DUMP_IS_DISABLED = "抓包被禁用";

	public static final String ERROR_NO_RPC_MSG_ID_IS_DUPLICATE = "034";
	public static final String ERROR_INFO_RPC_MSG_ID_IS_DUPLICATE_EN = "rpc msg id is duplicate, the main reason is routeitem in recycling.";
	public static final String ERROR_INFO_RPC_MSG_ID_IS_DUPLICATE = "消息编号重复, 很可能是路由死循环了。";

	public static final String ERROR_NO_CONNECT_TO_SSH_SERVER_FAILED = "113";
	public static final String ERROR_INFO_CONNECT_TO_SSH_SERVER_FAILED_EN = "connect to target ssh server failed";
	public static final String ERROR_INFO_CONNECT_TO_SSH_SERVER_FAILED = "连接到目标SSH服务器失败";

	public static final String ERROR_NO_ARG_COUNT_INCORRECT = "035";
	public static final String ERROR_INFO_ARG_COUNT_INCORRECT_EN = "arg count is incorrect";
	public static final String ERROR_INFO_ARG_COUNT_INCORRECT = "参数数量不正确";

	public static final String ERROR_NO_UNSUPPORT_DATA_TYPE = "036";
	public static final String ERROR_INFO_UNSUPPORT_DATA_TYPE_EN = "support plain params mode only support following datatypes: int,long,bigdecimal,string";
	public static final String ERROR_INFO_UNSUPPORT_DATA_TYPE = "当前仅支持下列数据类型:int,long,bigdecimal,string。";
	
	public static final String ERROR_NO_SFTP_UPLOAD = "037";
	public static final String ERROR_INFO_SFTP_UPLOAD_EN = "sftp upload failed";
	public static final String ERROR_INFO_SFTP_UPLOAD = "sftp上传失败";
	
	public static final String ERROR_NO_ILLEGAL_ARGUMENT = "038";
	public static final String ERROR_INFO_ILLEGAL_ARGUMENT_EN = "illegal argument";
	public static final String ERROR_INFO_ILLEGAL_ARGUMENT = "无效的参数";

	public static final String ERROR_NO_CANNOT_GET_JDBC_CONN = "039";
	public static final String ERROR_INFO_CANNOT_GET_JDBC_CONN_EN = "cannot get jdbc connection within timeout";
	public static final String ERROR_INFO_CANNOT_GET_JDBC_CONN = "数据库繁忙或者连接配置过低,无法在超时时间范围内获取数据库连接";
	
	static {
		Map<String,String> tmpErrorMap = new ConcurrentHashMap<String,String>();
		Field[] fields = SpiderErrorNoConstant.class.getFields();
        for( Field field : fields ){
            try {
            	tmpErrorMap.put(field.getName(), field.get(SpiderErrorNoConstant.class).toString());
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
        }
        for(Entry<String, String> entry : tmpErrorMap.entrySet()) {
        	try {
	        	if(entry.getKey().indexOf("_NO_")>0) {
	        		errorMap.put(entry.getValue(), tmpErrorMap.get(entry.getKey().replaceAll("\\_NO\\_", "_INFO_")));
	        	}
        	} catch (NullPointerException e) {
        		logger.error("错误号：" + entry.getKey());
        		e.printStackTrace();
        	}
         }
	}
	
	public static String getErrorInfo(String errorNo) {
		return errorMap.get(errorNo);
	}
}
