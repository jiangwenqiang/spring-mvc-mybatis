package org.elnino.demo.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.Map;

import org.springframework.util.StringUtils;

public class CommonUtils {

	public String fetchAnnotationTableName() {
		return fetchAnnotationTableName(this.getClass());
	}

	public static String fetchAnnotationTableName(Class<?> cl) {
		Annotation[] annos = cl.getAnnotations();
		for (Annotation ann : annos) {
			if (ann instanceof org.springframework.data.mongodb.core.mapping.Document) {
				return ((org.springframework.data.mongodb.core.mapping.Document) ann).collection();
			}
		}

		String name = cl.getName();
		name = name.substring(name.lastIndexOf(".") + 1).toLowerCase();
		return name.endsWith("bean") ? name.substring(0, name.length() - 4) : name;
	}

	public java.lang.reflect.Field fetchDeclaredField(String fieldName) {
		return fetchDeclaredField(this.getClass(), fieldName);
	}

	public static java.lang.reflect.Field fetchDeclaredField(Class<?> classDefine, String fieldName) {
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

	public StringBuffer fetchAllDeclaredField(StringBuffer sb) {
		return fetchAllDeclaredField(this.getClass(), sb);
	}

	public static StringBuffer fetchAllDeclaredField(Class<?> classDefine, StringBuffer sb) {
		java.lang.reflect.Field[] flds = classDefine.getDeclaredFields();

		for (java.lang.reflect.Field fld : flds) {
			// 静态成员
			if (Modifier.isStatic(fld.getModifiers()))
				continue;
			String fldname = fld.getName();
			sb.append(fldname + ",");
		}
		if (classDefine.getSuperclass() != null)
			fetchAllDeclaredField(classDefine.getSuperclass(), sb);

		if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',')
			sb.deleteCharAt(sb.length() - 1);
		return sb;
	}

	public static Map<String, String> fetchSelectField(Class<?> classDefine, Map<String, String> fldmap) {
		java.lang.reflect.Field[] flds = classDefine.getDeclaredFields();

		for (java.lang.reflect.Field fld : flds) {
			// 静态成员
			if (Modifier.isStatic(fld.getModifiers()))
				continue;
			String fldname = fld.getName();
			String fldvalue = fldname;

			boolean istransient = false;
			Annotation[] annos = fld.getAnnotations();
			for (Annotation ann : annos) {
				if (ann instanceof org.springframework.data.annotation.Transient)
					istransient = true;

				// 指定字段
				if (ann instanceof org.springframework.data.mongodb.core.mapping.Field) {
					fldvalue = ((org.springframework.data.mongodb.core.mapping.Field) ann).value();
					if (!StringUtils.isEmpty(fldvalue)) {
						fldvalue = fldvalue + " as " + fldname;
						istransient = false;
					} else
						fldvalue = fldname;
				}
			}
			if (istransient)
				continue;

			fldmap.put(fldname, fldvalue);
		}
		if (classDefine.getSuperclass() != null)
			fetchSelectField(classDefine.getSuperclass(), fldmap);

		return fldmap;
	}
}
