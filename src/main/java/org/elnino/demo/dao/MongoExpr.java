package org.elnino.demo.dao;

import java.util.HashMap;
import java.util.Map;

public class MongoExpr
{
  public static final int NOT_EQUAL = 1;
  public static final int LESS_THAN = 2;
  public static final int LESS_THAN_OR_EQUAL = 3;
  public static final int GREATER_THAN = 4;
  public static final int GREATER_THAN_OR_EQUAL = 5;
  public static final int NOT = 6;
  public static final int IN = 7;
  public static final int NOT_IN = 8;
  public static final int EXISTS = 9;
  public static final int AND = 10;
  public static final int OR = 11;
  public static final int NOR = 12;
  public static final int MOD = 13;
  public static final int ALL = 14;
  public static final int SIZE = 15;
  public static final int TYPE = 16;
  public static final int WITHIN = 17;
  public static final int NEAR = 18;
  public static final int NEAR_SPHERE = 19;
  public static final int MAX_DISTANCE = 20;
  public static final int ELEM_MATCH = 21;
  public static final int SET = 22;
  public static final int INC = 23;
  private static final Map<String, Integer> map = new HashMap();
  
  static
  {
    map.put("$ne", Integer.valueOf(1));
    map.put("$lt", Integer.valueOf(2));
    map.put("$lte", Integer.valueOf(3));
    map.put("$gt", Integer.valueOf(4));
    map.put("$gte", Integer.valueOf(5));
    map.put("$not", Integer.valueOf(6));
    map.put("$in", Integer.valueOf(7));
    map.put("$nin", Integer.valueOf(8));
    map.put("$exists", Integer.valueOf(9));
    map.put("$and", Integer.valueOf(10));
    map.put("$or", Integer.valueOf(11));
    map.put("$nor", Integer.valueOf(12));
    map.put("$mod", Integer.valueOf(13));
    map.put("$all", Integer.valueOf(14));
    map.put("$size", Integer.valueOf(15));
    map.put("$type", Integer.valueOf(16));
    map.put("$within", Integer.valueOf(17));
    map.put("$near", Integer.valueOf(18));
    map.put("$nearSphere", Integer.valueOf(19));
    map.put("$maxDistance", Integer.valueOf(20));
    map.put("$elemMatch", Integer.valueOf(21));
    map.put("$set", Integer.valueOf(22));
    map.put("$inc", Integer.valueOf(23));
  }
  
  public static int fromString(String op)
  {
    return ((Integer)map.get(op)).intValue();
  }
}
