package com.puppet.pcore.impl.types;

import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;

import java.util.Objects;

import static com.puppet.pcore.impl.Constants.KEY_TYPE;
import static com.puppet.pcore.impl.Constants.KEY_VALUE;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.optionalType;
import static com.puppet.pcore.impl.types.TypeFactory.resourceType;

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
	public Type _pType() {
		return ptype;
	}

	public boolean equals(Object o) {
		if(o instanceof ResourceType) {
			ResourceType rt = (ResourceType)o;
			return Objects.equals(downcasedName, rt.downcasedName) && Objects.equals(title, rt.title);
		}
		return false;
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public int hashCode() {
		return Objects.hashCode(downcasedName) * 31 + Objects.hashCode(title);
	}

	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType(ResourceType.class, "Pcore::ResourceType", "Pcore::CatalogEntryType",
				asMap(
						"type_name", asMap(
								KEY_TYPE, optionalType(StringType.NOT_EMPTY),
								KEY_VALUE, null),
						"title", asMap(
								KEY_TYPE, optionalType(StringType.NOT_EMPTY),
								KEY_VALUE, null)),
				(args) -> resourceType((String)args.get(0), (String)args.get(1)),
				(self) -> new Object[]{self.typeName, self.title});
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
