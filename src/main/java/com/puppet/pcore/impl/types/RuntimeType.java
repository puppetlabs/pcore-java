package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;

public class RuntimeType extends AnyType {
	public static final RuntimeType DEFAULT = new RuntimeType(null, null, null);
	private static final String JAVA_KEY = "java";
	public static final RuntimeType JAVA = new RuntimeType(JAVA_KEY, null, null);

	private static ObjectType ptype;
	public final String name;
	public final RegexpType pattern;
	public final String runtime;

	RuntimeType(String runtime, String name, RegexpType pattern) {
		this.runtime = runtime;
		this.name = name;
		this.pattern = pattern;
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof RuntimeType) {
			RuntimeType rt = (RuntimeType)o;
			return Objects.equals(runtime, rt.runtime) && Objects.equals(name, rt.name) && Objects.equals(pattern, rt
					.pattern);
		}
		return false;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return (Objects.hashCode(runtime) * 31 + Objects.hashCode(name)) * 31 + Objects.hashCode(pattern);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(RuntimeType.class, "Pcore::RuntimeType", "Pcore::AnyType",
				asMap(
						"runtime", asMap(
								KEY_TYPE, optionalType(StringType.NOT_EMPTY),
								KEY_VALUE, null),
						"name_or_pattern", asMap(
								KEY_TYPE, optionalType(variantType(StringType.NOT_EMPTY, tupleType(asList(regexpType(), StringType
										.NOT_EMPTY)))),
								KEY_VALUE, null)),
				(args) -> {
					String runtime = (String)args.get(0);
					Object nameOrPattern = args.get(1);
					if(nameOrPattern instanceof String)
						return runtimeType(runtime, (String)nameOrPattern);
					List<?> npList = (List<?>)nameOrPattern;
					return runtimeType(runtime, (String)npList.get(1), (RegexpType)npList.get(0));
				},
				(self) -> new Object[]{self.runtime, self.pattern == null ? self.name : asList(self.pattern, self.name)});
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return isIterable(guard) ? new IterableType(this) : null;
	}

	@Override
	boolean isInstance(Object v, RecursionGuard guard) {
		if(v == null || !JAVA_KEY.equals(runtime) || name == null || pattern != null)
			return false;
		if(name.equals(v.getClass().getName()))
			return true;
		Class<?> cls = loadClass();
		return cls != null && cls.isInstance(v);
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		Class<?> c = loadClass();
		return c != null && Iterable.class.isAssignableFrom(c);
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(!(t instanceof RuntimeType))
			return false;
		if(runtime == null)
			return true;
		RuntimeType rt = (RuntimeType)t;
		if(!runtime.equals(rt.runtime))
			return false;
		if(name == null)
			return true;
		if(pattern != null)
			return name.equals(rt.name) && pattern.equals(rt.pattern);

		// Compute assignability of loaded classes.
		Class<?> c1 = loadClass();
		if(c1 != null) {
			Class<?> c2 = rt.loadClass();
			return c2 != null && c1.isAssignableFrom(c2);
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		RuntimeType rt = (RuntimeType)other;
		if(Objects.equals(runtime, rt.runtime)) {
			if(pattern == null && rt.pattern == null && name != null && rt.name != null) {
				Class<?> c1 = loadClass();
				if(c1 != null) {
					Class<?> c2 = rt.loadClass();
					if(c2 != null) {
						for(; !c1.equals(Object.class); c1 = c1.getSuperclass()) {
							for(Class<?> s2 = c2; !s2.equals(Object.class); s2 = s2.getSuperclass()) {
								if(c1.isAssignableFrom(s2))
									return runtimeType(runtime, c1.getName());
							}
						}
					}
				}
				return runtimeType(runtime, "java.lang.Object");
			}
			return runtimeType(runtime, null);
		}
		return runtimeType();
	}

	private Class<?> loadClass() {
		if(JAVA_KEY.equals(runtime) && pattern == null && name != null) {
			try {
				return getClass().getClassLoader().loadClass(name);
			} catch(ClassNotFoundException e) {
				// Ignored
			}
		}
		return null;
	}
}
