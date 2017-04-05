package org.elnino.demo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class HashMapCase<K, V> extends HashMap<K, V> implements Map<K, V>, Cloneable, Serializable {
	private static final long serialVersionUID = 362498820763181265L;

	public HashMapCase(int paramInt, float paramFloat) {
		super(paramInt, paramFloat);
	}

	public HashMapCase(int paramInt) {
		super(paramInt);
	}

	public HashMapCase() {
	}

	public HashMapCase(Map<? extends K, ? extends V> paramMap) {
		super(paramMap);
	}

	public V get(Object paramObject) {
		return super.get(paramObject.toString().toLowerCase());
	}

	public boolean containsKey(Object paramObject) {
		return super.containsKey(paramObject.toString().toLowerCase());
	}

	@SuppressWarnings("unchecked")
	public V put(K paramK, V paramV) {
		if ((paramV instanceof BigDecimal)) {
			return super.put((K) paramK.toString().toLowerCase(), (V) ((BigDecimal) paramV).toString());
		}
		return super.put((K) paramK.toString().toLowerCase(), paramV);
	}

	public V remove(Object paramObject) {
		return super.remove(paramObject.toString().toLowerCase());
	}
}
