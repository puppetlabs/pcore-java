package com.puppet.pcore.impl.types;

import com.puppet.pcore.regex.Regexp;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class RegexpType extends ScalarType {
	public static final String DEFAULT_PATTERN = ".*";
	static final RegexpType DEFAULT = new RegexpType(DEFAULT_PATTERN);

	private static ObjectType ptype;
	public final Regexp pattern;

	RegexpType(Regexp pattern) {
		this.pattern = pattern;
	}

	RegexpType(String patternString) {
		this.pattern = Regexp.compile(patternString);
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
		return super.hashCode() * 31 + pattern.hashCode();
	}

	public boolean matches(String value) {
		return pattern().matcher(value).find();
	}

	public Regexp pattern() {
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
								KEY_TYPE, variantType(runtimeType("java", "com.puppet.pcore.regex.Regexp"), stringType()),
								KEY_VALUE, DEFAULT_PATTERN)));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, regexpTypeDispatcher(),
				(self) -> new Object[]{self.pattern.toString()});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o instanceof RegexpType && pattern.equals(((RegexpType)o).pattern);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Regexp && (pattern.toString().equals(DEFAULT_PATTERN) || pattern.equals(o));
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof RegexpType && (pattern.toString().equals(DEFAULT_PATTERN) || pattern.equals(((RegexpType)t).pattern));
	}
}
