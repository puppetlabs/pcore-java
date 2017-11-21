package com.puppet.pcore.impl.eval;

import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.all;
import static com.puppet.pcore.impl.Helpers.allWithIndex;

public class CompareOperator {
	public static boolean match(Object a, Object b, Scope scope) {
		if(b instanceof List<?>)
			return _match((List<?>)b, a, scope);

		if(b instanceof Map<?,?>)
			return _match((Map<?,?>)b, a, scope);

		if(b instanceof AnyType)
			return ((AnyType)b).isInstance(a);

		if(b instanceof Pattern)
			return _match((Pattern)b, a, scope);

		if(b instanceof Version)
			return _match((Version)b, a, scope);

		if(b instanceof VersionRange)
			return _match((VersionRange)b, a, scope);

		return equals(a, b);
	}

	public static boolean equals(Object a, Object b) {
		if(a instanceof String)
			return _equals((String)a, b);

		if(a instanceof Number)
			return _equals((Number)a, b);

		if(a instanceof List<?>)
			return _equals((List<?>)a, b);

		if(a instanceof Map<?,?>)
			return _equals((List<?>)a, b);

		return Objects.equals(a, b);
	}

	private static boolean _equals(List<?> array, Object val) {
		if(!(val instanceof List<?>))
			return false;

		List<?> right = (List<?>)val;
		return array.size() == right.size() && allWithIndex(array, (e, i) -> equals(e, right.get(i)));
	}

	private static boolean _equals(Map<?,?> hash, Object val) {
		if(!(val instanceof Map<?,?>))
			return false;

		Map<?,?> right = (Map<?,?>)val;
		int top = hash.size();
		if(top != right.size())
			return false;

		return all(hash.entrySet(), (e) -> equals(e.getValue(), right.get(e.getKey())));
	}

	private static boolean _equals(Number number, Object val) {
		if(val instanceof Float || val instanceof Double)
			return number.doubleValue() == ((Number)val).doubleValue();

		if(val instanceof Number) {
			if(number instanceof Float || number instanceof Double)
				return number.doubleValue() == ((Number)val).doubleValue();

			return number.longValue() == ((Number)val).longValue();
		}
		return false;
	}

	private static boolean _equals(String str, Object val) {
		return val instanceof String && str.equalsIgnoreCase((String)val);
	}

	private static boolean _match(List<?> array, Object val, Scope scope) {
		if(!(val instanceof List<?>))
			return false;

		List<?> left = (List<?>)val;
		return array.size() == left.size() && allWithIndex(array, (e, i) -> match(left.get(i), e, scope));
	}

	private static boolean _match(Map<?,?> hash, Object val, Scope scope) {
		if(!(val instanceof Map<?,?>))
			return false;

		Map<?,?> left = (Map<?,?>)val;
		return all(hash.entrySet(), (e) -> match(left.get(e.getKey()), e.getValue(), scope));
	}

	private static boolean _match(Pattern pattern, Object val, Scope scope) {
		if(!(val instanceof String))
			return false;

		Matcher m = pattern.matcher((String)val);
		if(m.find()) {
			if(scope != null)
				scope.setMatchData(m);
			return true;
		}
		return false;
	}

	private static boolean _match(Version version, Object val, Scope scope) {
		if(val instanceof Version) {
			return version.equals(val);
		}
		if(val instanceof String)
			try {
				return version.equals(Version.create((String)val));
			} catch(IllegalArgumentException e) {
			}
		return false;
	}

	private static boolean _match(VersionRange range, Object val, Scope scope) {
		if(val instanceof Version) {
			return range.includes((Version)val);
		}
		if(val instanceof String)
			try {
				return range.includes(Version.create((String)val));
			} catch(IllegalArgumentException e) {
			}
		return false;
	}
}
