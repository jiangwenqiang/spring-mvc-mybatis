/**
 * Copyright (C), 2007-2014, eFuture 北京富基融通科技有限公司
 * FileName:	StorageUtils.java
 * Author:		亮
 * Date:		2014-4-11 下午8:37:50
 * Description:	
 * History:
 * <author>		<time>			<version>		<description>
 * 
 */
package org.elnino.demo.utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Field;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;

/**
 * @author		亮
 * @description	
 * 
 */
public class StorageUtils
{
    public static Update createUpdateFormJSON(JSONObject json) throws IllegalArgumentException, IllegalAccessException
    {
        Update upt = new Update();
        Set<String> keys = json.keySet();
        for (String key : keys) 
        { 
            upt.set(key,json.get(key));  
        }
        return upt;
    }
    
    public static Update createUpdateFormBean(Object bean,Set<String> keys) throws IllegalArgumentException, IllegalAccessException
    {
        Update upt = new Update();
        for (String key : keys) 
        { 
            java.lang.reflect.Field fld = CommonUtils.fetchDeclaredField(bean.getClass(),key);
            
            // Transient表示存储时忽略的列
            if (fld != null && fld.getAnnotation(Transient.class) == null)
            {
                upt.set(key,fld.get(bean));  
            }
        }
        return upt;
    }
    
    public static Query createQueryFormJson(JSONObject json,Class<?> cl)
    {
        return createQueryFormJson(json,cl,true);
    }
    
    public static Query createQueryFormJson(JSONObject json,Class<?> cl,boolean mustpage)
    {
        // 按Bean Class 的数据类型进行格式化
        if (cl != null) convertBeanJSON(json,cl);
        
        List<Criteria> criteria = new ArrayList<Criteria>();        
        Set<String> keys = json.keySet();
        for (String key : keys)
        {
            if (isSpecialParamKey(key)) continue;

            Object o = json.get(key);
            if ("$and".equalsIgnoreCase(key) || "$or".equalsIgnoreCase(key))
            {
                List<Criteria> clst = new ArrayList<Criteria>();
                if (o instanceof JSONObject)
                {
                    JSONObject jo = (JSONObject)o;
                    Set<String> ks = jo.keySet();
                    for (String k : ks)
                    {
                        Criteria c = buildCriteria(k,jo.get(k),cl);
                        if (c != null) clst.add(c);
                    }
                }
                else if (o instanceof JSONArray)
                {
                    JSONArray ja = (JSONArray)o;
                    for (int i=0;i<ja.size();i++)
                    {
                        Object oo = ja.get(i);
                        if (oo instanceof JSONObject)
                        {
                            JSONObject jo = (JSONObject)oo;
                            Set<String> ks = jo.keySet();
                            for (String k : ks)
                            {
                                Criteria c = buildCriteria(k,jo.get(k),cl);
                                if (c != null) clst.add(c);
                            }
                        }
                    }
                }
                if (clst.size() > 0) 
                {
                    Criteria[] c = new Criteria[clst.size()];
                    if ("$and".equalsIgnoreCase(key)) criteria.add(new Criteria().andOperator(clst.toArray(c)));
                    else criteria.add(new Criteria().orOperator(clst.toArray(c)));
                }
            }
            else
            {
                Criteria c = buildCriteria(key,o,cl);
                if (c != null) criteria.add(c);
            }
        }
        Query quy = new Query();
        if (criteria.size() > 0) 
        {
            Criteria c = criteria.get(0);
            if (criteria.size() > 1) 
            {
                Criteria[] cc = new Criteria[criteria.size()-1];
                for (int i=1;i<criteria.size();i++) cc[i-1] = criteria.get(i);
                c.andOperator(cc);
            }
            quy.addCriteria(c);
        }
        
        return buildQueryFormJson(quy,json,mustpage,CommonUtils.fetchSelectField(cl, new HashMap<String,String>()));
    }
    
    public static Criteria buildCriteria(String key,Object o,Class<?> cl)
    {
        if (StringUtils.isEmpty(o))
        {
            // 字段不存在或为空都是空数据的匹配条件
            Criteria c = new Criteria().orOperator(Criteria.where(key).exists(false),Criteria.where(key).is(""));
            return c;
        }
        else
        if (o instanceof String && !StringUtils.isEmpty(o))
        {
            // 字符串参数name采用LIKE模式,其他采用精确匹配
            if (isExactMatchKey(key))
            {
                if (((String)o).indexOf(",") > 0)
                {
                    String[] ss = ((String)o).split(",");
                    return Criteria.where(key).in((Object[])ss);
                }
                else
                {
                    return Criteria.where(key).is(o);
                }
            }
            else
            {
                return Criteria.where(key).regex(String.valueOf(o));
            }
        }
        else
        if (o instanceof JSONArray)
        {
            JSONArray ja = ((JSONArray)o);
            Object[] oos = new Object[ja.size()];
            for (int i=0;i<ja.size();i++)
            {
                Object oo = ja.get(i);
                if (cl != null) oo = convertBeanField(oo,cl,key);
                oos[i] = oo; 
            }
            return Criteria.where(key).in(oos);
        }
        else
        if (o instanceof JSONObject)
        {
            List<Criteria> criteria = new ArrayList<Criteria>();
            Set<String> sets = ((JSONObject)o).keySet();
            for (String s : sets)
            {
                Object oo = ((JSONObject)o).get(s);
                if ("$in".equalsIgnoreCase(s)  || "in".equalsIgnoreCase(s) ||
                    "$nin".equalsIgnoreCase(s) || "notin".equalsIgnoreCase(s))
                {
                    Object[] oos = null;
                    if (oo instanceof JSONArray)
                    {
                        JSONArray ja = ((JSONArray)oo);
                        oos = new Object[ja.size()];
                        for (int i=0;i<ja.size();i++)
                        {
                            Object obj = ja.get(i);
                            if (cl != null) obj = convertBeanField(obj,cl,key);
                            oos[i] = obj; 
                        }
                    }                        
                    else 
                    {
                        String ss = oo.toString();
                        if (ss.startsWith("(") && ss.endsWith(")")) oos = new String[]{ss};
                        else oos = ss.split(",");
                    }
                    if ("$in".equalsIgnoreCase(s) || "in".equalsIgnoreCase(s)) criteria.add(Criteria.where(key).in(oos));
                    else criteria.add(Criteria.where(key).nin(oos));
                }                    
                else
                {
                    if (cl != null) oo = convertBeanField(oo,cl,key);
                    if ("$is".equalsIgnoreCase(s) || "==".equalsIgnoreCase(s) || "=".equalsIgnoreCase(s))
                    {
                        criteria.add(Criteria.where(key).is(oo));
                    }
                    else
                    if ("$ne".equalsIgnoreCase(s) || "<>".equalsIgnoreCase(s) || "!=".equalsIgnoreCase(s)) 
                    {
                        criteria.add(Criteria.where(key).ne(oo));
                    }
                    else
                    if ("like".equalsIgnoreCase(s))
                    {
                        criteria.add(Criteria.where(key).regex(String.valueOf(oo)));
                    }
                    else
                    if ("$gt".equalsIgnoreCase(s) || ">".equalsIgnoreCase(s))
                    {
                        criteria.add(Criteria.where(key).gt(oo));
                    }
                    else
                    if ("$gte".equalsIgnoreCase(s) || ">=".equalsIgnoreCase(s))
                    {
                        criteria.add(Criteria.where(key).gte(oo));
                    }
                    else
                    if ("$lt".equalsIgnoreCase(s) || "<".equalsIgnoreCase(s))
                    {
                        criteria.add(Criteria.where(key).lt(oo));
                    }
                    else
                    if ("$lte".equalsIgnoreCase(s) || "<=".equalsIgnoreCase(s)) 
                    {
                        criteria.add(Criteria.where(key).lte(oo));
                    }
                }
            }
            if (criteria.size() > 0)
            {
                Criteria[] c = new Criteria[criteria.size()];
                return new Criteria().andOperator(criteria.toArray(c));
            }
        }
        else
        {
            return Criteria.where(key).is(o);
        }
        
        return null;
    }
    
    public static Query buildQueryFormJson(Query quy,JSONObject json,boolean mustpage,Map<String,String> validflds)
    {
        // 指定返回字段
        if (!StringUtils.isEmpty(json.get(BeanConstant.QueryField.PARAMKEY_FIELDS)))
        {
            Field flds = quy.fields();
            String[] ss = json.getString(BeanConstant.QueryField.PARAMKEY_FIELDS).split(",");
            for (int i=0;i<ss.length;i++)
            {
                if (ss[i] == null) continue;
                if (StringUtils.isEmpty(ss[i].trim())) continue;
                if (validflds != null && validflds.size() > 0)
                {
                    if ("*".equals(ss[i].trim())) for (String key : validflds.keySet()) flds.include(validflds.get(key));
                    else if (!validflds.containsKey(ss[i].trim())) continue;
                    else flds.include(validflds.get(ss[i].trim()));
                }
                else 
                {
                    flds.include(ss[i].trim());
                }
            }
        }
        else
        {
            if (validflds != null && validflds.size() > 0)
            {
                Field flds = quy.fields();
                for (String key : validflds.keySet()) flds.include(validflds.get(key));
            }
        }
        
        // 排序分页设置
        int pageno = -1,pagesize = 40;
        Sort order = null; 
        if (mustpage) pageno = 1;
        if (json.containsKey(BeanConstant.QueryField.PARAMKEY_PAGENO) && !StringUtils.isEmpty(json.get(BeanConstant.QueryField.PARAMKEY_PAGENO))) 
        {
            pageno = json.getInteger(BeanConstant.QueryField.PARAMKEY_PAGENO);
            if (pageno <= 0) pageno = 1;
        }
        if (json.containsKey(BeanConstant.QueryField.PARAMKEY_PAGESIZE) && !StringUtils.isEmpty(json.get(BeanConstant.QueryField.PARAMKEY_PAGESIZE))) 
        {
            pagesize = json.getInteger(BeanConstant.QueryField.PARAMKEY_PAGESIZE);
            if (pagesize <= 0) pagesize = 40;
            if (pagesize > 9999) pagesize = 9999;
        }
        if (json.containsKey(BeanConstant.QueryField.PARAMKEY_ORDERFLD) && !StringUtils.isEmpty(json.get(BeanConstant.QueryField.PARAMKEY_ORDERFLD)))
        {
            if (json.containsKey(BeanConstant.QueryField.PARAMKEY_ORDERDIR) && !StringUtils.isEmpty(json.get(BeanConstant.QueryField.PARAMKEY_ORDERDIR)))
            {
                String[] dirs = json.getString(BeanConstant.QueryField.PARAMKEY_ORDERDIR).split(",");  
                String[] flds = json.getString(BeanConstant.QueryField.PARAMKEY_ORDERFLD).split(",");
                List<Sort.Order> lst = new ArrayList<Sort.Order>();
                for (int i=0;i<flds.length;i++) 
                {
                    if (i < dirs.length) lst.add(new Sort.Order(Direction.fromString(dirs[i]),flds[i].trim()));
                    else lst.add(new Sort.Order(flds[i].trim()));
                }
                order = new Sort(lst);
            }
            else
            {
                order = new Sort(json.getString(BeanConstant.QueryField.PARAMKEY_ORDERFLD).split(","));
            }
        }
        if (pageno >= 1) quy.with(new PageRequest(pageno-1,pagesize,order));
        else if (order != null) quy.with(order);
        
        return quy;
    }
    
    private static boolean isExactMatchKey(String key)
    {
        if (key.endsWith("name")) return false;
        
        return true;
    }
    
    private static boolean isSpecialParamKey(String key)
    {
        // 查询特定关键字
        if (key.equalsIgnoreCase(BeanConstant.QueryField.PARAMKEY_FIELDS)) return true;
        if (key.equalsIgnoreCase(BeanConstant.QueryField.PARAMKEY_PAGENO)) return true;
        if (key.equalsIgnoreCase(BeanConstant.QueryField.PARAMKEY_PAGESIZE)) return true;
        if (key.equalsIgnoreCase(BeanConstant.QueryField.PARAMKEY_ORDERFLD)) return true;
        if (key.equalsIgnoreCase(BeanConstant.QueryField.PARAMKEY_ORDERDIR)) return true;
        
        // 子表KEY,不做查询条件
        if (key.indexOf(":") > 0) return true;
        
        return false;
    }
    
    public static String parseChildParamKeyFields(String param,String key)
    {
        // 格式:KEY:FIELD|FIELD|...
        int startp = param.indexOf(key+":");
        if (startp < 0) return null;
        startp += (key+":").length();
        
        String s = null;
        int endp = param.indexOf(",", startp);
        if (endp > 0) s = param.substring(startp,endp);
        else s = param.substring(startp);
        
        return s.replace("|", ",");
    }
    
    public static Map<String,Object> parseChildParamKey(JSONObject json,String key)
    {
        // 格式:KEY:FIELD
        Map<String,Object> map = null;
        Set<String> sets = json.keySet();
        for (String param : sets)
        {
            if (!param.startsWith(key+":")) continue;
            if (map == null) map = new HashMap<String,Object>();
            
            int startp = param.indexOf(key+":");
            if (startp < 0) return null;
            startp += (key+":").length();
            
            String s = null;
            int endp = param.indexOf(",", startp);
            if (endp > 0) s = param.substring(startp,endp);
            else s = param.substring(startp);
            
            map.put(s, json.get(param));
        }
        
        return map;
    }
    
    public static JSONArray convertBeanJSON(JSONArray jsonarray,Class<?> cl)
    {
        for (int i=0;i<jsonarray.size();i++)
        {
            Object value = jsonarray.get(i);
            if (value instanceof JSONObject) jsonarray.set(i, convertBeanJSON((JSONObject)value,cl)); 
        }
        return jsonarray;
    }
    
    public static <T> T parseBeanObject(String text, Class<T> clazz)
    {
        JSONObject json = JSON.parseObject(text);
        return parseBeanObject(json, clazz);
    }
    
    public static <T> T parseBeanObject(JSONObject json, Class<T> clazz)
    {
        json = convertBeanJSON(json,clazz);
        return JSON.toJavaObject(json, clazz);
    }
    
    public static <T> T toJavaObject(Object obj,Class<T> entityClass)
    {
        return TypeUtils.cast(obj, entityClass, ParserConfig.getGlobalInstance());
    }
        
    private static JSONObject convertBeanJSON(JSONObject jsonparam,Class<?> cl)
    {
        try
        {
            Set<String> keys = jsonparam.keySet();
            for (String key : keys)
            {
                if (isSpecialParamKey(key)) continue;
                
                Object o = jsonparam.get(key);
                Object oo= convertBeanField(o,cl,key);
                if (!o.equals(oo)) jsonparam.put(key,oo);
            }
        }
        catch(Exception ex)
        {
            //ex.printStackTrace();
        }
        return jsonparam;
    }
    
    private static Object convertBeanField(Object obj,Class<?> cl,String key)
    {
        java.lang.reflect.Field fld = CommonUtils.fetchDeclaredField(cl,key);
        if (fld == null) return obj;
        if (!isSimpleType(obj)) return obj;
        
        // 按Bean Class 进行转换
        Object o = obj;
        String type1 = o.getClass().getName().toLowerCase();
        if (type1.lastIndexOf(".") > 0) type1 = type1.substring(type1.lastIndexOf(".")+1);
        String type2 = fld.getType().getName().toLowerCase();
        if (type2.lastIndexOf(".") > 0) type2 = type2.substring(type2.lastIndexOf(".")+1);
        if (!type1.startsWith(type2))
        {
            try
            {
                if (type2.contains("long")) o = StringUtils.isEmpty(o)?0:Long.parseLong(o.toString());
                else
                if (type2.contains("int")) o = StringUtils.isEmpty(o)?0:Integer.parseInt(o.toString());    
                else
                if (type2.contains("float")) o = StringUtils.isEmpty(o)?0:Float.parseFloat(o.toString());    
                else
                if (type2.contains("double")) o = StringUtils.isEmpty(o)?0:Double.parseDouble(o.toString());    
                else
                if (type2.contains("short")) o = StringUtils.isEmpty(o)?0:Short.parseShort(o.toString());
                else
                if (type2.contains("boolean"))
                {
                    if (o instanceof Boolean);
                    else 
                    {
                        if (StringUtils.isEmpty(o)) o = false;
                        else o = ("true".equalsIgnoreCase(o.toString()) || "Y".equalsIgnoreCase(o.toString()));
                    }
                }
                else 
                if (type2.contains("date")) 
                {
                    if (StringUtils.isEmpty(o)) o = null;
                    else
                    {
                        // 2014-04-04T16:00:00.000Z 格式做标准转换
                        String s = o.toString();
                        if (s.indexOf("-") > 0 || s.indexOf("/") > 0)
                        {
                            if (s.endsWith("Z")) s = s.replace("T", " ").replace("Z","");
                            s = s.replace("/", "-");
                            
                            // 转为日期型
                            SimpleDateFormat format = null;                        
                            if (s.length() <= 10) format = new SimpleDateFormat("yyyy-MM-dd");
                            else if (s.length() <= 19) format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            else format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            o = format.parse(s);
                        }
                        else
                        {
                            o = new Date(Long.parseLong(s));
                        }
                    }
                }
                else
                if (type2.contains("string")) o = String.valueOf(o);
            }
            catch(Exception ex)
            {
                //ex.printStackTrace();
            }
        }
        
        return o;
    }
    
    private static boolean isSimpleType(Object o)
    {
        if (o instanceof JSONObject) return false;
        if (o instanceof JSONArray) return false;
        
        return true;
    }
}
