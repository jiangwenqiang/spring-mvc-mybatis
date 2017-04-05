package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.db2.visitor.DB2OutputVisitor;
import com.alibaba.druid.sql.dialect.postgresql.visitor.PGOutputVisitor;
import com.alibaba.druid.sql.dialect.sqlserver.visitor.SQLServerOutputVisitor;
import com.alibaba.druid.sql.visitor.SQLASTOutputVisitor;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.SerializationUtils;

public abstract class QueryExtractor
{
  public static final int SELECT = 0;
  public static final int INSERT = 1;
  public static final int UPDATE = 2;
  public static final int REMOVE = 3;
  protected static final String DB = "db";
  protected String collectionName;
  public abstract String getQueryForMongo();
  
  public abstract String getQueryForSQL();
  
  public static enum DBTYPE
  {
    MYSQL,  ORACLE,  SQLSERVER,  POSTGRESQL,  DB2,  H2,  DERBY,  SQLITE,  FIREBIRD,  SYSBASE,  OTHERSQL;
    
    private DBTYPE() {}
  }
  
  protected DBTYPE SQLDBType = DBTYPE.MYSQL;
  
  public void setSQLDBType(DBTYPE type)
  {
    this.SQLDBType = type;
  }
  
  public String getQueryForSQL(DBTYPE type)
  {
    setSQLDBType(type);
    
    return getQueryForSQL();
  }
  
  protected String getMongoQueryString(Query query)
  {
    if (query == null) {
      return "";
    }
    return SerializationUtils.serializeToJsonSafely(query.getQueryObject());
  }
  
  protected String getSQL(SQLStatement statement)
  {
    StringBuilder out = new StringBuilder();
    
    SQLASTOutputVisitor visitor = null;
    switch (SQLDBType)
    {
    case DB2: 
      visitor = new DB2OutputVisitor(out);
      break;
    case MYSQL: 
      visitor = new MySqlOutputVisitorWithText(out);
      break;
    case ORACLE: 
      visitor = new OracleOutputVisitorWithDate(out);
      break;
    case SQLSERVER: 
      visitor = new SQLServerOutputVisitor(out);
      break;
    case POSTGRESQL: 
      visitor = new PGOutputVisitor(out);
      break;
    default: 
      visitor = new SQLASTOutputVisitor(out);
    }
    statement.accept(visitor);
    return out.toString().replace("\n", " ").replace(";", "");
  }
}
