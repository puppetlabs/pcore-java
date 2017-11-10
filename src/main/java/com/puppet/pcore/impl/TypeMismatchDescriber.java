package com.puppet.pcore.impl;

import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.impl.types.*;

import java.util.*;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.LabelProvider.aOrAn;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;

@SuppressWarnings("unused")
public class TypeMismatchDescriber extends Polymorphic<List<? extends TypeMismatchDescriber.Mismatch>> {
	private static class PathElement {
		final String key;
		final PathType pathType;

		PathElement(PathType pathType, String key) {
			this.key = key;
			this.pathType = pathType;
		}

		@Override
		public boolean equals(Object o) {
			return o != null
					&& getClass().equals(o.getClass())
					&& pathType == ((PathElement)o).pathType
					&& Objects.equals(key, ((PathElement)o).key);
		}

		@Override
		public int hashCode() {
			return pathType.hashCode() * 31 + Objects.hashCode(key);
		}

		@Override
		public String toString() {
			return pathType.format(key);
		}
	}

	static abstract class Mismatch implements Cloneable {
		List<PathElement> path;
		private List<PathElement> canonicalPath;

		Mismatch(List<PathElement> path) {
			this.path = path == null ? emptyList() : path;
		}

		@Override
		public boolean equals(Object o) {
			return o != null && getClass().equals(o.getClass()) && canonicalPath().equals(((Mismatch)o).canonicalPath());
		}

		public String format() {
			List<PathElement> p = path;
			String variant = "";
			String position = "";
			if(!p.isEmpty()) {
				PathElement f = p.get(0);
				if(f.pathType == PathType.SIGNATURE) {
					variant = " " + f;
					p = new ArrayList<>(p);
					p.remove(0);
				}
				position = join(" ", map(p, PathElement::toString));
			}
			return message(variant, position);
		}

		@Override
		public int hashCode() {
			return canonicalPath().hashCode();
		}

		@Override
		public String toString() {
			return format();
		}

		List<PathElement> canonicalPath() {
			if(canonicalPath == null)
				canonicalPath = select(path, p -> p.pathType != PathType.VARIANT);
			return canonicalPath;
		}

		Mismatch copy() {
			try {
				Mismatch m = (Mismatch)this.clone();
				m.path = new ArrayList<>(path);
				return m;
			} catch(CloneNotSupportedException ignored) {
				return null;
			}
		}

		Mismatch merge(List<PathElement> path, Mismatch o) {
			Mismatch result = copy();
			result.path = path;
			return result;
		}

		String message(String variant, String position) {
			return variant.length() == 0 && position.length() == 0 ? text() : variant + position + ' ' + text();
		}

		String pathString() {
			return join(" ", map(path, PathElement::toString));
		}

		Mismatch removeElementAt(int elementIndex) {
			Mismatch result = copy();
			result.path.remove(elementIndex);
			return result;
		}

		abstract String text();
	}

	private static abstract class KeyMismatch extends Mismatch {
		final String key;

		KeyMismatch(List<PathElement> path, String key) {
			super(path);
			this.key = key;
		}

		@Override
		public boolean equals(Object o) {
			return super.equals(o) && key.equals(((KeyMismatch)o).key);
		}

		@Override
		public int hashCode() {
			return super.hashCode() * 31 + key.hashCode();
		}
	}

	private static class MissingKey extends KeyMismatch {
		MissingKey(List<PathElement> path, String key) {
			super(path, key);
		}

		@Override
		String text() {
			return "expects a value for key '" + key + '\'';
		}
	}

	private static class MissingParameter extends KeyMismatch {
		MissingParameter(List<PathElement> path, String key) {
			super(path, key);
		}

		@Override
		String text() {
			return "expects a value for parameter '" + key + '\'';
		}
	}

	private static class ExtraneousKey extends KeyMismatch {
		ExtraneousKey(List<PathElement> path, String key) {
			super(path, key);
		}

		@Override
		String text() {
			return "unrecognized key '" + key + '\'';
		}
	}

	private static class InvalidParameter extends KeyMismatch {
		InvalidParameter(List<PathElement> path, String key) {
			super(path, key);
		}

		@Override
		String text() {
			return "has no parameter named '" + key + '\'';
		}
	}

	private static class UnexpectedBlock extends Mismatch {
		UnexpectedBlock(List<PathElement> path) {
			super(path);
		}

		@Override
		String text() {
			return "does not expect a block";
		}
	}

	private static class MissingRequiredBlock extends Mismatch {
		MissingRequiredBlock(List<PathElement> path) {
			super(path);
		}

		@Override
		String text() {
			return "expects a block";
		}
	}

	private static class UnresolvedTypeReference extends KeyMismatch {
		UnresolvedTypeReference(List<PathElement> path, String unresolved) {
			super(path, unresolved);
		}

		@Override
		String text() {
			return "references an unresolved type '" + key + '\'';
		}
	}

	private static abstract class ExpectedActualMismatch extends Mismatch {
		final AnyType actual;
		AnyType expected;

		ExpectedActualMismatch(List<PathElement> path, AnyType expected, AnyType actual) {
			super(path);
			this.expected = expected;
			this.actual = actual;
		}

		ExpectedActualMismatch(List<PathElement> path, List<AnyType> expected, AnyType actual) {
			super(path);
			this.expected = variantType(expected).normalize();
			this.actual = actual.normalize();
		}

		@Override
		public boolean equals(Object o) {
			return super.equals(o)
					&& expected.equals(((ExpectedActualMismatch)o).expected)
					&& actual.equals(((ExpectedActualMismatch)o).actual);
		}

		@Override
		public int hashCode() {
			return (super.hashCode() * 31 + expected.hashCode()) * 31 + actual.hashCode();
		}
	}

	private static class TypeMismatch extends ExpectedActualMismatch {
		TypeMismatch(List<PathElement> path, AnyType expected, AnyType actual) {
			super(path, expected, actual);
		}

		TypeMismatch(List<PathElement> path, List<AnyType> expected, AnyType actual) {
			super(path, expected, actual);
		}

		@Override
		Mismatch merge(List<PathElement> path, Mismatch o) {
			TypeMismatch cp = (TypeMismatch)super.merge(path, o);
			cp.expected = variantType(expected, ((TypeMismatch)o).expected);
			return cp;
		}

		@Override
		String text() {
			AnyType e = expected;
			AnyType a = actual;
			boolean multi = false;
			boolean optional = false;
			if(e instanceof OptionalType) {
				e = ((OptionalType)e).actualType();
				optional = true;
			}
			String as;
			String es;
			if(e instanceof VariantType) {
				List<AnyType> el = ((VariantType)e).types;
				List<String> els;
				if(reportDetailed(el, a)) {
					as = detailedActualToS(el, a);
					els = map(el, AnyType::toExpandedString);
				} else {
					els = distinct(map(el, AnyType::name));
					as = a.name();
				}
				if(optional)
					els.add(0, "Undef");
				switch(els.size()) {
				case 1:
					es = els.get(0);
					break;
				case 2:
					es = els.get(0) + " or " + els.get(1);
					multi = true;
					break;
				default:
					es = join(", ", els.subList(0, - 1)) + " or " + els.get(els.size() - 1);
					multi = true;
				}
			} else {
				if(reportDetailed(e, a)) {
					as = detailedActualToS(e, a);
					es = e.toExpandedString();
				} else {
					as = a.name();
					es = e.name();
				}
				if(optional) {
					es = "Undef or " + es;
					multi = true;
				}
			}
			StringBuilder bld = new StringBuilder();
			bld.append("expects ");
			if(multi) {
				bld.append("a value of type ");
				bld.append(es);
				bld.append(", got ");
			} else {
				bld.append(aOrAn(es));
				bld.append(" value, got ");
			}
			bld.append(as);
			return bld.toString();
		}

		private List<AnyType> allResolved(List<AnyType> tl) {
			return map(tl, this::allResolved);
		}

		private AnyType allResolved(AnyType t) {
			return t instanceof TypeAliasType ? ((TypeAliasType)t).resolvedType() : t;
		}

		private boolean alwaysFullyDetailed(List<AnyType> el, AnyType a) {
			return any(el, e -> alwaysFullyDetailed(e, a));
		}

		// Decides whether or not the report must be fully detailed, or if generalization can be permitted
		// in the mismatch report. All comparisons are made using resolved aliases rather than the alias
		// itself.
		private boolean alwaysFullyDetailed(AnyType e, AnyType a) {
			return e.getClass().equals(a.getClass()) || e instanceof TypeAliasType || a instanceof TypeAliasType ||
					specialization(e, a);
		}

		private boolean anyAssignable(List<AnyType> el, AnyType a) {
			return any(el, e -> e.isAssignable(a));
		}

		private boolean anyAssignable(AnyType e, AnyType a) {
			return e.isAssignable(a);
		}

		private boolean assignableToGeneric(AnyType e, AnyType a) {
			return allResolved(e).generalize().isAssignable(a);
		}

		private String detailedActualToS(List<AnyType> el, AnyType a) {
			el = allResolved(el);
			if(alwaysFullyDetailed(el, a))
				return a.toExpandedString();
			AnyType g = a.generalize();
			return anyAssignable(el, g) ? a.toExpandedString() : g.toString();
		}

		private String detailedActualToS(AnyType e, AnyType a) {
			e = allResolved(e);
			if(alwaysFullyDetailed(e, a))
				return a.toExpandedString();
			AnyType g = a.generalize();
			return anyAssignable(e, g) ? a.toExpandedString() : g.toString();
		}

		private boolean reportDetailed(List<AnyType> el, AnyType a) {
			return any(el, e -> alwaysFullyDetailed(e, a) || assignableToGeneric(e, a));
		}

		private boolean reportDetailed(AnyType e, AnyType a) {
			return alwaysFullyDetailed(e, a) || assignableToGeneric(e, a);
		}

		// Answers the question if `e` is a specialized type of `a`
		private boolean specialization(AnyType e, AnyType a) {
			if(e instanceof StructType)
				return a instanceof HashType;
			return e instanceof TupleType && a instanceof ArrayType;
		}
	}

	private static class PatternMismatch extends TypeMismatch {
		PatternMismatch(List<PathElement> path, AnyType expected, AnyType actual) {
			super(path, expected, actual);
		}

		PatternMismatch(List<PathElement> path, List<AnyType> expected, AnyType actual) {
			super(path, expected, actual);
		}

		String actualString() {
			if(actual instanceof StringType) {
				StringType sa = (StringType)actual;
				if(sa.value != null)
					return '\'' + sa.value + '\'';
			}
			return actual.name();
		}

		@Override
		String text() {
			AnyType e = expected;
			String valuePfx = "";
			if(e instanceof OptionalType) {
				e = ((OptionalType)e).actualType();
				valuePfx = "an undef value or ";
			}
			return "expects " + valuePfx + "a match for " + e.toExpandedString() + ", got " + actualString();
		}
	}

	private static class SizeMismatch extends ExpectedActualMismatch {
		SizeMismatch(List<PathElement> path, IntegerType expected, IntegerType actual) {
			super(path, expected, actual);
		}

		String rangeToS(IntegerType range, String zeroString) {
			long min = range.min;
			long max = range.max;
			if(min == max)
				return min == 0 ? zeroString : Long.toString(min);
			if(min == 0)
				return max == Long.MAX_VALUE ? "unlimited" : "at most " + max;
			if(max == Long.MAX_VALUE)
				return "at least " + min;
			return "between " + min + " and " + max;
		}

		@Override
		String text() {
			return "expects attribute count to be " + rangeToS((IntegerType)expected, "0") + ", got " + rangeToS(
					(IntegerType)actual, "0");
		}
	}

	private static class CountMismatch extends SizeMismatch {
		CountMismatch(List<PathElement> path, IntegerType expected, IntegerType actual) {
			super(path, expected, actual);
		}

		@Override
		String text() {
			long min = ((IntegerType)expected).min;
			long max = ((IntegerType)expected).max;
			String suffix = min == 1 && (max == 1 || max == Long.MAX_VALUE) || min == 0 && max == 1 ? "" : "s";
			return "expects " + rangeToS((IntegerType)expected, "no") + "argument" + suffix + ", got " + rangeToS(
					(IntegerType)actual, "none");
		}
	}

	private enum PathType {
		SUBJECT(null), ENTRY("entry"), ENTRY_KEY("key of entry"), PARAMETER("parameter"),
		RETURN("return"), BLOCK("block"), INDEX("index"), VARIANT("variant"), SIGNATURE(null);

		final String string;

		PathType(String string) {
			this.string = string;
		}

		String format(String key) {
			if(string == null)
				return key;
			return string + " '" + key + "'";
		}
	}

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(TypeMismatchDescriber.class, "_describe", 3);
	public static final TypeMismatchDescriber SINGLETON = new TypeMismatchDescriber();

	private TypeMismatchDescriber() {
	}

	/**
	 * Describe a confirmed mismatch
	 *
	 * @param expected expected type
	 * @param actual   actual type
	 * @return the description
	 */
	public String describeMismatch(AnyType expected, AnyType actual) {
		return errorString(describe(expected, actual, emptyList()));
	}

	/**
	 * @param subject       string to be prepended to the exception message
	 * @param parameterName parameter name
	 * @param parameterType parameter type
	 * @param value         value to be validated against the given type
	 */
	public void validateParameterValue(String subject, String parameterName, AnyType parameterType, Object value) {
		if(parameterType.isInstance(value))
			return;
		String msg = errorString(describe(parameterType, inferSet(value).generalize(),
				singletonList(new PathElement(PathType.PARAMETER, parameterName))));
		if(msg.length() > 0)
			throw new TypeAssertionException(format("%s:%s", subject, msg));
	}

	/**
	 * Validates that all entries in the give_hash exists in the given param_struct, that their type conforms
	 * with the corresponding param_struct element and that all required values are provided.
	 *
	 * @param subject      string to be prepended to the exception message
	 * @param paramsStruct Struct to use for validation
	 * @param parameters   the parameters to validate
	 * @param missingOk    Do not generate errors on missing parameters
	 */
	public void validateParameters(
			String subject, StructType paramsStruct, Map<String,Object> parameters, boolean
			missingOk) {
		String msg = errorString(describeStructSignature(paramsStruct, parameters, missingOk));
		if(msg.length() > 0)
			throw new TypeAssertionException(format("%s:%s", subject, msg));
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	List<? extends Mismatch> _describe(AnyType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return expected.isAssignable(actual) ? emptyList() : singletonList(new TypeMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(ArrayType expected, AnyType original, AnyType actual, List<PathElement> path) {
		if(expected.isAssignable(actual))
			return emptyList();
		if(actual instanceof TupleType)
			return describeArrayTuple(expected, original, (TupleType)actual, path);
		if(actual instanceof ArrayType)
			return describeArrayArray(expected, original, (ArrayType)actual, path);
		return singletonList(new TypeMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(CallableType expected, AnyType original, AnyType actual, List<PathElement> path) {
		if(expected.isAssignable(actual))
			return emptyList();

		if(!(actual instanceof CallableType))
			return singletonList(new TypeMismatch(path, original, actual));

		CallableType ca = (CallableType)actual;

		List<? extends Mismatch> paramErrors = describeArgumentTuple(ca.parametersType, expected.parametersType, path);
		if(!paramErrors.isEmpty())
			return paramErrors;

		if(!expected.returnType.isAssignable(ca.returnType))
			return singletonList(new TypeMismatch(
					append(path, new PathElement(PathType.RETURN, null)), expected.returnType, ca.returnType));

		AnyType ebt = expected.blockType == null ? undefType() : expected.blockType;
		AnyType abt = ca.blockType == null ? undefType() : ca.blockType;

		// NOTE: this test is made in reverse as it is calling the callable that is constrained
		// (it's lower bound), not its upper bound
		if(abt.isAssignable(ebt))
			return emptyList();

		return singletonList(new TypeMismatch(path, ebt, abt));
	}

	List<? extends Mismatch> _describe(EnumType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return expected.isAssignable(actual) ? emptyList() : singletonList(new PatternMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(HashType expected, AnyType original, AnyType actual, List<PathElement> path) {
		if(expected.isAssignable(actual))
			return emptyList();
		if(actual instanceof StructType)
			return describeHashStruct(expected, original, (StructType)actual, path);
		if(actual instanceof HashType)
			return describeHashHash(expected, original, (HashType)actual, path);
		return singletonList(new TypeMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(OptionalType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return actual instanceof UndefType ? emptyList() : doDescribe(expected.type, original instanceof TypeAliasType ? original : expected, actual, path);
	}

	List<? extends Mismatch> _describe(PatternType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return expected.isAssignable(actual) ? emptyList() : singletonList(new PatternMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(StructType expected, AnyType original, AnyType actual, List<PathElement> path) {
		if(expected.isAssignable(actual))
			return emptyList();
		if(actual instanceof StructType)
			return describeStructStruct(expected, original, (StructType)actual, path);
		if(actual instanceof HashType)
			return describeStructHash(expected, original, (HashType)actual, path);
		return singletonList(new TypeMismatch(path, original, actual));
	}

	List<? extends Mismatch> _describe(TupleType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return describeTuple(expected, original, actual, path, false);
	}

	List<? extends Mismatch> _describe(TypeAliasType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return doDescribe(expected.resolvedType().normalize(), expected, actual, path);
	}

	List<? extends Mismatch> _describe(VariantType expected, AnyType original, AnyType actual, List<PathElement> path) {
		List<Mismatch> descriptions = new ArrayList<>();
		List<AnyType> types = expected.types;
		if(original instanceof OptionalType) {
			types = new ArrayList<>(types);
			types.add(TypeFactory.undefType());
		}
		int top = types.size();
		for(int idx = 0; idx < top; ++idx) {
			AnyType et = expected.types.get(idx);
			List<? extends Mismatch> d = doDescribe(et.normalize(), et, actual,
					append(path, new PathElement(PathType.VARIANT, Integer.toString(idx))));
			if(d.isEmpty())
				return d;
			descriptions.addAll(d);
		}
		List<? extends Mismatch> result = mergeDescriptions(path.size(), SizeMismatch.class, descriptions);
		if(original instanceof TypeAliasType && result.size() == 1)
			// All variants failed in this alias so we report it as a mismatch on the alias
			// rather than reporting individual failures of the variants
			result = singletonList(new TypeMismatch(path, original, actual));
		return result;
	}

	private static <T> List<T> append(List<? extends T> list, T value) {
		List<T> result = new ArrayList<>(list);
		result.add(value);
		return result;
	}

	private List<? extends Mismatch> describe(AnyType expected, AnyType actual, List<PathElement> path) {
		String unresolvedType = expected.findUnresolvedType();
		return unresolvedType == null
				? doDescribe(expected.normalize(), expected, actual, path)
				: singletonList(new UnresolvedTypeReference(path, unresolvedType));
	}

	private List<? extends Mismatch> describeArgumentTuple(TupleType expected, AnyType actual, List<PathElement> path) {
		return describeTuple(expected, expected, actual, path, true);
	}

	private List<? extends Mismatch> describeArrayArray(ArrayType expected, AnyType original, ArrayType actual, List<PathElement> path) {
		return expected.size.isAssignable(actual.size)
				? singletonList(new TypeMismatch(path, original, actual))
				: singletonList(new SizeMismatch(path, expected.size, actual.size));
	}

	private List<? extends Mismatch> describeArrayTuple(ArrayType expected, AnyType original, TupleType actual, List<PathElement> path) {
		if(!expected.size.isAssignable(actual.givenOrActualSize))
			return singletonList(new SizeMismatch(path, expected.size, actual.givenOrActualSize));

		List<Mismatch> descriptions = new ArrayList<>();
		int top = actual.types.size();
		for(int idx = 0; idx < top; ++idx) {
			AnyType type = actual.types.get(idx);
			if(!expected.type.isAssignable(type))
				descriptions.addAll(doDescribe(expected.type.normalize(), expected.type, type,
						append(path, new PathElement(PathType.INDEX, Integer.toString(idx)))));
		}
		return descriptions;
	}

	private List<? extends Mismatch> describeHashHash(HashType expected, AnyType original, HashType actual, List<PathElement> path) {
		return expected.size.isAssignable(actual.size)
				? singletonList(new TypeMismatch(path, original, actual))
				: singletonList(new SizeMismatch(path, expected.size, actual.size));
	}

	private List<? extends Mismatch> describeHashStruct(HashType expected, AnyType original, StructType actual, List<PathElement> path) {
		if(!expected.size.isAssignable(actual.size))
			return singletonList(new SizeMismatch(path, expected.size, actual.size));

		List<Mismatch> descriptions = new ArrayList<>();
		for(StructElement m : actual.elements) {
			if(!expected.keyType.isAssignable(m.key))
				descriptions.addAll(doDescribe(expected.keyType.normalize(), expected.keyType, m.key, append(path, new PathElement(PathType.ENTRY_KEY, m
						.name))));
			if(!expected.type.isAssignable(m.value))
				descriptions.addAll(doDescribe(expected.type.normalize(), expected.type, m.value, append(path, new PathElement(PathType.ENTRY, m.name))));
		}
		return descriptions;
	}

	private List<? extends Mismatch> describeStructHash(StructType expected, AnyType original, HashType actual, List<PathElement> path) {
		return expected.size.isAssignable(actual.size)
				? singletonList(new TypeMismatch(path, original, actual))
				: singletonList(new SizeMismatch(path, expected.size, actual.size));
	}

	private List<? extends Mismatch> describeStructSignature(StructType paramsStruct, Map<String,Object> paramHash, boolean missingOk) {
		Map<String,StructElement> paramTypeHash = paramsStruct.hashedMembers();
		List<Mismatch> result = map(select(paramHash.keySet(), p -> !paramTypeHash.containsKey(p)), p -> new InvalidParameter(null, p));
		for(StructElement member : paramsStruct.elements) {
			Object value = paramHash.get(member.name);
			if(paramHash.containsKey(member.name))
				result.addAll(describe(member.value, inferSet(value), singletonList(new
						PathElement(PathType.PARAMETER, member.name))));
			else if(!missingOk || member.key instanceof OptionalType)
				result.add(new MissingParameter(null, member.name));
		}
		return result;
	}

	private List<? extends Mismatch> describeStructStruct(StructType expected, AnyType original, StructType actual, List<PathElement> path) {
		List<Mismatch> descriptions = new ArrayList<>();
		Map<String,StructElement> h2 = new LinkedHashMap<>(actual.hashedMembers());
		for(StructElement e1 : expected.elements) {
			String key = e1.name;
			StructElement e2 = h2.remove(key);
			if(e2 == null) {
				if(!e1.key.isAssignable(undefType()))
					descriptions.add(new MissingKey(path, key));
			} else {
				if(!e1.key.isAssignable(e2.key))
					descriptions.addAll(doDescribe(e1.key.normalize(), e1.key, e2.key, append(path, new PathElement(PathType.ENTRY_KEY, e1.name))));
				if(!e1.value.isAssignable(e2.value))
					descriptions.addAll(doDescribe(e1.value.normalize(), e1.value, e2.value, append(path, new PathElement(PathType.ENTRY, e1.name))));
			}
		}
		for(String key : h2.keySet())
			descriptions.add(new ExtraneousKey(path, key));
		return descriptions;
	}

	private List<? extends Mismatch> describeTuple(TupleType expected, AnyType original, AnyType actual, List<PathElement> path, boolean isCount) {
		if(expected.isAssignable(actual))
			return emptyList();
		if(actual instanceof TupleType)
			return describeTupleTuple(expected, original, (TupleType)actual, path, isCount);
		if(actual instanceof ArrayType)
			return describeTupleArray(expected, original, (ArrayType)actual, path, isCount);
		return singletonList(new TypeMismatch(path, original, actual));
	}

	private List<? extends Mismatch> describeTupleArray(TupleType expected, AnyType original, ArrayType actual, List<PathElement> path, boolean isCount) {
		if(actual.type.isAssignable(anyType()))
			return singletonList(new TypeMismatch(path, original, actual));

		if(!expected.givenOrActualSize.isAssignable(actual.size))
			return singletonList(isCount
					? new CountMismatch(path, expected.givenOrActualSize, actual.size)
					: new SizeMismatch(path, expected.givenOrActualSize, actual.size));

		List<Mismatch> descriptions = new ArrayList<>();
		int top = expected.types.size();
		for(int idx = 0; idx < top; ++idx) {
			AnyType type = expected.types.get(idx);
			if(!type.isAssignable(actual.type))
				descriptions.addAll(doDescribe(type.normalize(), type, actual.type, append(path, new PathElement(PathType.INDEX, Integer
						.toString(idx)))));
		}
		return descriptions;
	}

	private List<? extends Mismatch> describeTupleTuple(
			TupleType expected, AnyType original, TupleType actual, List<PathElement> path,
			boolean isCount) {
		if(!expected.givenOrActualSize.isAssignable(actual.givenOrActualSize))
			return singletonList(isCount
					? new CountMismatch(path, expected.givenOrActualSize, actual.givenOrActualSize)
					: new SizeMismatch(path, expected.givenOrActualSize, actual.givenOrActualSize));

		int expectedSize = expected.types.size();
		if(expectedSize == 0)
			return emptyList();

		List<Mismatch> descriptions = new ArrayList<>();
		int top = actual.types.size();
		for(int idx = 0; idx < top; ++idx) {
			AnyType type = actual.types.get(idx);
			int adx = idx >= expectedSize ? expectedSize : idx;
			AnyType ex = expected.types.get(adx);
			descriptions.addAll(doDescribe(ex.normalize(), ex, type, append(path, new PathElement(
					PathType.INDEX,
					Integer.toString(idx)))));
		}
		return descriptions;
	}

	private List<? extends Mismatch> doDescribe(AnyType expected, AnyType original, AnyType actual, List<PathElement> path) {
		return dispatch(expected, original, actual, path);
	}

	private String errorString(List<? extends Mismatch> errors) {
		switch(errors.size()) {
		case 0:
			return "";
		case 1:
			return errors.get(0).format();
		default:
			return join("\n ", map(errors, Mismatch::format));
		}
	}

	@SuppressWarnings("unchecked")
	private List<? extends Mismatch> mergeDescriptions(
			int varyingPathPosition, Class<? extends Mismatch> mismatchClass,
			List<Mismatch> descriptions) {
		for(Class<? extends Mismatch> mc : asList(mismatchClass, MissingRequiredBlock.class, UnexpectedBlock.class,
				TypeMismatch.class)) {
			List<Mismatch> mismatches = select(descriptions, mc::isInstance);
			if(mismatches.size() == descriptions.size()) {
				// If all have the same canonical path, then we can compact this into one
				Mismatch generic = reduce(mismatches, null, (prev, curr) ->
						prev == null
								? curr
								: (curr == null || !curr.canonicalPath().equals(prev.canonicalPath())
										? null
										: prev.merge(prev.path, curr))
				);
				if(generic != null)
					return singletonList(generic.removeElementAt(varyingPathPosition));
			}
		}
		List<Mismatch> unique = new ArrayList<>(new LinkedHashSet<>(descriptions));
		return unique.size() == 1 ? singletonList(unique.get(0).removeElementAt(varyingPathPosition)) : unique;
	}
}
