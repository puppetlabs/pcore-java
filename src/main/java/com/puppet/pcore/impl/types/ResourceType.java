package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class ResourceType extends CatalogEntryType {
	public static final ResourceType DEFAULT = new ResourceType(null, null);

	private static ObjectType ptype;
	public final String title;
	public final String typeName;
	private final String downcasedName;

	ResourceType(String typeName, String title) {
		this.typeName = typeName;
		this.title = title;
		this.downcasedName = typeName == null ? null : typeName.toLowerCase();
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
		return Objects.hashCode(downcasedName) * 31 + Objects.hashCode(title);
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::ResourceType", "Pcore::CatalogEntryType",
				asMap(
						"type_name", asMap(
								KEY_TYPE, optionalType(StringType.NOT_EMPTY),
								KEY_VALUE, null),
						"title", asMap(
								KEY_TYPE, optionalType(StringType.NOT_EMPTY),
								KEY_VALUE, null)));
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, resourceTypeDispatcher(),
				(self) -> new Object[]{self.typeName, self.title});
	}

	@Override
	boolean guardedEquals(Object o, RecursionGuard guard) {
		if(o instanceof ResourceType) {
			ResourceType rt = (ResourceType)o;
			return Objects.equals(downcasedName, rt.downcasedName) && Objects.equals(title, rt.title);
		}
		return false;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		if(t instanceof ResourceType) {
			ResourceType rt = (ResourceType)t;
			return (downcasedName == null || Objects.equals(downcasedName, rt.downcasedName)) && (title == null || Objects
					.equals(title, rt.title));
		}
		return false;
	}

	@Override
	AnyType notAssignableSameClassCommon(AnyType other) {
		ResourceType rt = (ResourceType)other;
		return Objects.equals(downcasedName, rt.downcasedName) ? resourceType(typeName) : resourceType();
	}
}
