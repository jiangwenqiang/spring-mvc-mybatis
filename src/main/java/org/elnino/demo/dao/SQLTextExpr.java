package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.ast.expr.SQLValuableExpr;

public class SQLTextExpr
  extends SQLCharExpr
  implements SQLValuableExpr
{
  public SQLTextExpr() {}
  
  public SQLTextExpr(String text)
  {
    super(text);
  }
  
  public void output(StringBuffer buf)
  {
    if ((this.text == null) || (this.text.length() == 0)) {
      buf.append("NULL");
    } else {
      buf.append(this.text);
    }
  }
}
