package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.TypeResolverException;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.TypeEvaluatorImpl;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.serialization.ArgumentsAccessor;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.anyType;
import static com.puppet.pcore.impl.types.TypeFactory.variantType;
import static java.lang.String.format;

public class TypeAliasType extends AnyType {
	/**
	 * Acceptor used when checking for self recursion and that a type contains
	 * something other than aliases or type references
	 */
	private static class AssertOtherTypeAcceptor implements Visitor {
		boolean otherTypeDetected = false;

		@Override
		public void visit(ModelObject type, RecursionGuard guard) {
			if(!(otherTypeDetected || type instanceof TypeAliasType || type instanceof VariantType))
				otherTypeDetected = true;
		}
	}

	/**
	 * Acceptor used when re-checking for self recursion that resets self recursion status in detected type aliases
	 */
	private static class AssertSelfRecursionStatusAcceptor implements Visitor {
		@Override
		public void visit(ModelObject type, RecursionGuard guard) {
			if(type instanceof TypeAliasType)
				((TypeAliasType)type).setSelfRecursionStatus();
		}
	}

	public static final TypeAliasType DEFAULT = new TypeAliasType("UnresolvedAlias", null, DefaultType.DEFAULT);

	private static ObjectType ptype;
	public final String name;
	private final Object typeExpression;
	private AnyType resolvedType;
	private boolean selfRecursion;

	TypeAliasType(ArgumentsAccessor args) throws IOException {
		args.remember(this);
		this.name = (String)args.get(0);
		this.typeExpression = null;
		this.resolvedType = (AnyType)args.get(1);
	}

	TypeAliasType(String name, Object typeExpression, AnyType resolvedType) {
		this.name = name;
		this.typeExpression = typeExpression;
		this.resolvedType = resolvedType;
		this.selfRecursion = false;
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		return super.equals(o) && name.equals(((TypeAliasType)o).name);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public boolean isRecursive() {
		return selfRecursion;
	}

	@Override
	public AnyType resolve(TypeEvaluator evaluator) {
		if(resolvedType == null) {
			// resolved to TypeReferenceType.DEFAULT during resolve to avoid endless recursion
			resolvedType = TypeReferenceType.DEFAULT;
			selfRecursion = true; // assumed while it's being found out below
			try {
				if(typeExpression instanceof TypeReferenceType)
					resolvedType = ((TypeReferenceType)typeExpression).resolve(evaluator);
				else
					resolvedType = ((TypeEvaluatorImpl)evaluator).resolveType((Expression)typeExpression);

				// Find out if this type is recursive. A recursive type has performance implications
				// on several methods and this knowledge is used to avoid that for non-recursive
				// types.
				RecursionGuard guard = new RecursionGuard();
				AssertOtherTypeAcceptor realTypeAsserter = new AssertOtherTypeAcceptor();
				accept(realTypeAsserter, guard);
				if(!realTypeAsserter.otherTypeDetected)
					throw new TypeResolverException(format("Type alias '%s' cannot be resolved to a real type", name));
				selfRecursion = guard.recursiveThis(this);

				if(selfRecursion) {
					// All aliases involved must re-check status since this alias is now resolved
					accept(new AssertSelfRecursionStatusAcceptor(), new RecursionGuard());
					whenSelfRecursionDetected();
				}
			} catch(RuntimeException e) {
				resolvedType = null;
				throw e;
			}
		} else
			// An alias may appoint an Object type that isn't resolved yet. The default type
			// reference is used to prevent endless recursion and should not be resolved here.
			if(!resolvedType.equals(TypeReferenceType.DEFAULT))
				resolvedType.resolve(evaluator);

		return this;
	}

	@Override
	public AnyType resolvedType() {
		if(resolvedType == null)
			throw new TypeResolverException(format("Reference to unresolved type '%s'", name));
		return resolvedType.resolvedType();
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(TypeAliasType.class, "Pcore::TypeAliasType", "Pcore::AnyType",
				asMap(
						"name", StringType.NOT_EMPTY,
						"resolved_type", anyType()),
				TypeAliasType::new,
				(self) -> new Object[]{self.name, self.resolvedType()});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		guardedRecursion(guard, null, g -> {
			if(resolvedType != null)
				resolvedType.accept(visitor, g);
			super.accept(visitor, g);
			return null;
		});
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		return guardedRecursion(guard, null, g -> resolvedType().asIterableType(g));
	}

	@Override
	void checkSelfRecursion(AnyType originator) {
		if(originator != this)
			resolvedType().checkSelfRecursion(originator);
	}

	@Override
	boolean isAssignable(AnyType o, RecursionGuard guard) {
		if(isRecursive()) {
			RecursionGuard g = guard == null ? new RecursionGuard() : guard;
			return g.withThis(this, state -> state == RecursionGuard.SELF_RECURSION_IN_BOTH || super.isAssignable(o, g));
		}
		return super.isAssignable(o, guard);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return isReallyInstance(o, guard) == 1;
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return guardedRecursion(guard, false, g -> resolvedType().isIterable(g));
	}

	@Override
	int isReallyInstance(Object o, RecursionGuard guard) {
		if(selfRecursion) {
			RecursionGuard g = guard == null ? new RecursionGuard() : guard;
			return g.withThat(o, thatState -> g.withThis(this, state -> state == RecursionGuard.SELF_RECURSION_IN_BOTH ? 0 : resolvedType().isReallyInstance(o, g)));
		}
		return resolvedType().isReallyInstance(o, guard);
	}

	@Override
	boolean isUnsafeAssignable(AnyType type, RecursionGuard guard) {
		return resolvedType().isUnsafeAssignable(type, guard);
	}

	private <R> R guardedRecursion(RecursionGuard guard, R dflt, Function<RecursionGuard,? extends R> block) {
		if(selfRecursion) {
			RecursionGuard g = guard == null ? new RecursionGuard() : guard;
			return g.withThis(this, state -> (state & RecursionGuard.SELF_RECURSION_IN_THIS) == 0 ? block.apply(g) : dflt);
		}
		return block.apply(guard);
	}

	private void setSelfRecursionStatus() {
		if(selfRecursion || resolvedType instanceof TypeReferenceType)
			return;
		selfRecursion = true;
		RecursionGuard guard = new RecursionGuard();
		accept(NoopAcceptor.singleton, guard);
		selfRecursion = guard.recursiveThis(this);
		whenSelfRecursionDetected();
	}

	private void whenSelfRecursionDetected() {
		if(resolvedType instanceof VariantType) {
			// Drop variants that are not real types
			List<AnyType> resolvedTypes = ((VariantType)resolvedType).types;
			List<AnyType> realTypes = resolvedTypes.stream().filter(type -> {
				if(type == this)
					return false;
				AssertOtherTypeAcceptor realTypeAsserter = new AssertOtherTypeAcceptor();
				type.accept(realTypeAsserter, new RecursionGuard());
				return realTypeAsserter.otherTypeDetected;
			}).collect(Collectors.toList());
			if(realTypes.size() != resolvedTypes.size()) {
				resolvedType = variantType(realTypes);
				RecursionGuard guard = new RecursionGuard();
				accept(NoopAcceptor.singleton, guard);
				selfRecursion = guard.recursiveThis(this);
			}
		}
		if(selfRecursion)
			resolvedType.checkSelfRecursion(this);
	}
}
