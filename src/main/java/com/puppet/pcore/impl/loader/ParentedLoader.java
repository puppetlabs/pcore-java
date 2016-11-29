package com.puppet.pcore.impl.loader;

import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;

public class ParentedLoader extends BasicLoader {
	private final Loader parentLoader;

	public ParentedLoader(Loader parentLoader) {
		this.parentLoader = parentLoader;
	}

	@Override
	public Object loadOrNull(TypedName name) {
		Object found = parentLoader.loadOrNull(name);
		return found == null ? super.loadOrNull(name) : found;
	}
}
