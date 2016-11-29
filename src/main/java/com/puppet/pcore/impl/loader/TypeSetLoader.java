package com.puppet.pcore.impl.loader;

import com.puppet.pcore.impl.types.TypeSetType;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;

import java.net.URI;

public class TypeSetLoader extends ParentedLoader {
	public final TypeSetType typeSet;

	public TypeSetLoader(Loader parentLoader, TypeSetType typeSet) {
		super(parentLoader);
		this.typeSet = typeSet;
	}

	@Override
	public URI getNameAuthority() {
		return typeSet.getNameAuthority();
	}

	@Override
	public Object loadOrNull(TypedName name) {
		Object found = super.loadOrNull(name);
		if(found == null && "type".equals(name.type) && typeSet.getNameAuthority().equals(name.nameAuthority))
			found = typeSet.get(name.name);
		return found;
	}
}
