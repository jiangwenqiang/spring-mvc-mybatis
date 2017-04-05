package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQuery;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.ast.statement.SQLSelectStatement;
import com.alibaba.druid.sql.dialect.db2.ast.stmt.DB2SelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock.Limit;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelect;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleSelectQueryBlock;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectQueryBlock;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGSelectStatement;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelect;
import com.alibaba.druid.sql.dialect.sqlserver.ast.SQLServerSelectQueryBlock;
import com.mongodb.DBObject;
import java.util.List;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.SerializationUtils;

public class QuerySelectExtractor
  extends QueryExtractor
{
  private Query query;
  private boolean needOrderBy = true;
  
  public QuerySelectExtractor(String collectionName, Query query)
  {
    this.collectionName = collectionName;
    this.query = query;
  }
  
  public String getQueryForMongo()
  {
    StringBuilder sb = new StringBuilder();
    

    sb.append("db").append(".").append(this.collectionName).append(".find(").append(getMongoQueryString(this.query));
    if ((this.query != null) && (this.query.getFieldsObject() != null)) {
      sb.append(",").append(SerializationUtils.serializeToJsonSafely(this.query.getFieldsObject()));
    }
    sb.append(")");
    if ((this.query != null) && (this.query.getSortObject() != null)) {
      sb.append(".sort(").append(SerializationUtils.serializeToJsonSafely(this.query.getSortObject())).append(")");
    }
    if ((this.query != null) && (this.query.getLimit() > 0)) {
      sb.append(".limit(").append(this.query.getLimit()).append(")");
    }
    return sb.toString();
  }
  
  public String getQueryForSQL()
  {
    SQLSelect select = null;
    switch (SQLDBType)
    {
    case ORACLE: 
      select = new OracleSelect();
      break;
    case SQLSERVER: 
      select = new SQLServerSelect();
      break;
    default: 
      select = new SQLSelect();
    }
    select.setQuery(parseSelectQuery(this.collectionName, this.query));
    if (this.needOrderBy) {
      select.setOrderBy(parseOrderBy(this.query));
    }
    SQLSelectStatement stmt = null;
    switch (SQLDBType)
    {
    case POSTGRESQL: 
      stmt = new PGSelectStatement(select);
      break;
    case ORACLE: 
      stmt = new OracleSelectStatementWithLimit(select, this.query.getLimit());
      break;
    default: 
      stmt = new SQLSelectStatement(select);
    }
    return getSQL(stmt);
  }
  
  private SQLOrderBy parseOrderBy(Query query)
  {
    SQLOrderBy orderBy = new SQLOrderBy();
    DBObject sortObj;
    if ((query != null) && (query.getSortObject() != null))
    {
      sortObj = query.getSortObject();
      for (String key : sortObj.keySet())
      {
        SQLSelectOrderByItem item = new SQLSelectOrderByItem();
        item.setExpr(ExprTranslator.translateKey(key));
        Integer value = (Integer)sortObj.get(key);
        if (value.intValue() == 1) {
          item.setType(SQLOrderingSpecification.ASC);
        } else if (value.intValue() == -1) {
          item.setType(SQLOrderingSpecification.DESC);
        }
        orderBy.addItem(item);
      }
    }
    return orderBy;
  }
  
  private SQLSelectQuery parseSelectQuery(String collectionName, Query query)
  {
    SQLSelectQueryBlock queryBlock = null;
    switch (SQLDBType)
    {
    case DB2: 
      queryBlock = new DB2SelectQueryBlock();
      break;
    case MYSQL: 
      queryBlock = new MySqlSelectQueryBlock();
      break;
    case ORACLE: 
      queryBlock = new OracleSelectQueryBlock();
      break;
    case SQLSERVER: 
      queryBlock = new SQLServerSelectQueryBlock();
      break;
    case POSTGRESQL: 
      queryBlock = new PGSelectQueryBlock();
      break;
    default: 
      queryBlock = new SQLSelectQueryBlock();
    }
    if ((query != null) && (query.getLimit() > 0) && ((queryBlock instanceof MySqlSelectQueryBlock)))
    {
      MySqlSelectQueryBlock.Limit limit = new MySqlSelectQueryBlock.Limit();
      limit.setRowCount(new SQLNumberExpr(Integer.valueOf(query.getLimit())));
      limit.setOffset(new SQLNumberExpr(Integer.valueOf(query.getSkip())));
      ((MySqlSelectQueryBlock)queryBlock).setLimit(limit);
      ((MySqlSelectQueryBlock)queryBlock).setOrderBy(parseOrderBy(query));
      this.needOrderBy = false;
    }
    queryBlock.setFrom(ExprTranslator.translateTableName(collectionName));
    parseSelectList(query, queryBlock);
    parseWhere(query, queryBlock);
    return queryBlock;
  }
  
  private void parseWhere(Query query, SQLSelectQueryBlock queryBlock)
  {
    if ((query != null) && (query.getQueryObject() != null))
    {
      DBObject queryObj = query.getQueryObject();
      queryBlock.setWhere(ExprTranslator.translateWhere(queryObj, this.SQLDBType));
    }
  }
  
  private void parseSelectList(Query query, SQLSelectQueryBlock queryBlock)
  {
    List<SQLSelectItem> selectList = queryBlock.getSelectList();
    DBObject fieldObj;
    if ((query != null) && (query.getFieldsObject() != null))
    {
      fieldObj = query.getFieldsObject();
      for (String key : fieldObj.keySet())
      {
        Object value = fieldObj.get(key);
        if (((value instanceof Integer)) && (((Integer)value).intValue() == 1))
        {
          SQLSelectItem selectItem = new SQLSelectItem(ExprTranslator.translateKey(key), null);
          selectList.add(selectItem);
        }
      }
    }
    else
    {
      SQLSelectItem selectItem = new SQLSelectItem(new SQLAllColumnExpr(), null);
      selectList.add(selectItem);
    }
  }
}
