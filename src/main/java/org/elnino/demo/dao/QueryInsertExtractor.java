package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement.ValuesClause;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.postgresql.ast.stmt.PGInsertStatement;
import com.alibaba.druid.sql.dialect.sqlserver.ast.stmt.SQLServerInsertStatement;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.mongodb.core.query.SerializationUtils;

public class QueryInsertExtractor
  extends QueryExtractor
{
  private List<DBObject> dbDocList;
  private boolean multi = false;
  private List<String> columns;
  
  public QueryInsertExtractor(String collectionName, DBObject dbDoc)
  {
    this(collectionName, new ArrayList(Arrays.asList(new DBObject[] { dbDoc })));
  }
  
  public QueryInsertExtractor(String collectionName, List<DBObject> dbDocList)
  {
    this.collectionName = collectionName;
    if (dbDocList.isEmpty()) {
      throw new IllegalArgumentException("Error: DBObject list is empty!");
    }
    this.dbDocList = dbDocList;
    init();
  }
  
  private void init()
  {
    for (DBObject obj : this.dbDocList)
    {
      obj.removeField("_class");
      obj.removeField("_id");
    }
    this.multi = (this.dbDocList.size() > 1);
    this.columns = new ArrayList(((DBObject)this.dbDocList.get(0)).keySet());
  }
  
  public String getQueryForMongo()
  {
    StringBuilder sb = new StringBuilder();
    sb.append("db").append(".").append(this.collectionName).append(".insert(");
    if (!this.multi)
    {
      sb.append(SerializationUtils.serializeToJsonSafely(this.dbDocList.get(0)));
    }
    else
    {
      BasicDBList dbList = new BasicDBList();
      for (DBObject obj : this.dbDocList) {
        dbList.add(obj);
      }
      sb.append(SerializationUtils.serializeToJsonSafely(dbList));
    }
    sb.append(")");
    return sb.toString();
  }
  
  public String getQueryForSQL()
  {
    SQLInsertStatement stmt = null;
    switch (SQLDBType)
    {
    case MYSQL: 
      stmt = new MySqlInsertStatement();
      break;
    case ORACLE: 
      stmt = new OracleInsertStatement();
      break;
    case SQLSERVER: 
      stmt = new SQLServerInsertStatement();
      break;
    case POSTGRESQL: 
      stmt = new PGInsertStatement();
      break;
    default: 
      stmt = new SQLInsertStatement();
    }
    stmt.setTableSource(ExprTranslator.translateTableName(this.collectionName));
    parseColumns(this.columns, stmt);
    parseValuesList(this.columns, this.dbDocList, stmt);
    return getSQL(stmt);
  }
  
  private void parseColumns(List<String> keys, SQLInsertStatement stmt)
  {
    List<SQLExpr> columns = stmt.getColumns();
    for (String key : keys) {
      columns.add(ExprTranslator.translateKey(key));
    }
  }
  
  private SQLInsertStatement.ValuesClause parseValues(List<String> keys, DBObject dbDoc)
  {
    SQLInsertStatement.ValuesClause values = new SQLInsertStatement.ValuesClause();
    for (String key : keys) {
      values.addValue(ExprTranslator.parseObject(dbDoc.get(key), this.SQLDBType));
    }
    return values;
  }
  
  private void parseValuesList(List<String> keys, List<DBObject> dbDocList, SQLInsertStatement stmt)
  {
    List<SQLInsertStatement.ValuesClause> valuesList = null;
    if ((stmt instanceof MySqlInsertStatement)) {
      valuesList = ((MySqlInsertStatement)stmt).getValuesList();
    } else if ((stmt instanceof SQLServerInsertStatement)) {
      valuesList = ((SQLServerInsertStatement)stmt).getValuesList();
    } else if ((stmt instanceof PGInsertStatement)) {
      valuesList = ((PGInsertStatement)stmt).getValuesList();
    }
    if (valuesList != null) {
      for (DBObject obj : dbDocList) {
        valuesList.add(parseValues(keys, obj));
      }
    } else {
      stmt.setValues(parseValues(keys, (DBObject)dbDocList.get(0)));
    }
  }
}
