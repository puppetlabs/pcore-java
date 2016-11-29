package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableMap;

public class StructType extends AnyType {

	public static final StructType DEFAULT = new StructType(Collections.emptyList());
	static final AnyType KEY_TYPE = variantType(StringType.NOT_EMPTY, optionalType(StringType.NOT_EMPTY));

	private static ObjectType ptype;
	public final List<StructElement> elements;
	public final IntegerType size;
	private Map<String,StructElement> hashedMembers;

	StructType(List<StructElement> elements) {
		this.elements = elements;
		size = integerType(elements.stream().filter(m -> !m.key.isAssignable(UndefType.DEFAULT)).count(), elements.size());
	}

	@Override
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		return o instanceof StructType && elements.equals(((StructType)o).elements);
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return elements.hashCode();
	}

	public Map<String,StructElement> hashedMembers() {
		if(hashedMembers == null) {
			Map<String,StructElement> hm = new LinkedHashMap<>();
			elements.forEach(m -> hm.put(m.name, m));
			hashedMembers = unmodifiableMap(hm);
		}
		return hashedMembers;
	}

	@SuppressWarnings("unchecked")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		StructElement.registerPcoreType(pcore);
		return ptype = pcore.createObjectType(StructType.class, "Pcore::StructType", "Pcore::AnyType",
				asMap("elements", arrayType(typeReferenceType("Pcore::StructElement"))),
				(args) -> structType((List<StructElement>)args.get(0)),
				(self) -> new Object[]{self.elements});
	}

	@Override
	void accept(Visitor visitor, RecursionGuard guard) {
		elements.forEach(member -> member.accept(visitor, guard));
		super.accept(visitor, guard);
	}

	@Override
	IterableType asIterableType(RecursionGuard guard) {
		if(this.equals(DEFAULT))
			return iterableType(HashType.DEFAULT_KEY_PAIR_TUPLE);
		return iterableType(tupleType(asList(
				variantType(elements.stream().map(member -> member.key)),
				variantType(elements.stream().map(member -> member.value))), HashType.KEY_PAIR_TUPLE_SIZE));
	}

	@Override
	boolean isIterable(RecursionGuard guard) {
		return true;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof StructType) {
			StructType ht = (StructType)t;
			Map<String,StructElement> h2 = ht.hashedMembers();
			int[] matched = {0};
			return elements.stream().allMatch(e1 -> {
				StructElement e2 = h2.get(e1.name);
				if(e2 == null)
					return e1.key.isAssignable(undefType(), guard);
				matched[0]++;
				return e1.key.isAssignable(e2.key, guard) && e1.value.isAssignable(e2.value, guard);
			}) && matched[0] == h2.size();
		}

		if(t instanceof HashType) {
			HashType ht = (HashType)t;
			int[] required = {0};
			boolean requiredMembersAssignable = elements.stream().allMatch(e -> {
				AnyType key = e.key;
				if(key.isAssignable(undefType(), guard))
					// StructElement is optional so Hash does not need to provide it
					return true;

				required[0]++;

				// Hash must have something that is assignable. We don't care about the name or attributeCount of the key
				// though
				// because we have no instance of a hash to compare against.
				return e.value.isAssignable(ht.type) && key.generalize().isAssignable(ht.keyType, guard);
			});
			if(requiredMembersAssignable)
				return integerType(required[0], elements.size()).isAssignable(ht.size, guard);
		}
		return false;
	}
}
