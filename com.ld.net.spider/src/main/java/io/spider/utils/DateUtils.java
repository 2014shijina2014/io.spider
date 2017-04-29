/**
 * Licensed under the Apache License, Version 2.0
 */
package io.spider.utils;

import io.spider.meta.SpiderOtherMetaConstant;

import java.text.SimpleDateFormat;

public class DateUtils {

	public static final SimpleDateFormat SDF_DATE = new SimpleDateFormat(SpiderOtherMetaConstant.DATE_FORMAT);
	
	public static final SimpleDateFormat SDF_DATETIME = new SimpleDateFormat(SpiderOtherMetaConstant.DATETIME_FORMAT);
	
	public static final SimpleDateFormat SDF_DATE_NUM = new SimpleDateFormat(SpiderOtherMetaConstant.DATE_FORMAT_NUM);
	
	public static final SimpleDateFormat SDF_DATETIME_NUM = new SimpleDateFormat(SpiderOtherMetaConstant.DATETIME_FORMAT_NUM);
	
	public static final SimpleDateFormat SDF_DATETIMEMS = new SimpleDateFormat(SpiderOtherMetaConstant.DATETIMEMS_FORMAT);
	
	public static final SimpleDateFormat SDF_DATETIMEMS_NUM = new SimpleDateFormat(SpiderOtherMetaConstant.DATETIMEMS_FORMAT_NUM);
	
}
