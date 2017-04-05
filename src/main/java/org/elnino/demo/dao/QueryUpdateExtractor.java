package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.statement.SQLUpdateSetItem;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlSelectQueryBlock.Limit;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlUpdateStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGUpdateStatement;
import com.alibaba.druid.sql.dialect.sqlserver.ast.stmt.SQLServerUpdateStatement;
import com.mongodb.DBObject;
import java.util.List;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.SerializationUtils;
import org.springframework.data.mongodb.core.query.Update;

public class QueryUpdateExtractor
  extends QueryExtractor
{
  private Query query;
  private Update update;
  private boolean upsert;
  private boolean multi;
  
  public QueryUpdateExtractor(String collectionName, Query query, Update update)
  {
    this(collectionName, query, update, false, true);
  }
  
  public QueryUpdateExtractor(String collectionName, Query query, Update update, boolean upsert, boolean multi)
  {
    this.collectionName = collectionName;
    this.query = query;
    this.update = update;
    this.upsert = upsert;
    this.multi = multi;
  }
  
  public String getQueryForMongo()
  {
    StringBuilder sb = new StringBuilder();
    

    sb.append("db").append(".").append(this.collectionName).append(".update(").append(getMongoQueryString(this.query));
    if (this.update != null)
    {
      sb.append(", ");
      sb.append(SerializationUtils.serializeToJsonSafely(this.update.getUpdateObject()));
    }
    if ((this.upsert) || (this.multi))
    {
      sb.append(", {");
      String key = "upsert";
      if (this.multi) {
        key = "multi";
      }
      sb.append("\"").append(key).append("\" : true");
      sb.append("}");
    }
    sb.append(")");
    return sb.toString();
  }
  
  public String getQueryForSQL()
  {
    if (this.upsert) {
      throw new IllegalArgumentException("Upsert not supported");
    }
    SQLUpdateStatement stmt = null;
    switch (SQLDBType)
    {
    case MYSQL: 
      stmt = new MySqlUpdateStatement();
      break;
    case ORACLE: 
      stmt = new OracleUpdateStatement();
      break;
    case SQLSERVER: 
      stmt = new SQLServerUpdateStatement();
      break;
    case POSTGRESQL: 
      stmt = new PGUpdateStatement();
      break;
    default: 
      stmt = new SQLUpdateStatement();
    }
    stmt.setTableSource(ExprTranslator.translateTableName(this.collectionName));
    if ((this.query != null) && (this.query.getQueryObject() != null)) {
      stmt.setWhere(ExprTranslator.translateWhere(this.query.getQueryObject(), this.SQLDBType));
    }
    parseSetItems(this.update, stmt);
    if (!this.multi) {
      if ((stmt instanceof MySqlUpdateStatement))
      {
        MySqlSelectQueryBlock.Limit limit = new MySqlSelectQueryBlock.Limit();
        limit.setRowCount(new SQLNumberExpr(Integer.valueOf(1)));
        ((MySqlUpdateStatement)stmt).setLimit(limit);
      }
    }
    return getSQL(stmt);
  }
  
  private void parseSetItems(Update update, SQLUpdateStatement stmt)
  {
    List<SQLUpdateSetItem> updateItems = stmt.getItems();
    DBObject updateObj = update.getUpdateObject();
    for (String op : updateObj.keySet())
    {
      DBObject obj = (DBObject)updateObj.get(op);
      switch (MongoExpr.fromString(op))
      {
      case 22: 
        for (String key : obj.keySet())
        {
          SQLUpdateSetItem item = new SQLUpdateSetItem();
          item.setColumn(ExprTranslator.translateKey(key));
          item.setValue(ExprTranslator.parseObject(obj.get(key), this.SQLDBType));
          updateItems.add(item);
        }
        break;
      case 23: 
        for (String key : obj.keySet())
        {
          SQLUpdateSetItem item = new SQLUpdateSetItem();
          SQLExpr keyExpr = ExprTranslator.translateKey(key);
          item.setColumn(keyExpr);
          SQLExpr incValueExpr = ExprTranslator.parseObject(obj.get(key), this.SQLDBType);
          SQLExpr valExpr = new SQLBinaryOpExpr(keyExpr, SQLBinaryOperator.Add, incValueExpr);
          item.setValue(valExpr);
          updateItems.add(item);
        }
        break;
      default: 
        throw new IllegalArgumentException("Unsupported array operator: " + op);
      }
    }
  }
}
