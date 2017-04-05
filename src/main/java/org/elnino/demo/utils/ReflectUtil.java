package org.elnino.demo.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.apache.log4j.Logger;

public class ReflectUtil
{
  private static Logger log = Logger.getLogger(ReflectUtil.class);
  
  private static Object operate(Object obj, String fieldName, Object fieldVal, String type)
  {
    Object ret = null;
    try
    {
      Class<? extends Object> classType = obj.getClass();
      

      Field[] fields = classType.getDeclaredFields();
      for (int i = 0; i < fields.length; i++)
      {
        Field field = fields[i];
        if (field.getName().equals(fieldName))
        {
          String firstLetter = fieldName.substring(0, 1).toUpperCase();
          if ("set".equals(type))
          {
            String setMethodName = "set" + firstLetter + fieldName.substring(1);
            Method setMethod = classType.getMethod(setMethodName, new Class[] { field.getType() });
            


            ret = setMethod.invoke(obj, new Object[] { fieldVal });
          }
          Method getMethod = null;
          if ("get".equals(type))
          {
            String getMethodName = "get" + firstLetter + fieldName.substring(1);
            getMethod = classType.getMethod(getMethodName, new Class[0]);
          }
          return getMethod.invoke(obj, new Object[0]);
        }
      }
    }
    catch (Exception e)
    {
      log.warn("reflect error:" + fieldName, e);
    }
    return ret;
  }
  
  public static Object getVal(Object obj, String fieldName)
  {
    return operate(obj, fieldName, null, "get");
  }
  
  public static void setVal(Object obj, String fieldName, Object fieldVal)
  {
    operate(obj, fieldName, fieldVal, "set");
  }
  
  private static Method getDeclaredMethod(Object object, String methodName, Class<?>[] parameterTypes)
  {
    for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
      try
      {
        return superClass.getDeclaredMethod(methodName, parameterTypes);
      }
      catch (NoSuchMethodException e) {}
    }
    return null;
  }
  
  private static void makeAccessible(Field field)
  {
    if (!Modifier.isPublic(field.getModifiers())) {
      field.setAccessible(true);
    }
  }
  
  private static Field getDeclaredField(Object object, String filedName)
  {
    for (Class<?> superClass = object.getClass(); superClass != Object.class; superClass = superClass.getSuperclass()) {
      try
      {
        return superClass.getDeclaredField(filedName);
      }
      catch (NoSuchFieldException e) {}
    }
    return null;
  }
  
  public static Object invokeMethod(Object object, String methodName, Class<?>[] parameterTypes, Object[] parameters)
    throws InvocationTargetException
  {
    Method method = getDeclaredMethod(object, methodName, parameterTypes);
    if (method == null) {
      throw new IllegalArgumentException("Could not find method [" + methodName + "] on target [" + object + "]");
    }
    method.setAccessible(true);
    try
    {
      return method.invoke(object, parameters);
    }
    catch (IllegalAccessException e) {}
    return null;
  }
  
  public static void setFieldValue(Object object, String fieldName, Object value)
  {
    Field field = getDeclaredField(object, fieldName);
    if (field == null) {
      throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + object + "]");
    }
    makeAccessible(field);
    try
    {
      field.set(object, value);
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }
  }
  
  public static Object getFieldValue(Object object, String fieldName)
  {
    Field field = getDeclaredField(object, fieldName);
    if (field == null) {
      throw new IllegalArgumentException("Could not find field [" + fieldName + "] on target [" + object + "]");
    }
    makeAccessible(field);
    
    Object result = null;
    try
    {
      result = field.get(object);
    }
    catch (IllegalAccessException e)
    {
      e.printStackTrace();
    }
    return result;
  }
}
