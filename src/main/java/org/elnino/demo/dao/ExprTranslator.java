package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLBooleanExpr;
import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLInListExpr;
import com.alibaba.druid.sql.ast.expr.SQLNotExpr;
import com.alibaba.druid.sql.ast.expr.SQLNullExpr;
import com.alibaba.druid.sql.ast.expr.SQLNumberExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryExpr;
import com.alibaba.druid.sql.ast.expr.SQLUnaryOperator;
import com.alibaba.druid.sql.ast.statement.SQLExprTableSource;
import com.alibaba.druid.sql.dialect.oracle.ast.expr.OracleDateExpr;
import com.mongodb.DBObject;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ExprTranslator
{
  private static final Log LOG = LogFactory.getLog(ExprTranslator.class);
  private static final String DATETIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
  private static final Map<String, SQLBinaryOperator> opMap = new HashMap();
  
  static
  {
    opMap.put("$ne", SQLBinaryOperator.NotEqual);
    opMap.put("$lt", SQLBinaryOperator.LessThan);
    opMap.put("$lte", SQLBinaryOperator.LessThanOrEqual);
    opMap.put("$gt", SQLBinaryOperator.GreaterThan);
    opMap.put("$gte", SQLBinaryOperator.GreaterThanOrEqual);
    opMap.put("$and", SQLBinaryOperator.BooleanAnd);
    opMap.put("$or", SQLBinaryOperator.BooleanOr);
  }
  
  public static SQLExprTableSource translateTableName(String tableName)
  {
    return new SQLExprTableSource(new SQLIdentifierExpr(tableName));
  }
  
  public static SQLExpr translateKey(String key)
  {
    return new SQLIdentifierExpr(key);
  }
  
  public static SQLExpr translateWhere(DBObject queryObj, QueryExtractor.DBTYPE dbType)
  {
    return parse(queryObj, dbType);
  }
  
  private static SQLExpr parse(DBObject object, QueryExtractor.DBTYPE dbType)
  {
    SQLExpr expr = null;
    boolean first = true;
    for (String key : object.keySet())
    {
      SQLExpr subExpr = null;
      if (key.startsWith("$"))
      {
        Object value = object.get(key);
        if (!(value instanceof List)) {
          throw new IllegalArgumentException("Syntex Error");
        }
        if ((key.equals("$and")) || (key.equals("$or"))) {
          subExpr = parseOperator(key, (List)value, dbType);
        } else if (key.equals("$nor")) {
          subExpr = new SQLUnaryExpr(SQLUnaryOperator.NOT, parseOperator("$and", (List)value, dbType));
        } else {
          throw new IllegalArgumentException("Syntex Error");
        }
      }
      else
      {
        subExpr = parseSingleKey(key, object.get(key), dbType);
      }
      if (first)
      {
        expr = subExpr;
        first = false;
      }
      else
      {
        expr = new SQLBinaryOpExpr(expr, key.equals("$or") ? SQLBinaryOperator.BooleanOr : SQLBinaryOperator.BooleanAnd, subExpr);
      }
    }
    return expr;
  }
  
  private static SQLExpr parseSingleKey(String key, Object object, QueryExtractor.DBTYPE dbType)
  {
    SQLExpr expr = null;
    SQLExpr left = new SQLIdentifierExpr(key);
    DBObject obj;
    boolean first;
    if ((object instanceof DBObject))
    {
      obj = (DBObject)object;
      first = true;
      for (String op : obj.keySet())
      {
        SQLExpr subExpr = null;
        switch (MongoExpr.fromString(op))
        {
        case 6: 
          subExpr = parseNot(key, obj.get(op), dbType);
          break;
        case 7: 
        case 8: 
          subExpr = parseIn(left, op.equals("$nin"), obj.get(op), dbType);
          break;
        case 9: 
          subExpr = parseExists(left, obj.get(op));
          break;
        case 1: 
        case 2: 
        case 3: 
        case 4: 
        case 5: 
          subExpr = parseRelational(left, op, obj.get(op), dbType);
          break;
        default: 
          LOG.warn("Unsupported operator: " + op);
          throw new IllegalArgumentException("Unsupported operator: " + op);
        }
        if (first)
        {
          expr = subExpr;
          first = false;
        }
        else
        {
          expr = new SQLBinaryOpExpr(expr, key.equals("$or") ? SQLBinaryOperator.BooleanOr : SQLBinaryOperator.BooleanAnd, subExpr);
        }
      }
    }
    else
    {
      SQLExpr right = null;
      if ((object instanceof Pattern))
      {
        expr = new SQLBinaryOpExpr(left, SQLBinaryOperator.Like, parseObject(object.toString(), dbType));
      }
      else
      {
        right = parseObject(object, dbType);
        expr = new SQLBinaryOpExpr(left, SQLBinaryOperator.Equality, right);
      }
    }
    return expr;
  }
  
  private static SQLExpr parseNot(String key, Object object, QueryExtractor.DBTYPE dbType)
  {
    return new SQLNotExpr(parseSingleKey(key, object, dbType));
  }
  
  private static SQLExpr parseIn(SQLExpr left, boolean not, Object object, QueryExtractor.DBTYPE dbType)
  {
    SQLInListExpr inListExpr = new SQLInListExpr(left, not);
    Collection collection = (Collection)object;
    for (Object obj : collection) {
      if (((obj instanceof String)) && (((String)obj).startsWith("(")) && (((String)obj).endsWith(")"))) {
        inListExpr.getTargetList().add(new SQLTextExpr((String)obj));
      } else {
        inListExpr.getTargetList().add(parseObject(obj, dbType));
      }
    }
    return inListExpr;
  }
  
  private static SQLExpr parseExists(SQLExpr left, Object object)
  {
    SQLExpr rightExpr = new SQLNullExpr();
    SQLExpr expr = new SQLBinaryOpExpr(left, ((Boolean)object).booleanValue() ? SQLBinaryOperator.IsNot : SQLBinaryOperator.Is, rightExpr);
    return expr;
  }
  
  public static SQLExpr parseObject(Object obj, QueryExtractor.DBTYPE dbType)
  {
    SQLExpr expr = null;
    if (obj == null)
    {
      expr = new SQLNullExpr();
    }
    else if ((obj instanceof Number))
    {
      expr = new SQLNumberExpr((Number)obj);
    }
    else if ((obj instanceof String))
    {
      expr = new SQLCharExpr((String)obj);
    }
    else if ((obj instanceof Boolean))
    {
      expr = new SQLBooleanExpr(((Boolean)obj).booleanValue());
    }
    else if ((obj instanceof Character))
    {
      expr = new SQLCharExpr(obj.toString());
    }
    else if ((obj instanceof Date))
    {
      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      String date = formatter.format((Date)obj);
      if (dbType == QueryExtractor.DBTYPE.ORACLE)
      {
        expr = new OracleDateExpr();
        ((OracleDateExpr)expr).setLiteral(date);
      }
      else
      {
        expr = new SQLCharExpr(date);
      }
    }
    else
    {
      LOG.warn("Unsupported data type");
      throw new IllegalArgumentException("Unsupported data type in query: " + obj.getClass().getName());
    }
    return expr;
  }
  
  private static SQLExpr parseRelational(SQLExpr left, String op, Object object, QueryExtractor.DBTYPE dbType)
  {
    SQLBinaryOperator oper = (SQLBinaryOperator)opMap.get(op);
    return new SQLBinaryOpExpr(left, oper, parseObject(object, dbType));
  }
  
  private static SQLExpr parseOperator(String key, List list, QueryExtractor.DBTYPE dbType)
  {
    SQLExpr expr = null;
    boolean first = true;
    for (Object object : list)
    {
      SQLExpr subExpr = parse((DBObject)object, dbType);
      if (first)
      {
        expr = subExpr;
        first = false;
      }
      else
      {
        SQLBinaryOperator op = (SQLBinaryOperator)opMap.get(key);
        expr = new SQLBinaryOpExpr(expr, key.equals("$or") ? SQLBinaryOperator.BooleanOr : SQLBinaryOperator.BooleanAnd, subExpr);
      }
    }
    return expr;
  }
}
