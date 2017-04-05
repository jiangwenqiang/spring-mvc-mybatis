package org.elnino.demo.dao;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;

public class TotalResultHandler
  implements ResultHandler
{
  public long total = 0L;
  
  public void handleResult(ResultContext context)
  {
    this.total = context.getResultCount();
  }
  
  public long getTotal()
  {
    return this.total;
  }
}
