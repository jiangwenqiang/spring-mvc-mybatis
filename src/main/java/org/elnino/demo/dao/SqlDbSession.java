package org.elnino.demo.dao;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;

public class SqlDbSession
{
  private SqlSession sqlSession;
  private ExecutorType executorType;
  private boolean begintrans;
  
  public SqlDbSession(SqlSessionTemplate sqlTemplate)
  {
    this(sqlTemplate, ExecutorType.BATCH);
  }
  
  public SqlDbSession(SqlSessionTemplate sqlTemplate, ExecutorType executorType)
  {
    this.executorType = executorType;
    this.sqlSession = sqlTemplate.getSqlSessionFactory().openSession(this.executorType, false);
  }
  
  public void close()
  {
    if (this.sqlSession != null)
    {
      if (this.begintrans) {
        rollback();
      }
      this.sqlSession.close();
      this.sqlSession = null;
    }
  }
  
  public void beginTrans()
  {
    try
    {
      this.begintrans = true;
      this.sqlSession.getConnection().setAutoCommit(!this.begintrans);
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }
  
  public void commit()
  {
    try
    {
      if (this.executorType == ExecutorType.BATCH) {
        this.sqlSession.flushStatements();
      }
      this.sqlSession.commit();
      this.sqlSession.getConnection().commit();
      this.sqlSession.clearCache();
      

      this.begintrans = false;
      this.sqlSession.getConnection().setAutoCommit(!this.begintrans);
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }
  
  public void rollback()
  {
    try
    {
      this.sqlSession.rollback();
      this.sqlSession.getConnection().rollback();
      this.sqlSession.clearCache();
      

      this.begintrans = false;
      this.sqlSession.getConnection().setAutoCommit(!this.begintrans);
    }
    catch (SQLException e)
    {
      e.printStackTrace();
    }
  }
  
  private String makeFullSqlStatement(String nameSpace, String sqlId)
  {
    return FMybatisTemplate.makeFullSqlStatement(nameSpace, sqlId);
  }
  
  public <T> T selectOne(String nameSpace, String sqlId)
  {
    return this.sqlSession.selectOne(makeFullSqlStatement(nameSpace, sqlId));
  }
  
  public <T> T selectOne(String nameSpace, String sqlId, Object paramObject)
  {
    return this.sqlSession.selectOne(makeFullSqlStatement(nameSpace, sqlId), paramObject);
  }
  
  public <E> List<E> selectList(String nameSpace, String sqlId)
  {
    return this.sqlSession.selectList(makeFullSqlStatement(nameSpace, sqlId));
  }
  
  public <E> List<E> selectList(String nameSpace, String sqlId, Object paramObject)
  {
    return this.sqlSession.selectList(makeFullSqlStatement(nameSpace, sqlId), paramObject);
  }
  
  public <E> List<E> selectList(String nameSpace, String sqlId, Object paramObject, RowBounds paramRowBounds)
  {
    return this.sqlSession.selectList(makeFullSqlStatement(nameSpace, sqlId), paramObject, paramRowBounds);
  }
  
  public <E> List<E> selectList(String nameSpace, String sqlId, Object paramObject, int offset, int limit)
  {
    return this.sqlSession.selectList(makeFullSqlStatement(nameSpace, sqlId), paramObject, new RowBounds(offset, limit));
  }
  
  public <K, V> Map<K, V> selectMap(String nameSpace, String sqlId, String fields)
  {
    return this.sqlSession.selectMap(makeFullSqlStatement(nameSpace, sqlId), fields);
  }
  
  public <K, V> Map<K, V> selectMap(String nameSpace, String sqlId, Object paramObject, String fields)
  {
    return this.sqlSession.selectMap(makeFullSqlStatement(nameSpace, sqlId), paramObject, fields);
  }
  
  public <K, V> Map<K, V> selectMap(String nameSpace, String sqlId, Object paramObject, String fields, RowBounds paramRowBounds)
  {
    return this.sqlSession.selectMap(makeFullSqlStatement(nameSpace, sqlId), paramObject, fields, paramRowBounds);
  }
  
  public <K, V> Map<K, V> selectMap(String nameSpace, String sqlId, Object paramObject, String fields, int offset, int limit)
  {
    return this.sqlSession.selectMap(makeFullSqlStatement(nameSpace, sqlId), paramObject, fields, new RowBounds(offset, limit));
  }
  
  public void select(String nameSpace, String sqlId, Object paramObject, ResultHandler paramResultHandler)
  {
    this.sqlSession.select(makeFullSqlStatement(nameSpace, sqlId), paramObject, paramResultHandler);
  }
  
  public void select(String nameSpace, String sqlId, ResultHandler paramResultHandler)
  {
    this.sqlSession.select(makeFullSqlStatement(nameSpace, sqlId), paramResultHandler);
  }
  
  public void select(String nameSpace, String sqlId, Object paramObject, RowBounds paramRowBounds, ResultHandler paramResultHandler)
  {
    this.sqlSession.select(makeFullSqlStatement(nameSpace, sqlId), paramObject, paramRowBounds, paramResultHandler);
  }
  
  public void select(String nameSpace, String sqlId, Object paramObject, int offset, int limit, ResultHandler paramResultHandler)
  {
    this.sqlSession.select(makeFullSqlStatement(nameSpace, sqlId), paramObject, new RowBounds(offset, limit), paramResultHandler);
  }
  
  public long count(String nameSpace, String sqlId)
  {
    TotalResultHandler handler = new TotalResultHandler();
    select(nameSpace, sqlId, handler);
    return handler.getTotal();
  }
  
  public long count(String nameSpace, String sqlId, Object paramObject)
  {
    TotalResultHandler handler = new TotalResultHandler();
    select(nameSpace, sqlId, paramObject, handler);
    return handler.getTotal();
  }
  
  public int insert(String nameSpace, String sqlId)
  {
    return this.sqlSession.insert(makeFullSqlStatement(nameSpace, sqlId));
  }
  
  public int insert(String nameSpace, String sqlId, Object paramObject)
  {
    String statement = makeFullSqlStatement(nameSpace, sqlId);
    if ((paramObject instanceof Collection))
    {
      int n = 0;
      Collection<? extends Object> collection = (Collection)paramObject;
      for (Object obj : collection) {
        n += this.sqlSession.insert(statement, obj);
      }
      return n;
    }
    return this.sqlSession.insert(statement, paramObject);
  }
  
  public int insertBatch(String nameSpace, String sqlId, Object paramObject)
  {
    return this.sqlSession.insert(makeFullSqlStatement(nameSpace, sqlId), paramObject);
  }
  
  public int update(String nameSpace, String sqlId)
  {
    return this.sqlSession.update(makeFullSqlStatement(nameSpace, sqlId));
  }
  
  public int update(String nameSpace, String sqlId, Object paramObject)
  {
    String statement = makeFullSqlStatement(nameSpace, sqlId);
    if ((paramObject instanceof Collection))
    {
      int n = 0;
      Collection<? extends Object> collection = (Collection)paramObject;
      for (Object obj : collection) {
        n += this.sqlSession.update(statement, obj);
      }
      return n;
    }
    return this.sqlSession.update(statement, paramObject);
  }
  
  public int updateBatch(String nameSpace, String sqlId, Object paramObject)
  {
    return this.sqlSession.update(makeFullSqlStatement(nameSpace, sqlId), paramObject);
  }
  
  public int delete(String nameSpace, String sqlId)
  {
    return this.sqlSession.delete(makeFullSqlStatement(nameSpace, sqlId));
  }
  
  public int delete(String nameSpace, String sqlId, Object paramObject)
  {
    String statement = makeFullSqlStatement(nameSpace, sqlId);
    if ((paramObject instanceof Collection))
    {
      int n = 0;
      Collection<? extends Object> collection = (Collection)paramObject;
      for (Object obj : collection) {
        n += this.sqlSession.delete(statement, obj);
      }
      return n;
    }
    return this.sqlSession.delete(statement, paramObject);
  }
  
  public int deleteBatch(String nameSpace, String sqlId, Object paramObject)
  {
    return this.sqlSession.delete(makeFullSqlStatement(nameSpace, sqlId), paramObject);
  }
}
