package com.puppet.pcore.impl.types;

import com.puppet.pcore.PcoreException;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class InitType extends TypeContainerType {
	static final InitType DEFAULT = new InitType(AnyType.DEFAULT, emptyList(), true);

	private static ObjectType ptype;

	public final List<?> initArgs;

	private boolean hasOptionalSingle;

	private boolean initialized;

	private AnyType singleType;

	private AnyType otherType;

	private boolean selfRecursion = false;

	InitType(AnyType type, List<?> initArgs, boolean resolved) {
		super(type, resolved);
		this.initArgs = initArgs;
		this.selfRecursion = true;
		if(AnyType.DEFAULT.equals(type)) {
			if(!initArgs.isEmpty())
				throw new IllegalArgumentException("Init cannot be parameterized with an undefined type and additional arguments");
			initialized = true;
		}
		else
			initialized = false;
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public AnyType actualType() {
		return type.actualType();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> FactoryDispatcher<T> factoryDispatcher() {
		if(AnyType.DEFAULT.equals(type))
			return super.factoryDispatcher();

		assertInitialized();
		if(initArgs.isEmpty()) {
			return (FactoryDispatcher<T>)dispatcher(
					constructor((args) -> {
						List<?> value = (List<?>)args.get(0);
						return singleType.isInstance(value) || otherType != null && !otherType.isInstance(value) && hasOptionalSingle && otherType.isInstance(args)
							? type.newInstance(value)
							: type.newInstance(value.toArray());
					},
						arrayType()),
					constructor((args) -> type.newInstance(args.get(0)),
						anyType())
			);
		}
		return (FactoryDispatcher<T>)dispatcher(
				constructor((args) -> {
						Object[] allArgs = new Object[1 + initArgs.size()];
						allArgs[0] = args.get(0);
						for(int idx = 1; idx < allArgs.length; ++idx)
							allArgs[idx] = initArgs.get(idx - 1);
						return type.newInstance(allArgs);
					},
						anyType())
		);
	}

	@Override
	public AnyType generalize() {
		return equals(DEFAULT) ? this : new InitType(type.generalize(), emptyList(), true);
	}

	public int hashCode() {
		return super.hashCode() * 31 + initArgs.hashCode();
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::InitType", "Pcore::AnyType",
				asMap(
						"type", asMap(
								KEY_TYPE, typeType(),
								KEY_VALUE, anyType()),
						"init_args", asMap(
								KEY_TYPE, arrayType(),
								KEY_VALUE, emptyList())));
	}

	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, initTypeDispatcher(),
				(self) -> new Object[]{self.type, self.initArgs});
	}

	@Override
	AnyType copyWith(AnyType type, boolean resolved) {
		return new InitType(type, initArgs, resolved);
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		return super.guardedEquals(o, guard) && equals(initArgs, ((InitType)o).initArgs, guard);
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return isReallyInstance(o, guard) == 1;
	}

	@Override
	int isReallyInstance(Object o, RecursionGuard guard) {
		if(type.equals(AnyType.DEFAULT))
			return richDataType().isReallyInstance(o, guard);

		assertInitialized();
		return guardedRecursion(guard, 0, (g) -> {
			int v = type.isReallyInstance(o, g);
			if(v < 1 && singleType != null) {
				int s = singleType.isReallyInstance(o, g);
				if(s > v)
					v = s;
			}
			if(v < 1 && otherType != null) {
				int s = otherType.isReallyInstance(o, g);
				if(s < 0 && hasOptionalSingle)
					s = otherType.isReallyInstance(singletonList(o), g);
				if(s > v)
					v = s;
			}
			return v;
		});
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return guardedRecursion(guard, false, (g) -> {
			assertInitialized();
			if(t instanceof InitType)
				return type.isAssignable(((InitType)t).type, g);
			if(AnyType.DEFAULT.equals(type))
				return richDataType().isAssignable(t, g);
			return type.isAssignable(t, g) ||
					singleType != null && singleType.isAssignable(t, g) ||
					otherType != null && (otherType.isAssignable(t, g) || hasOptionalSingle && otherType.isAssignable(tupleType(singletonList(t)), g));
		});
	}

	private void assertInitialized() {
		if(initialized)
			return;

		initialized = true;
		selfRecursion = true;

		if(type instanceof InitType || type instanceof OptionalType || type instanceof NotUndefType)
			throw new PcoreException(format("Creation of new instance of type '%s' is not supported", type.name()));
		FactoryDispatcher<?> dispatcher = type.factoryDispatcher();
		List<TupleType> paramTuples = map(dispatcher.constructors(), (ctor) -> (TupleType)ctor.signature());

		List<AnyType> singleTypes = new ArrayList<>();
		singleTypes.add(type);

		List<TupleType> otherTuples;

		if(initArgs.isEmpty()) {
			List<TupleType>[] partitioned = partitionBy(paramTuples, (tuple) -> IntegerType.ONE.equals(tuple.size));
			singleTypes.addAll(map(partitioned[0], (tuple) -> tuple.types.get(0)));
			otherTuples = partitioned[1];
		} else {
			List<AnyType> initArgTypes = map(initArgs, TypeFactory::inferSet);
			int argCount = 1 + initArgTypes.size();

			paramTuples = select(paramTuples, (tuple) -> {
				IntegerType size = tuple.givenOrActualSize;
				return argCount >= size.min && argCount <= size.max &&
					tuple.isAssignable(tupleType(concat(asList(tuple.types.get(0)), initArgTypes)));
			});

			if(paramTuples.isEmpty())
				throw new PcoreException(format("The type '%s' does not represent a valid set of parameters for 'new' function", toString()));
			singleTypes.addAll(map(paramTuples, (tuple) -> tuple.types.get(0)));
			otherTuples = emptyList();
		}
		singleType = variantType(singleTypes);
		if(!otherTuples.isEmpty()) {
			otherType = variantType(otherTuples);
			hasOptionalSingle = any(otherTuples, (tuple) -> tuple.givenOrActualSize.min == 1);
		}

		RecursionGuard guard = new RecursionGuard();
		accept(NoopAcceptor.singleton, guard);
		selfRecursion = guard.recursiveThis(this);
	}

	private <R> R guardedRecursion(RecursionGuard guard, R dflt, Function<RecursionGuard,? extends R> block) {
		if(selfRecursion) {
			RecursionGuard g = guard == null ? new RecursionGuard() : guard;
			return g.withThis(this, state -> (state & RecursionGuard.SELF_RECURSION_IN_THIS) == 0 ? block.apply(g) : dflt);
		}
		return block.apply(guard);
	}
}
