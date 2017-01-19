package com.puppet.pcore.impl;

import com.puppet.pcore.Default;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.TypeResolverException;
import com.puppet.pcore.impl.parser.*;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.time.DurationFormat;
import com.puppet.pcore.time.InstantFormat;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static com.puppet.pcore.Pcore.loader;
import static com.puppet.pcore.impl.Constants.KEY_NAME_AUTHORITY;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.Helpers.mapRange;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;

@SuppressWarnings("unused")
public class TypeEvaluatorImpl extends Polymorphic implements TypeEvaluator {

	public static final Long ZERO = 0L;
	public static final Map<String,AnyType> BASIC_TYPES;
	private static final DispatchMap dispatchMap = initPolymorphicDispatch(TypeEvaluatorImpl.class, "eval");

	static {
		Map<String,AnyType> coreTypes = new HashMap<>();
		coreTypes.put("any", anyType());
		coreTypes.put("array", arrayType());
		coreTypes.put("binary", binaryType());
		coreTypes.put("boolean", booleanType());
		coreTypes.put("callable", allCallableType());
		coreTypes.put("catalogentry", catalogEntryType());
		coreTypes.put("class", classType());
		coreTypes.put("collection", collectionType());
		coreTypes.put("data", dataType());
		coreTypes.put("default", defaultType());
		coreTypes.put("enum", enumType());
		coreTypes.put("float", floatType());
		coreTypes.put("hash", hashType());
		coreTypes.put("integer", integerType());
		coreTypes.put("iterable", iterableType());
		coreTypes.put("iterator", iteratorType());
		coreTypes.put("notundef", notUndefType());
		coreTypes.put("numeric", numericType());
		coreTypes.put("object", objectType());
		coreTypes.put("optional", optionalType());
		coreTypes.put("pattern", patternType());
		coreTypes.put("regexp", regexpType());
		coreTypes.put("resource", resourceType());
		coreTypes.put("runtime", runtimeType());
		coreTypes.put("scalar", scalarType());
		coreTypes.put("scalardata", scalarDataType());
		coreTypes.put("semver", semVerType());
		coreTypes.put("semverrange", semVerRangeType());
		coreTypes.put("sensitive", sensitiveType());
		coreTypes.put("string", stringType());
		coreTypes.put("struct", structType());
		coreTypes.put("timespan", timeSpanType());
		coreTypes.put("timestamp", timestampType());
		coreTypes.put("tuple", tupleType());
		coreTypes.put("typealias", typeAliasType());
		coreTypes.put("typereference", typeReferenceType());
		coreTypes.put("typeset", typeSetType());
		coreTypes.put("type", typeType());
		coreTypes.put("undef", undefType());
		coreTypes.put("unit", unitType());
		coreTypes.put("variant", variantType());
		BASIC_TYPES = Collections.unmodifiableMap(coreTypes);
	}

	public static int assertParameterCount(int min, int max, int actual, String name) {
		if(actual < min || actual > max) {
			String rq;
			if(min == max)
				rq = Integer.toString(min);
			else if(max < Integer.MAX_VALUE)
				rq = format("%d to %d", min, max);
			else
				rq = format("at least %d", min);
			throw new TypeResolverException(
					format("Invalid number of type parameters specified: '%s' requires %s parameters, %d provided", name, rq, actual));
		}
		return actual;
	}

	public AnyType bindByName(String name, AnyType typeToBind, URI nameAuthority) {
		Loader loader = loader();
		TypedName typedName = new TypedName("type", name, nameAuthority);
		AnyType type = (AnyType)loader.loadOrNull(typedName);
		if(type != null && type.equals(typeToBind))
			return type;

		loader.bind(typedName, typeToBind);
		return typeToBind;
	}

	@Override
	public AnyType declareType(String name, String typeExpression) {
		return declareType(name, parse(typeExpression), null);
	}

	@Override
	public AnyType declareType(String name, String typeExpression, URI nameAuthority) {
		return declareType(name, parse(typeExpression), nameAuthority);
	}

	@Override
	public AnyType declareType(String name, Expression expr, URI nameAuthority) {
		Loader loader = loader();
		AnyType createdType = null;
		if(expr instanceof AccessExpression) {
			AccessExpression ae = (AccessExpression)expr;
			Expression receiver = ae.expr;
			if(receiver instanceof TypeNameExpression) {
				TypeNameExpression te = (TypeNameExpression)receiver;
				switch(te.downcasedName()) {
				case "object":
					assertParameterCount(1, 1, ae.elements, te.name);
					createdType = objectType(name, ae.elements.get(0));
					break;
				case "typeset":
					assertParameterCount(1, 1, ae.elements, te.name);
					HashExpression ha = (HashExpression)ae.elements.get(0);
					if(nameAuthority == null) {
						Expression nsExpr = ha.getValue(KEY_NAME_AUTHORITY);
						if(nsExpr instanceof StringExpression)
							nameAuthority = URI.create(((StringExpression)nsExpr).getString());
						else
							nameAuthority = loader.getNameAuthority();
					}
					createdType = typeSetType(name, nameAuthority, ha);
				}
			}
		}
		if(createdType == null)
			createdType = typeAliasType(name, expr);

		return bindByName(name, createdType, nameAuthority == null ? loader.getNameAuthority() : nameAuthority);
	}

	@Override
	public Object resolve(String typeString) {
		return resolve(parse(typeString));
	}

	@Override
	public Object resolve(Expression expression) {
		try {
			return dispatch(expression);
		} catch(InvocationTargetException e) {
			Throwable te = e.getCause();
			if(!(te instanceof RuntimeException))
				te = new RuntimeException(te);
			throw (RuntimeException)te;
		}
	}

	@Override
	public AnyType resolveType(String typeString) {
		return resolveType(parse(typeString));
	}

	@Override
	public AnyType resolveType(Expression expression) {
		Object t = resolve(expression);

		if(t instanceof AnyType)
			return (AnyType)t;

		throw new TypeResolverException(format("'%s' did not resolve to a Pcore type", expression));
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	Object eval(ConstantExpression ce) {
		return ce.value;
	}

	Object eval(HashExpression ce) {
		Map<Object,Object> result = new LinkedHashMap<>();
		List<Object> args = map(ce.elements, this::resolve);
		int top = args.size();
		for(int idx = 0; idx < top; ) {
			Object key = args.get(idx++);
			result.put(key, args.get(idx++));
		}
		return result;
	}

	Object eval(ArrayExpression ce) {
		return map(ce.elements, this::resolve);
	}

	Object eval(AssignmentExpression ce) {
		if(!(ce.lhs instanceof TypeNameExpression))
			throw new TypeResolverException("LHS of assignment expression must be a Type name");

		return declareType(((TypeNameExpression)ce.lhs).name, ce.rhs, loader().getNameAuthority()).resolve();
	}

	Object eval(RegexpExpression ce) {
		return regexpType((String)ce.value);
	}

	Object eval(AccessExpression ae) {
		if(!(ae.expr instanceof TypeNameExpression))
			throw new TypeResolverException("LHS of [] expression must be a Type name");
		TypeNameExpression te = (TypeNameExpression)ae.expr;

		String dcName = te.downcasedName();
		if("object".equals(dcName) || "typeset".equals(dcName)) {
			assertParameterCount(1, 1, ae.elements.size(), te.name);
			HashExpression i12nExpr = assertOneParam(HashExpression.class, ae.elements.get(0), 0, te.name);
			return "object".equals(dcName) ? objectType(null, i12nExpr) : typeSetType(null, loader().getNameAuthority(), i12nExpr);
		}

		Object[] args = map(ae.elements, this::resolve).toArray();
		switch(dcName) {
		case "array":
			switch(assertParameterCount(1, 3, args, te.name)) {
			case 1:
				return arrayType(assertType(args, 0, te.name));
			case 2:
				if(args[0] instanceof AnyType) {
					if(args[1] instanceof IntegerType)
						return arrayType((AnyType)args[0], (IntegerType)args[1]);
					return arrayType((AnyType)args[0], integerType(assertRangeMin(args, 1, te.name)));
				}
				return arrayType(integerType(assertRangeMin(args, 0, te.name), assertMax(args, 1, te.name)));
			default:
				return arrayType(assertType(args, 0, te.name), integerType(assertRangeMin(args, 1, te.name), assertMax
						(args, 2, te.name)));
			}
		case "callable": {
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			if(args.length == 2 && Objects.equals(args[0], ZERO) && Objects.equals(args[1], ZERO))
				return callableType(TupleType.EXPLICIT_EMPTY);
			Object first = args[0];
			Object[] params = args;
			AnyType returnType = anyType();
			CallableType blockType = null;
			if(first instanceof List) {
				assertParameterCount(2, 2, args, te.name);
				returnType = assertType(args, 1, te.name);
				params = ((List<?>)first).toArray();
			}
			if(args.length > 0) {
				Object last = args[args.length - 1];
				if(last instanceof CallableType) {
					blockType = (CallableType)last;
					params = Arrays.copyOf(args, args.length - 1);
				}
			}
			return params.length == 2 && ZERO.equals(params[0]) && ZERO.equals(params[1])
					? callableType(tupleType(Collections.emptyList()), blockType, returnType)
					: callableType(tupleType(assertTypes(params, te.name)), blockType, returnType);
		}
		case "class":
			assertParameterCount(1, 1, args, te.name);
			return classType(assertClass(String.class, args, 0, te.name));
		case "collection":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				return collectionType(args[0] instanceof IntegerType
						? (IntegerType)args[0]
						: integerType(assertRangeMin(args, 0, te.name)));
			default:
				return collectionType(integerType(assertRangeMin(args, 0, te.name), assertMax(args, 1, te.name)));
			}
		case "enum":
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			return enumType(mapRange(0, args.length, paramNo -> assertClass(String.class, args, paramNo, te.name)));
		case "float":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				return floatType(assertFloatMin(args, 0, te.name));
			default:
				return floatType(assertFloatMin(args, 0, te.name), assertFloatMax(args, 1, te.name));
			}
		case "hash":
			switch(assertParameterCount(2, 4, args, te.name)) {
			case 2:
				if(args[0] instanceof AnyType && args[1] instanceof AnyType)
					return hashType((AnyType)args[0], (AnyType)args[1]);
				return hashType(unitType(), unitType(), integerType(assertRangeMin(args, 0, te.name), assertMax(args, 1,
						te.name)));
			case 3:
				IntegerType sizeType = args[2] instanceof IntegerType
						? (IntegerType)args[2]
						: integerType(assertRangeMin(args, 2, te.name));
				return hashType(assertType(args, 0, te.name), assertType(args, 1, te.name), sizeType);
			default:
				return hashType(assertType(args, 0, te.name), assertType(args, 1, te.name), integerType(assertRangeMin
						(args, 2, te.name), assertMax(args, 3, te.name)));
			}
		case "integer":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				return integerType(assertMin(args, 0, te.name));
			default:
				return integerType(assertMin(args, 0, te.name), assertMax(args, 1, te.name));
			}
		case "iterable":
			assertParameterCount(1, 1, args, te.name);
			return iterableType(assertType(args, 0, te.name));
		case "iterator":
			assertParameterCount(1, 1, args, te.name);
			return iteratorType(assertType(args, 0, te.name));
		case "notundef":
			assertParameterCount(1, 1, args, te.name);
			return notUndefType(assertTypeOrString(args, 0, te.name));
		case "optional":
			assertParameterCount(1, 1, args, te.name);
			return optionalType(assertTypeOrString(args, 0, te.name));
		case "pattern":
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			return patternType(mapRange(0, args.length, paramNo -> assertClass(RegexpType.class, args, paramNo, te
					.name)));
		case "regexp":
			assertParameterCount(1, 1, args, te.name);
			return args[0] instanceof RegexpType ? args[0] : regexpType(assertClass(String.class, args, 0, te.name));
		case "resource": {
			assertParameterCount(1, 2, args, te.name);
			String title = null;
			String name;
			if(args[0] instanceof TypeReferenceType) {
				assertParameterCount(1, 1, args, te.name);
				String typeString = ((TypeReferenceType)args[0]).typeString;
				int paramStart = typeString.indexOf('[');
				if(paramStart < 0)
					name = typeString;
				else {
					Object[] tps = ((List<?>)resolve(typeString.substring(paramStart))).toArray();
					assertParameterCount(1, 1, tps, te.name);
					name = typeString.substring(0, paramStart);
					title = assertClass(String.class, tps, 0, te.name);
				}
			} else {
				name = assertClass(String.class, args, 0, te.name);
				if(args.length == 2)
					title = assertClass(String.class, args, 1, te.name);
			}
			return resourceType(name, title);
		}
		case "runtime":
			assertParameterCount(2, 2, args, te.name);
			return runtimeType(assertClass(String.class, args, 0, te.name), assertClass(String.class, args, 1, te.name));
		case "semver":
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			return semVerType(mapRange(0, args.length, paramNo -> VersionRange.create(assertClass(String.class,
					args, paramNo, te.name))));
		case "sensitive":
			assertParameterCount(1, 1, args, te.name);
			return sensitiveType(assertType(args, 0, te.name));
		case "string":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				if(args[0] instanceof IntegerType)
					return stringType((IntegerType)args[0]);
				else if(args[0] instanceof String)
					return stringType((String)args[0]);
				return stringType(integerType(assertRangeMin(args, 0, te.name)));
			default:
				return stringType(integerType(assertMin(args, 0, te.name), assertMax(args, 1, te.name)));
			}
		case "struct":
			assertParameterCount(1, 1, args, te.name);
			Map<?,?> members = assertClass(Map.class, args, 0, te.name);
			return structType(map(members.entrySet(), entry -> {
				Object key = entry.getKey();
				Object value = entry.getValue();
				if(!(value instanceof AnyType))
					throw new TypeResolverException(format(
							"Invalid parameter type specified: '%s' requires member value to be a Type, %s provided",
							te.name,
							valueClassName(value)));
				if(key instanceof String)
					return structElement((String)entry.getKey(), (AnyType)value);
				if(key instanceof AnyType)
					return structElement((AnyType)entry.getKey(), (AnyType)value);
				throw new TypeResolverException(format(
						"Invalid parameter type specified: '%s' requires member key to be a Type or a String, %s provided",
						te.name,
						valueClassName(value)));
			}));
		case "timespan":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				return timeSpanType(assertTimeSpanMin(args, 0, te.name));
			default:
				return timeSpanType(assertTimeSpanMin(args, 0, te.name), assertTimeSpanMax(args, 1, te.name));
			}
		case "timestamp":
			switch(assertParameterCount(1, 2, args, te.name)) {
			case 1:
				return timestampType(assertTimestampMin(args, 0, te.name));
			default:
				return timestampType(assertTimestampMin(args, 0, te.name), assertTimestampMax(args, 1, te.name));
			}
		case "tuple":
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			if(isRangeParameter(args[args.length - 1])) {
				if(args.length > 1 && isRangeParameter(args[args.length - 2]))
					return tupleType(assertTypes(Arrays.copyOf(args, args.length - 2), te.name), integerType(assertRangeMin
							(args, args.length - 2, te.name), assertMax(args, args.length - 1, te.name)));
				return tupleType(assertTypes(Arrays.copyOf(args, args.length - 1), te.name), integerType(assertRangeMin
						(args, args.length - 1, te.name)));
			}
			return tupleType(assertTypes(args, te.name));
		case "typereference":
			assertParameterCount(1, 1, args, te.name);
			return typeReferenceType(assertClass(String.class, args, 0, te.name));
		case "type":
			assertParameterCount(1, 1, args, te.name);
			return typeType(assertType(args, 0, te.name));
		case "variant":
			assertParameterCount(1, Integer.MAX_VALUE, args, te.name);
			return variantType(assertTypes(args, te.name));
		case "any":
		case "binary":
		case "boolean":
		case "catalogentry":
		case "data":
		case "default":
		case "numeric":
		case "scalar":
		case "semverrange":
		case "typealias":
		case "undef":
		case "unit":
			throw new TypeResolverException(format("Not a parameterized type '%s'", te.name));
		default: {
			StringBuilder bld = new StringBuilder(te.name);
			new TypeFormatter(bld).format(asList(args));
			return typeReferenceType(bld.toString());
		}
		}
	}

	Object eval(IdentifierExpression te) {
		return te.name;
	}

	Object eval(TypeNameExpression te) {
		String dcName = te.downcasedName();
		AnyType type = BASIC_TYPES.get(dcName);
		if(type != null)
			return type;

		Loader loader = loader();
		TypedName typedName = new TypedName("type", te.name, loader.getNameAuthority());
		AnyType found = (AnyType)loader.loadOrNull(typedName);
		return found == null ? typeReferenceType(te.name) : found.resolve();
	}

	private <T> T assertClass(Class<T> cls, Object[] args, int paramNo, String name) {
		return assertOneParam(cls, args[paramNo], paramNo, name);
	}

	private double assertFloatMax(Object[] args, int paramNo, String name) {
		return assertFloatRange(args, paramNo, name, Double.POSITIVE_INFINITY);
	}

	private double assertFloatMin(Object[] args, int paramNo, String name) {
		return assertFloatRange(args, paramNo, name, Double.NEGATIVE_INFINITY);
	}

	private double assertFloatRange(Object[] args, int paramNo, String name, double dflt) {
		Object val = args[paramNo];
		if(val instanceof Default)
			return dflt;
		if(val instanceof Number)
			return ((Number)val).doubleValue();
		throw new TypeResolverException(format(
				"Invalid parameter type specified: '%s' requires parameter %d to be a Float or Integer, %s provided",
				name,
				paramNo,
				valueClassName(val)));
	}

	private <T> T assertInstance(Class<T> cls, Object value, String name) {
		if(!cls.isInstance(value))
			throw new TypeResolverException(format(
					"Invalid parameter type specified: '%s' requires member value to be a Type, %s provided",
					name,
					valueClassName(value)));
		return cls.cast(value);
	}

	private long assertMax(Object[] args, int paramNo, String name) {
		return assertRange(args, paramNo, name, Long.MAX_VALUE);
	}

	private long assertMin(Object[] args, int paramNo, String name) {
		return assertRange(args, paramNo, name, Long.MIN_VALUE);
	}

	private <T> T assertOneParam(Class<T> cls, Object val, int paramNo, String name) {
		if(!(cls.isInstance(val)))
			throw new TypeResolverException(format(
					"Invalid parameter type specified: '%s' requires parameter %d to be a '%s', %s provided",
					name,
					paramNo,
					cls.getSimpleName(),
					valueClassName(val)));
		return cls.cast(val);
	}

	private int assertParameterCount(int min, int max, Object[] args, String name) {
		return assertParameterCount(min, max, args.length, name);
	}

	private int assertParameterCount(int min, int max, List<?> args, String name) {
		return assertParameterCount(min, max, args.size(), name);
	}

	private long assertRange(Object[] args, int paramNo, String name, long dflt) {
		Object val = args[paramNo];
		if(val instanceof Default)
			return dflt;
		if(val instanceof Long)
			return (Long)val;
		throw new TypeResolverException(
				format("Invalid parameter type specified: '%s' requires parameter %d to be an Integer, %s provided",
						name, paramNo, valueClassName(val)));
	}

	private long assertRangeMin(Object[] args, int paramNo, String name) {
		return assertRange(args, paramNo, name, 0);
	}

	private Duration assertTimeSpanMax(Object[] args, int paramNo, String name) {
		return assertTimeSpanRange(args, paramNo, name, TimeSpanType.MAX_DURATION);
	}

	private Duration assertTimeSpanMin(Object[] args, int paramNo, String name) {
		return assertTimeSpanRange(args, paramNo, name, TimeSpanType.MIN_DURATION);
	}

	private Duration assertTimeSpanRange(Object[] args, int paramNo, String name, Duration dflt) {
		Object val = args[paramNo];
		if(val instanceof Default)
			return dflt;
		if(val instanceof String)
			return DurationFormat.defaultParse((String)val);
		throw new TypeResolverException(
				format("Invalid parameter type specified: '%s' requires parameter %d to be a String, %s provided",
						name, paramNo, valueClassName(val)));
	}

	private Instant assertTimestampMax(Object[] args, int paramNo, String name) {
		return assertTimestampRange(args, paramNo, name, Instant.MAX);
	}

	private Instant assertTimestampMin(Object[] args, int paramNo, String name) {
		return assertTimestampRange(args, paramNo, name, Instant.MIN);
	}

	private Instant assertTimestampRange(Object[] args, int paramNo, String name, Instant dflt) {
		Object val = args[paramNo];
		if(val instanceof Default)
			return dflt;
		if(val instanceof String)
			return InstantFormat.SINGLETON.parse((String)val);
		throw new TypeResolverException(
				format("Invalid parameter type specified: '%s' requires parameter %d to be a String, %s provided",
						name, paramNo, valueClassName(val)));
	}

	private AnyType assertType(Object[] args, int paramNo, String name) {
		return assertOneParam(AnyType.class, args[paramNo], paramNo, name);
	}

	private AnyType assertType(List<?> args, int paramNo, String name) {
		return assertOneParam(AnyType.class, args.get(paramNo), paramNo, name);
	}

	private AnyType assertTypeOrString(Object[] args, int paramNo, String name) {
		Object arg = args[paramNo];
		return arg instanceof String ? stringType((String)arg) : assertOneParam(AnyType.class, arg, paramNo, name);
	}

	private List<AnyType> assertTypes(Object[] args, String name) {
		return mapRange(0, args.length, paramNo -> assertType(args, paramNo, name));
	}

	private boolean isRangeParameter(Object val) {
		return val instanceof Default || val instanceof Long;
	}

	private Expression parse(String typeString) {
		return new DefaultExpressionParser(DefaultExpressionFactory.SINGLETON).parse(typeString);
	}

	private String valueClassName(Object value) {
		return value == null ? "Undef" : value.getClass().getSimpleName();
	}
}
