/**
 * Copyright (C), 2007-2014, eFuture 北京富基融通科技有限公司
 * FileName:	EntityConstant.java
 * Author:		亮
 * Date:		2014-4-3 上午9:25:34
 * Description:	
 * History:
 * <author>		<time>			<version>		<description>
 * 
 */
package org.elnino.demo.utils;

/**
 * @author		亮
 * @description	
 * 
 */
public interface BeanConstant
{
    interface Status
    {
        final String INVALID = "0";
        final String NORMAL = "1";
        final String PUBLISH = "2";
    }
    
    interface QueryField
    {
        final String PARAMKEY_FIELDS = "fields";
        final String PARAMKEY_ORDERFLD = "order_field";
        final String PARAMKEY_ORDERDIR = "order_direction";
        final String PARAMKEY_PAGENO = "page_no";
        final String PARAMKEY_PAGESIZE = "page_size";
    }
}
