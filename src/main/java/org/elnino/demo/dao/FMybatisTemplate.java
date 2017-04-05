package org.elnino.demo.dao;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.session.SqlSessionFactory;
import org.elnino.demo.utils.ReflectUtil;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mapping.model.MappingException;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.util.TypeUtils;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.util.JSON;
import com.mongodb.util.JSONParseException;

public class FMybatisTemplate implements FStorageOperations {
	private SqlSessionTemplate sqlTemplate;
	private ExecutorType executorType;
	private final int lostAffect = -2147482646;
	private QueryExtractor.DBTYPE dbType = QueryExtractor.DBTYPE.MYSQL;
	private String chartset = null;
	private final String SQL_SELECT = "mybatis.sql.select";
	private final String SQL_INSERT = "mybatis.sql.insert";
	private final String SQL_UPDATE = "mybatis.sql.update";
	private final String SQL_DELETE = "mybatis.sql.delete";
	private FStorageLogger logger = new FStorageLogger();

	public FMybatisTemplate(SqlSessionFactory sqlSessionFactory) {
		this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType(), null);
	}

	public FMybatisTemplate(SqlSessionFactory sqlSessionFactory, String characterSet) {
		this(sqlSessionFactory, sqlSessionFactory.getConfiguration().getDefaultExecutorType(), characterSet);
	}

	public FMybatisTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType) {
		this(sqlSessionFactory, executorType, null);
	}

	public FMybatisTemplate(SqlSessionFactory sqlSessionFactory, ExecutorType executorType, String characterSet) {
		this.executorType = executorType;
		this.sqlTemplate = new SqlSessionTemplate(sqlSessionFactory, ExecutorType.SIMPLE);
		this.dbType = getDBType();
		this.chartset = characterSet;
	}

	public SqlSessionTemplate getSqlSessionTemplate() {
		return this.sqlTemplate;
	}

	public SqlDbSession getSqlDbSession() {
		SqlDbSession dbSession = new SqlDbSession(this.sqlTemplate, this.executorType);
		return dbSession;
	}

	public static String makeFullSqlStatement(String nameSpace, String sqlId) {
		if (StringUtils.isEmpty(sqlId)) {
			return nameSpace;
		}
		return nameSpace + "." + sqlId;
	}

	public void destroy() {
	}

	public <T> T selectOne(Query query, Class<T> entityClass) {
		return selectOne(query, entityClass, fetchAnnotationTableName(entityClass));
	}

	public <T> T selectOne(Query query, Class<T> entityClass, String tableName) {
		int limit = query.getLimit();
		query.limit(1);

		QuerySelectExtractor extractor = new QuerySelectExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);

		query.limit(limit);

		return toJavaObject(this.sqlTemplate.selectOne("mybatis.sql.select", convertChartsetSQL(sql)), entityClass);
	}

	public Map<String, Object> selectOne(Query query, String tableName) {
		int limit = query.getLimit();
		query.limit(1);

		QuerySelectExtractor extractor = new QuerySelectExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);

		query.limit(limit);
		// List<Map<String, Object>> listMap =
		// this.sqlTemplate.selectList("mybatis.sql.select",
		// convertChartsetSQL(sql));
		// return listMap.get(0);
		// return (Map)this.sqlTemplate.selectOne("mybatis.sql.select",
		// convertChartsetSQL(sql));
		return this.sqlTemplate.selectOne("mybatis.sql.select", convertChartsetSQL(sql));
	}

	public <T> List<T> select(Query query, Class<T> entityClass) {
		return select(query, entityClass, fetchAnnotationTableName(entityClass));
	}

	public <T> List<T> select(Query query, Class<T> entityClass, String tableName) {
		QuerySelectExtractor extractor = new QuerySelectExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);
		if ((query != null) && ((query.getLimit() > 0) || (query.getSkip() > 0))
				&& (this.dbType != QueryExtractor.DBTYPE.MYSQL)) {
			int offset = query.getSkip() > 0 ? query.getSkip() : 0;
			int limit = query.getLimit() > 0 ? query.getLimit() : 2147483647;
			RowBounds rowBounds = new RowBounds(offset, limit);
			return toJavaObjectList(
					this.sqlTemplate.selectList("mybatis.sql.select", convertChartsetSQL(sql), rowBounds), entityClass);
		}
		return toJavaObjectList(this.sqlTemplate.selectList("mybatis.sql.select", convertChartsetSQL(sql)),
				entityClass);
	}

	public List<Map<String, Object>> select(Query query, String tableName) {
		QuerySelectExtractor extractor = new QuerySelectExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);
		if ((query != null) && ((query.getLimit() > 0) || (query.getSkip() > 0))
				&& (this.dbType != QueryExtractor.DBTYPE.MYSQL)) {
			int offset = query.getSkip() > 0 ? query.getSkip() : 0;
			int limit = query.getLimit() > 0 ? query.getLimit() : 2147483647;
			RowBounds rowBounds = new RowBounds(offset, limit);
			return this.sqlTemplate.selectList("mybatis.sql.select", convertChartsetSQL(sql), rowBounds);
		}
		return this.sqlTemplate.selectList("mybatis.sql.select", convertChartsetSQL(sql));
	}

	public long count(Query query, Class<?> entityClass) {
		return count(query, fetchAnnotationTableName(entityClass));
	}

	public long count(Query query, String tableName) {
		int oldlimit = 0;
		int oldskip = 0;
		if (query != null) {
			oldlimit = query.getLimit();
			oldskip = query.getSkip();

			query.limit(0);
			query.skip(0);
		}
		QuerySelectExtractor extractor = new QuerySelectExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);
		if (oldlimit > 0) {
			query.limit(oldlimit);
		}
		if (oldskip > 0) {
			query.skip(oldskip);
		}
		sql = "select count(*) total from (" + sql + " ) ta";
		Map<String, Object> map = (Map<String, Object>) this.sqlTemplate.selectOne("mybatis.sql.select", sql);
		return Long.parseLong(map.get("total").toString());
	}

	public void insert(Object objectToSave) {
		insert(objectToSave, fetchAnnotationTableName(objectToSave.getClass()));
	}

	public void insert(Object objectToSave, String tableName) {
		QueryInsertExtractor extractor = new QueryInsertExtractor(tableName, toDbObject(objectToSave));
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);

		this.sqlTemplate.insert("mybatis.sql.insert", convertChartsetSQL(sql));
	}

	public void insert(Collection<? extends Object> batchToSave, Class<?> entityClass) {
		Object obj;
		for (Iterator i$ = batchToSave.iterator(); i$.hasNext(); insert(obj)) {
			obj = i$.next();
		}
	}

	public void insert(Collection<? extends Object> batchToSave, String tableName) {
		Object obj;
		for (Iterator i$ = batchToSave.iterator(); i$.hasNext(); insert(obj, tableName)) {
			obj = i$.next();
		}
	}

	public void insertAll(Collection<? extends Object> objectsToSave) {
		Object obj;
		for (Iterator i$ = objectsToSave.iterator(); i$.hasNext(); insert(obj)) {
			obj = i$.next();
		}
	}

	public int updateOrInsert(Query query, Update update, Class<?> entityClass) {
		return updateOrInsert(query, update, entityClass, fetchAnnotationTableName(entityClass));
	}

	public int updateOrInsert(Query query, Update update, String tableName) {
		return updateOrInsert(query, update, null, tableName);
	}

	public int updateOrInsert(Query query, Update update, Class<?> entityClass, String tableName) {
		int n = update(query, update, tableName);
		if (n <= 0) {
			insert(update, tableName);
		}
		return n;
	}

	public int update(Query query, Update update, Class<?> entityClass) {
		return update(query, update, entityClass, fetchAnnotationTableName(entityClass));
	}

	public int update(Query query, Update update, String tableName) {
		return update(query, update, null, tableName);
	}

	public int update(Query query, Update update, Class<?> entityClass, String tableName) {
		QueryUpdateExtractor extractor = new QueryUpdateExtractor(tableName, query, update);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);

		int n = this.sqlTemplate.update("mybatis.sql.update", convertChartsetSQL(sql));
		if (n == -2147482646) {
			return 1;
		}
		return n;
	}

	public int delete(Object object) {
		return delete(object, fetchAnnotationTableName(object.getClass()));
	}

	public int delete(Object object, String tableName) {
		Criteria criteria = null;

		java.lang.reflect.Field fld = fetchDeclaredField(object.getClass(), "ID_KEY");
		if (fld != null) {
			try {
				String idkey = fld.get(null).toString();
				fld = fetchDeclaredField(object.getClass(), idkey);
				if (fld != null) {
					criteria = Criteria.where(idkey).is(fld.get(object));
				}
			} catch (Exception e) {
			}
		}
		DBObject dbDoc;
		if (criteria == null) {
			criteria = new Criteria();
			dbDoc = toDbObject(object);
			Set<String> keys = dbDoc.keySet();
			for (String key : keys) {
				Object obj = dbDoc.get(key);
				if (!StringUtils.isEmpty(obj)) {
					criteria = criteria.and(key).is(obj);
				}
			}
		}
		Query query = new Query(criteria);
		return delete(query, tableName);
	}

	public int delete(Query query, Class<?> entityClass) {
		return delete(query, entityClass, fetchAnnotationTableName(entityClass));
	}

	public int delete(Query query, Class<?> entityClass, String tableName) {
		return delete(query, tableName);
	}

	public int delete(Query query, String tableName) {
		QueryRemoveExtractor extractor = new QueryRemoveExtractor(tableName, query);
		String sql = extractor.getQueryForSQL(this.dbType);
		this.logger.logSQLQuery(sql);

		int n = this.sqlTemplate.delete("mybatis.sql.delete", convertChartsetSQL(sql));
		if (n == -2147482646) {
			return 1;
		}
		return n;
	}

	private String fetchAnnotationTableName(Class<?> cl) {
		Annotation[] annos = cl.getAnnotations();
		for (Annotation ann : annos) {
			if ((ann instanceof Document)) {
				return ((Document) ann).collection();
			}
		}
		String name = cl.getName();
		name = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		return name.endsWith("bean") ? name.substring(0, name.length() - 4) : name;
	}

	private java.lang.reflect.Field fetchDeclaredField(Class<?> classDefine, String fieldName) {
		java.lang.reflect.Field field = null;
		try {
			field = classDefine.getDeclaredField(fieldName);
			field.setAccessible(true);
		} catch (NoSuchFieldException e) {
			if (classDefine.getSuperclass() != null) {
				return fetchDeclaredField(classDefine.getSuperclass(), fieldName);
			}
		}
		return field;
	}

	private StringBuffer fetchAllDeclaredField(Class<?> classDefine, StringBuffer sb) {
		java.lang.reflect.Field[] flds = classDefine.getDeclaredFields();
		for (java.lang.reflect.Field fld : flds) {
			if (!Modifier.isStatic(fld.getModifiers())) {
				String fldname = fld.getName();

				boolean istransient = false;
				Annotation[] annos = fld.getAnnotations();
				for (Annotation ann : annos) {
					if ((ann instanceof Transient)) {
						istransient = true;
						break;
					}
					if ((ann instanceof org.springframework.data.mongodb.core.mapping.Field)) {
						fldname = ((org.springframework.data.mongodb.core.mapping.Field) ann).value();
					}
				}
				if (!istransient) {
					sb.append(fldname + ",");
				}
			}
		}
		if (classDefine.getSuperclass() != null) {
			fetchAllDeclaredField(classDefine.getSuperclass(), sb);
		}
		if ((sb.length() > 0) && (sb.charAt(sb.length() - 1) == ',')) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb;
	}

	private <T> DBObject toDbObject(T objectToSave) {
		if (!(objectToSave instanceof String)) {
			DBObject dbDoc = new BasicDBObject();
			if ((objectToSave instanceof Map)) {
				Map<String, Object> map = (Map<String, Object>) objectToSave;
				Set<String> keys = map.keySet();
				String key;
				for (Iterator<String> i$ = keys.iterator(); i$.hasNext(); dbDoc.put(key, map.get(key))) {
					key = i$.next();
				}
			} else {
				if ((objectToSave instanceof Update)) {
					Update update = (Update) objectToSave;
					return update.getUpdateObject();
				}
				Class<?> clz = objectToSave.getClass();
				StringBuffer sb = new StringBuffer();
				fetchAllDeclaredField(clz, sb);
				String[] flds = sb.toString().split(",");
				for (String s : flds) {
					clz = objectToSave.getClass();
					String getMethodName = "get" + s.substring(0, 1).toUpperCase() + s.substring(1);
					Method getMethod = null;
					java.lang.reflect.Field getFld = null;
					while ((getMethod == null) && (clz != null)) {
						try {
							if (getFld == null) {
								getFld = clz.getDeclaredField(s);
							}
							getMethod = clz.getDeclaredMethod(getMethodName, new Class[0]);
						} catch (Exception e) {
							clz = clz.getSuperclass();
						}
					}
					if (getMethod != null) {
						try {
							Object val = getMethod.invoke(objectToSave, new Object[0]);
							if (val != null) {
								dbDoc.put(s, val);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						try {
							getFld.setAccessible(true);
							Object val = getFld.get(objectToSave);
							if (val != null) {
								dbDoc.put(s, val);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
			return dbDoc;
		}
		try {
			return (DBObject) JSON.parse((String) objectToSave);
		} catch (JSONParseException e) {
			throw new MappingException("Could not parse given String to save into a JSON document!", e);
		}
	}

	public <T> T toJavaObject(Object obj, Class<T> entityClass) {
		return TypeUtils.cast(obj, entityClass, ParserConfig.getGlobalInstance());
	}

	public <T> List<T> toJavaObjectList(List<?> objlist, Class<T> entityClass) {
		List<T> list = new ArrayList<T>();
		for (Object obj : objlist) {
			list.add(toJavaObject(obj, entityClass));
		}
		return list;
	}

	public QueryExtractor.DBTYPE getDBType() {
		String type = "";
		DataSource dataSource = this.sqlTemplate.getConfiguration().getEnvironment().getDataSource();
		if ((dataSource instanceof DruidDataSource)) {
			type = ((String) ReflectUtil.getFieldValue(dataSource, "jdbcUrl")).toLowerCase();
		} else {
			type = ((String) ReflectUtil.getFieldValue(dataSource, "url")).toLowerCase();
		}
		int first = type.indexOf(":");
		type = type.substring(first + 1, type.indexOf(":", first + 1));
		if ("mysql".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.MYSQL;
		}
		if ("oracle".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.ORACLE;
		}
		if ("sqlserver".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.SQLSERVER;
		}
		if ("postgresql".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.POSTGRESQL;
		}
		if ("db2".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.DB2;
		}
		if ("h2".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.H2;
		}
		if ("derby".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.DERBY;
		}
		if ("sqlite".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.SQLITE;
		}
		if ("firebirdsql".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.FIREBIRD;
		}
		if ("sysbase".equalsIgnoreCase(type)) {
			return QueryExtractor.DBTYPE.SYSBASE;
		}
		return QueryExtractor.DBTYPE.OTHERSQL;
	}

	private String convertChartsetSQL(String sql) {
		if (StringUtils.isEmpty(this.chartset)) {
			return sql;
		}
		try {
			return new String(sql.getBytes("GBK"), this.chartset);
		} catch (Exception ex) {
		}
		return sql;
	}

	public String decodeChartsetString(String sql) {
		if (StringUtils.isEmpty(this.chartset)) {
			return sql;
		}
		try {
			return new String(sql.getBytes(this.chartset), "GBK");
		} catch (Exception ex) {
		}
		return sql;
	}
}
