package com.puppet.pcore.impl;

import java.util.Map;

import static com.puppet.pcore.impl.LabelProvider.article;
import static java.lang.String.format;

public class Options {
	private static IllegalArgumentException badValue(String key, Object value, String type) {
		return new IllegalArgumentException(
				format("Expected %s value for option '%s', got %s", article(type), key, value == null ? "null" : value.getClass().getName()));
	}

	private static <T extends Object, D extends T> T assertOption(Map<String, Object> options, String key, D defaultValue, Class<T> optionClass, String type) {
		Object value = options.get(key);
		if(value == null) {
			if(!options.containsKey(key))
				return defaultValue;
		} else {
			try {
				return optionClass.cast(value);
			} catch(ClassCastException e) {
			}
		}
		throw badValue(key, value, type);
	}

	public static boolean get(Map<String, Object> options, String key, boolean defaultValue) {
		return assertOption(options, key, defaultValue, Boolean.class, "boolean");
	}

	public static String get(Map<String, Object> options, String key, String defaultValue) {
		return assertOption(options, key, defaultValue, String.class, "string");
	}

	public static int get(Map<String, Object> options, String key, int defaultValue) {
		Number v = assertOption(options, key, defaultValue, Number.class, "integer");
		if(v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte)
			return v.intValue();
		throw badValue(key, v, "integer");
	}

	public static long get(Map<String, Object> options, String key, long defaultValue) {
		Number v = assertOption(options, key, defaultValue, Number.class, "integer");
		if(v instanceof Long || v instanceof Integer || v instanceof Short || v instanceof Byte)
			return v.longValue();
		throw badValue(key, v, "integer");
	}

	public static <T> T get(Map<String, Object> options, String key, Class<? extends T> valueClass) {
		Object value = options.get(key);
		if(value == null)
			throw new IllegalArgumentException(format("Expected a value for option '%s'", key));
		return valueClass.cast(value);
	}
}
