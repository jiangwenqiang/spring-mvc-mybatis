/**
 * Copyright (C), 2007-2014, eFuture 北京富基融通科技有限公司
 * FileName:	BaseComponent.java
 * Author:		亮
 * Date:		2014-3-27 上午10:17:09
 * Description:	
 * History:
 * <author>		<time>			<version>		<description>
 * 
 */
package org.elnino.demo.dao;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.elnino.demo.utils.BeanConstant;
import org.elnino.demo.utils.CommonUtils;
import org.elnino.demo.utils.StorageUtils;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSONObject;

/**
 * @author		亮
 * @description	服务组件基类
 * 
 */
public class BasicComponent
{	
    public final static String StorageOperation = "StorageOperation";
    
	/**
	 * 获取数据库操作操作对象
	 * @return
	 */
    public FStorageOperations getStorageOperations()
    {
        return SpringBeanFactory.getBean(StorageOperation,FStorageOperations.class);
    }
    
    public <T> T getStorageOperations(Class<T> clazz)
    {
        return SpringBeanFactory.getBean(StorageOperation,clazz);
    }
    
    /**
     * 通用单表查询，返回单行结果
     * @param jsonparam   查询参数
     * @param objClass    返回类
     * @return
     * @throws Exception
     */
    public <T> T selectOne(JSONObject jsonparam, Class<T> objClass) throws Exception
    {
        // 限制只查1行数据
        jsonparam.put(BeanConstant.QueryField.PARAMKEY_PAGESIZE, 1);
        
        @SuppressWarnings("unchecked")
		List<T> list = (List<T>) select(jsonparam,objClass,null);
        
        if (list != null && list.size() > 0) return list.get(0);
        else return null;
    }
    // 通用单表查询
    protected List<?> select(JSONObject jsonparam, Class<?> objClass,StringBuffer total) throws Exception
    {
        FStorageOperations storage = null;
        try
        {         
            // 得到数据源
            storage = getStorageOperations();           
            
            // 不返回总行数也没有设定分页大小则不分页
            Query query = null;
            if (total == null && !jsonparam.containsKey(BeanConstant.QueryField.PARAMKEY_PAGESIZE)) query = StorageUtils.createQueryFormJson(jsonparam,objClass,false);
            else query = StorageUtils.createQueryFormJson(jsonparam,objClass);

            // 指定返回字段则返回
            List<?> list = null;
            if (jsonparam.containsKey(BeanConstant.QueryField.PARAMKEY_FIELDS) && !StringUtils.isEmpty(jsonparam.get(BeanConstant.QueryField.PARAMKEY_FIELDS)))
            {
                //printDebug(String.format("Bean:[%1$s] Operate:[doGet(%2$s)Map] MongoSyntax:[%3$s]", this.getClass().getCanonicalName(),CommonUtils.fetchAnnotationTableName(objClass),query));
                list = storage.select(query, CommonUtils.fetchAnnotationTableName(objClass));
            }
            else
            {
                //printDebug(String.format("Bean:[%1$s] Operate:[doGet(%2$s)Bean] MongoSyntax:[%3$s]", this.getClass().getCanonicalName(),CommonUtils.fetchAnnotationTableName(objClass),query));
                list = storage.select(query, objClass);
            }
            
            // 设置总数
            if (total != null)
            {
                total.delete(0, total.length());
                if (list == null || list.size() <= 0) total.append("0");
                else if (query.getLimit() > 0) 
                {
                    total.append(storage.count(query, objClass));
                }
                else 
                {
                    total.append(list.size());
                }
            }
            return list;
        }
        finally
        {
            if (storage != null) storage.destroy();
        }
    }
    
    // 分解导出字段
    protected void analyzeExportFields(String specfld,StringBuffer flds,List<String> fldlst,Map<String,String> disps,Map<String,String> fmts)
    {
        // fields="fld1:XXXX:fmt,fld2:XXXXX:fmt,fld3:XXXXX:fmt";
        String[] fields = specfld.split(",");
        for (String s : fields)
        {
            String[] ss = s.split(":");
            flds.append(ss[0].trim()+",");
            fldlst.add(ss[0].trim());
            if (ss.length > 1)
            {
                disps.put(ss[0].trim(), ss[1].trim());
                if (ss.length > 2) fmts.put(ss[0].trim(), ss[2].trim().replace(";", ","));
            }
            else 
            {
                disps.put(ss[0].trim(),ss[0].trim());
            }
        }
    }
    
    // 生成XLS导出
    protected String createExportXLS(List<Map<String,Object>> list,List<String> fldlst,Map<String,String> disps,Map<String,String> fmts)
    {
        // 生成XLS文件的XML
        StringBuffer sb = new StringBuffer();
        sb.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\r\n");
        sb.append("<table cellspacing=\"0\" cellpadding=\"5\" rules=\"all\" border=\"1\">\r\n");
        
        // 生成列头
        sb.append("<tr style=\"font-weight: bold; color:red; white-space: nowrap;\">\r\n");
        for (String s : fldlst) sb.append(String.format("<td>%1$s</td>\r\n",disps.get(s)));
        sb.append("</tr>\r\n");
        
        // 填写数据
        if (list != null)
        {
            for (Map<String,Object> mp : list) 
            {
                sb.append("<tr>\r\n");
                for (String s : fldlst)
                {
                    Object obj = mp.get(s);
                    if (obj == null) sb.append(String.format("<td style=\"vnd.ms-excel.numberformat:@\">%1$s</td>\r\n",""));
                    else
                    {
                        String style = fmts.get(s);
                        if (obj instanceof Date)
                        {
                            if (StringUtils.isEmpty(style)) style = "yyyy-mm-dd";
                            long date = ((Date)obj).getTime()/86400000 + 25569; // 毫秒转为天,JAVA的日期型从1970-1-1开始，excle从1900-1-1开始
                            sb.append(String.format("<td style=\"vnd.ms-excel.numberformat:"+style+"\">%1$s</td>\r\n",date));
                        }
                        else
                        {
                            if (StringUtils.isEmpty(style))
                            {
                                if (obj instanceof Integer || obj instanceof Long || obj instanceof Short) style = "0";
                                else if (obj instanceof Double || obj instanceof Float) style = "#,##0.00";
                                else style = "@";
                            }
                            sb.append(String.format("<td style=\"vnd.ms-excel.numberformat:"+style+"\">%1$s</td>\r\n",obj));
                        }
                    }
                }
                sb.append("</tr>\r\n");
            }
        }
        sb.append("</table>\r\n");
        
        // 返回
        return sb.toString();
    }
}
