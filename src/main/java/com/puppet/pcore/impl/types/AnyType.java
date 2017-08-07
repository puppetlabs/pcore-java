package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.*;
import com.puppet.pcore.serialization.ArgumentsAccessor;
import com.puppet.pcore.serialization.Constructor;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import static com.puppet.pcore.impl.Helpers.any;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;

public class AnyType extends ModelObject implements Type, PuppetObject {
	private static class UnresolvedTypeFinder implements Visitor {
		String unresolved = null;

		@Override
		public void visit(ModelObject type, RecursionGuard guard) {
			if(unresolved == null && type instanceof TypeReferenceType)
				unresolved = ((TypeReferenceType)type).typeString;
		}
	}

	static final AnyType DEFAULT = new AnyType();
	private static ObjectType ptype;

	AnyType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	public <T> T assertInstanceOf(T actual, boolean nullOK, Supplier<String> identifier) {
		if(nullOK && actual == null || isInstance(actual))
			return actual;

		throw new TypeAssertionException(identifier.get() + ' ' +
				TypeMismatchDescriber.SINGLETON.describeMismatch(this, inferSet(actual)));
	}

	public <T> T assertInstanceOf(T actual, Supplier<String> identifier) {
		return assertInstanceOf(actual, false, identifier);
	}

	@Override
	public AnyType common(Type other) {
		if(isAssignable(other))
			return this;
		if(other.isAssignable(this))
			return (AnyType)other;
		return notAssignableCommon((AnyType)other);
	}

	public String findUnresolvedType() {
		UnresolvedTypeFinder unresolvedFinder = new UnresolvedTypeFinder();
		accept(unresolvedFinder, null);
		return unresolvedFinder.unresolved;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	@Override
	@SuppressWarnings("unchecked")
	public FactoryDispatcher<?> factoryDispatcher() {
		throw new PcoreException(format("Creation of new instance of type '%s' is not supported", name()));
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	@Override
	public final boolean isAssignable(Type t) {
		return isAssignable((AnyType)t, null);
	}

	@Override
	public final boolean isInstance(Object o) {
		return isInstance(o, null);
	}

	/**
	 * Returns the simple name of the class stripped from the "Type" suffix
	 *
	 * @return the simple name
	 */
	@Override
	public String name() {
		String s = getClass().getSimpleName();
		return s.substring(0, s.length() - 4);
	}

	public Object newInstance(ArgumentsAccessor args) throws IOException {
		return factoryDispatcher().createInstance(this, args);
	}

	public Object newInstance(Object... args) {
		return factoryDispatcher().createInstance(this, args);
	}

	@Override
	public AnyType normalize() {
		return this;
	}

	public AnyType resolve() {
		return this;
	}

	public ParameterInfo parameterInfo() {
		throw new PcoreException(format("Creation of new instance of type '%s' is not supported", name()));
	}

	public boolean roundtripWithString() {
		return false;
	}

	public boolean roundtripWithHash() {
		return any(factoryDispatcher().constructors(), Constructor::isHashConstructor);
	}

	public void assertHashInitializer(Map<?, ?> hash) {
		for(ConstructorImpl<?> ctor : ((FactoryDispatcherImpl<?>)factoryDispatcher()).constructors())
			if(ctor.isHashConstructor()) {
				ctor.signature().types.get(0).assertInstanceOf(hash, () -> format("initializer hash for %s", name()));
				return;
			}
		throw new PcoreException(format("Creation of new instance of type '%s' using an initializer hash is not supported", name()));
	}

	@Override
	public String toDebugString() {
		StringBuilder bld = new StringBuilder();
		buildString(bld, true, true);
		return bld.toString();
	}

	@Override
	public String toExpandedString() {
		StringBuilder bld = new StringBuilder();
		buildString(bld, true, false);
		return bld.toString();
	}

	@Override
	public final String toString() {
		StringBuilder bld = new StringBuilder();
		buildString(bld, false, false);
		return bld.toString();
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::AnyType", null);
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, anyTypeDispatcher());
	}

	/**
	 * Returns the actual type for this type which will correspond to `this` in all cases
	 * except for the {@link NotUndefType} and {@link OptionalType}. They will instead return the result of
	 * calling this method on the type that they contain.
	 *
	 * @return the actual type
	 */
	AnyType actualType() {
		return this;
	}

	final IterableType asIterableType() {
		return asIterableType(null);
	}

	IterableType asIterableType(RecursionGuard guard) {
		return null;
	}

	void checkSelfRecursion(AnyType originator) {
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return o != null && o.getClass().equals(getClass());
	}

	/**
	 * Checks if _type_ is a type that is assignable to this type.
	 * <p>
	 * The check for assignable must be guarded against self recursion since `this`, the given type _type_,
	 * or both, might be a `TypeAlias`. The initial caller of this method will typically never care
	 * about this and hence pass only the first argument, but as soon as a check of a contained type
	 * encounters a `TypeAlias`, then a `RecursionGuard` instance is created and passed on in all
	 * subsequent calls. The recursion is allowed to continue until self recursion has been detected in
	 * both `this` and in the given type. At that point the given type is considered to be assignable
	 * to `this` since all checks up to that point were positive.
	 *
	 * @param t     the class or type to test
	 * @param guard guard against recursion. Only used by internal calls
	 * @return `true` when _o_ is assignable to this type
	 */
	boolean isAssignable(AnyType t, RecursionGuard guard) {
		if(t == null)
			return false;
		if(t instanceof UnitType || getClass().equals(AnyType.class))
			return true;

		if(t instanceof TypeAliasType) {
			if(t.isRecursive()) {
				RecursionGuard g = guard == null ? new RecursionGuard() : guard;
				// A recursion detected both in self and other means that other is assignable
				// to self. This point would not have been reached otherwise
				return g.withThat(t, state -> state == RecursionGuard.SELF_RECURSION_IN_BOTH || isAssignable(t.resolvedType(), g));
			}
			return isAssignable(t.resolvedType(), guard);
		}

		if(t instanceof VariantType) {
			// Assignable if all contained variants are assignable
			return Helpers.all(((VariantType)t).types, variant -> isAssignable(variant, guard));
		}

		if(t instanceof NotUndefType) {
			NotUndefType nut = (NotUndefType)t;
			if(!nut.type.isAssignable(UndefType.DEFAULT, guard))
				return isAssignable(nut.type, guard);
		}
		return isUnsafeAssignable(t, guard);
	}

	boolean isInstance(Object o, RecursionGuard guard) {
		return true;
	}

	final boolean isIterable() {
		return isIterable(null);
	}

	boolean isIterable(RecursionGuard guard) {
		return false;
	}

	int isReallyInstance(Object o, RecursionGuard guard) {
		return isInstance(o, guard) ? 1 : -1;
	}

	boolean isRecursive() {
		return false;
	}

	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return true;
	}

	AnyType notAssignableCommon(AnyType other) {
		if(getClass().equals(other.getClass()))
			return notAssignableSameClassCommon(other);
		if(isCommonNumeric(this, other))
			return numericType();
		if(isCommonScalarData(this, other))
			return scalarDataType();
		if(isCommonScalar(this, other))
			return scalarType();
		if(isCommonData(this, other))
			return dataType();
		return DEFAULT;
	}

	AnyType notAssignableSameClassCommon(AnyType other) {
		return generalize();
	}

	AnyType resolvedType() {
		return this;
	}

	private static boolean isCommonData(AnyType t1, AnyType t2) {
		return dataType().isAssignable(t1) && dataType().isAssignable(t2);
	}

	private static boolean isCommonNumeric(AnyType t1, AnyType t2) {
		return numericType().isAssignable(t1) && numericType().isAssignable(t2);
	}

	private static boolean isCommonScalar(AnyType t1, AnyType t2) {
		return scalarType().isAssignable(t1) && scalarType().isAssignable(t2);
	}

	private static boolean isCommonScalarData(AnyType t1, AnyType t2) {
		return scalarDataType().isAssignable(t1) && scalarDataType().isAssignable(t2);
	}

	private void buildString(StringBuilder bld, boolean expanded, boolean debug) {
		new TypeFormatter(bld, -1, 0, expanded, debug).format(this);
	}
}
