package org.elnino.demo.dao;

import com.alibaba.druid.sql.ast.expr.SQLCharExpr;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlOutputVisitor;

public class MySqlOutputVisitorWithText
  extends MySqlOutputVisitor
{
  public MySqlOutputVisitorWithText(Appendable appender)
  {
    super(appender);
  }
  
  public boolean visit(SQLCharExpr x)
  {
    if ((x instanceof SQLTextExpr))
    {
      print(x.getText());
      return false;
    }
    return super.visit(x);
  }
}
