package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class RegexpType extends ScalarType {
	public static final String DEFAULT_PATTERN = ".*";
	public static final RegexpType DEFAULT = new RegexpType(DEFAULT_PATTERN);

	private static ObjectType ptype;
	public final String patternString;
	private Pattern pattern;

	RegexpType(Pattern pattern) {
		this.patternString = pattern.toString();
		this.pattern = pattern;
	}

	RegexpType(String patternString) {
		this.patternString = patternString;
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		return o instanceof RegexpType && patternString.equals(((RegexpType)o).patternString);
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

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(RegexpType.class, "Pcore::RegexpType", "Pcore::ScalarType",
				asMap(
						"pattern", asMap(
								KEY_TYPE, variantType(runtimeType("java", "java.util.regex.Pattern"), stringType()),
								KEY_VALUE, DEFAULT_PATTERN)),
				(args) -> regexpType((String)args.get(0)),
				(self) -> new Object[]{self.patternString});
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof RegexpType && (patternString.equals(DEFAULT_PATTERN) || patternString.equals(((RegexpType)t)
				.patternString));
	}
}
