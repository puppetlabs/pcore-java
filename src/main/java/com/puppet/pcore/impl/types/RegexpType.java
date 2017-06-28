package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.regex.Pattern.*;

public class RegexpType extends ScalarType {
	public static final String DEFAULT_PATTERN = ".*";
	static final RegexpType DEFAULT = new RegexpType(DEFAULT_PATTERN);

	private static ObjectType ptype;
	public final String patternString;
	private Pattern pattern;

	public static String patternWithFlagsExpanded(Pattern pattern) {
		String patternString = pattern.toString();
		int flags = pattern.flags();
		if((flags & (CASE_INSENSITIVE|UNIX_LINES|MULTILINE|DOTALL|UNICODE_CASE|COMMENTS|UNICODE_CHARACTER_CLASS)) != 0) {
			StringBuilder bld = new StringBuilder();
			bld.append("(?");
			if((flags & CASE_INSENSITIVE) != 0)
				bld.append('i');
			if((flags & UNIX_LINES) != 0)
				bld.append('d');
			if((flags & MULTILINE) != 0)
				bld.append('m');
			if((flags & DOTALL) != 0)
				bld.append('s');
			if((flags & UNICODE_CASE) != 0)
				bld.append('u');
			if((flags & COMMENTS) != 0)
				bld.append('x');
			if((flags & UNICODE_CHARACTER_CLASS) != 0)
				bld.append('U');
			bld.append(':');
			bld.append(patternString);
			bld.append(')');
			patternString = bld.toString();
		}
		return patternString;
	}

	RegexpType(Pattern pattern) {
		this.patternString = patternWithFlagsExpanded(pattern);
		this.pattern = pattern;
	}

	RegexpType(String patternString) {
		this.patternString = patternString;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return super.hashCode() * 31 + patternString.hashCode();
	}

	public boolean matches(String value) {
		return pattern().matcher(value).matches();
	}

	public Pattern pattern() {
		if(pattern == null)
			pattern = Pattern.compile(patternString);
		return pattern;
	}

	@Override
	public boolean roundtripWithString() {
		return true;
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::RegexpType", "Pcore::ScalarType",
				asMap(
						"pattern", asMap(
								KEY_TYPE, variantType(runtimeType("java", "java.util.regex.Pattern"), stringType()),
								KEY_VALUE, DEFAULT_PATTERN)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, regexpTypeDispatcher(),
				(self) -> new Object[]{self.patternString});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof RegexpType && patternString.equals(((RegexpType)o).patternString);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Pattern && (patternString.equals(DEFAULT_PATTERN) || patternWithFlagsExpanded((Pattern)o).equals(patternString));
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof RegexpType && (patternString.equals(DEFAULT_PATTERN) || patternString.equals(((RegexpType)t).patternString));
	}
}
