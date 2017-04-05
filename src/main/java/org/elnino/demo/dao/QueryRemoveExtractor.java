package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.statement.SQLDeleteStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlDeleteStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleDeleteStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGDeleteStatement;
import org.springframework.data.mongodb.core.query.Query;

public class QueryRemoveExtractor
  extends QueryExtractor
{
  private Query query;
  
  public QueryRemoveExtractor(String collectionName, Query query)
  {
    this.collectionName = collectionName;
    this.query = query;
  }
  
  public String getQueryForMongo()
  {
    StringBuilder sb = new StringBuilder();
    
    sb.append("db").append(".").append(this.collectionName).append(".remove(").append(getMongoQueryString(this.query)).append(")");
    return sb.toString();
  }
  
  public String getQueryForSQL()
  {
    SQLDeleteStatement stmt = null;
    switch (SQLDBType)
    {
    case MYSQL: 
      stmt = new MySqlDeleteStatement();
      break;
    case ORACLE: 
      stmt = new OracleDeleteStatement();
      break;
    case POSTGRESQL: 
      stmt = new PGDeleteStatement();
      break;
    default: 
      stmt = new SQLDeleteStatement();
    }
    stmt.setTableSource(ExprTranslator.translateTableName(this.collectionName));
    if ((this.query != null) && (this.query.getQueryObject() != null)) {
      stmt.setWhere(ExprTranslator.translateWhere(this.query.getQueryObject(), this.SQLDBType));
    }
    return getSQL(stmt);
  }
}
