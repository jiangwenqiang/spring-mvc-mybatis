package org.elnino.demo.dao;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public abstract interface FStorageOperations
{
  public abstract void destroy();
  
  public abstract <T> T selectOne(Query paramQuery, Class<T> paramClass);
  
  public abstract <T> T selectOne(Query paramQuery, Class<T> paramClass, String paramString);
  
  public abstract Map<String, Object> selectOne(Query paramQuery, String paramString);
  
  public abstract <T> List<T> select(Query paramQuery, Class<T> paramClass);
  
  public abstract <T> List<T> select(Query paramQuery, Class<T> paramClass, String paramString);
  
  public abstract List<Map<String, Object>> select(Query paramQuery, String paramString);
  
  public abstract long count(Query paramQuery, Class<?> paramClass);
  
  public abstract long count(Query paramQuery, String paramString);
  
  public abstract void insert(Object paramObject);
  
  public abstract void insert(Object paramObject, String paramString);
  
  public abstract void insert(Collection<? extends Object> paramCollection, Class<?> paramClass);
  
  public abstract void insert(Collection<? extends Object> paramCollection, String paramString);
  
  public abstract void insertAll(Collection<? extends Object> paramCollection);
  
  public abstract int updateOrInsert(Query paramQuery, Update paramUpdate, Class<?> paramClass);
  
  public abstract int updateOrInsert(Query paramQuery, Update paramUpdate, String paramString);
  
  public abstract int updateOrInsert(Query paramQuery, Update paramUpdate, Class<?> paramClass, String paramString);
  
  public abstract int update(Query paramQuery, Update paramUpdate, Class<?> paramClass);
  
  public abstract int update(Query paramQuery, Update paramUpdate, String paramString);
  
  public abstract int update(Query paramQuery, Update paramUpdate, Class<?> paramClass, String paramString);
  
  public abstract int delete(Object paramObject);
  
  public abstract int delete(Object paramObject, String paramString);
  
  public abstract int delete(Query paramQuery, Class<?> paramClass);
  
  public abstract int delete(Query paramQuery, Class<?> paramClass, String paramString);
  
  public abstract int delete(Query paramQuery, String paramString);
}
