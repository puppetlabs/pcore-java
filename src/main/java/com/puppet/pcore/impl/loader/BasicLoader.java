package com.puppet.pcore.impl.loader;

import com.puppet.pcore.NoSuchTypeException;
import com.puppet.pcore.TypeRedefinedException;
import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.loader.TypedName;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static com.puppet.pcore.impl.Constants.RUNTIME_NAME_AUTHORITY;

public class BasicLoader implements Loader {
	private final Map<TypedName,Object> boundObjects = new HashMap<>();
	private boolean frozen = false;

	@Override
	public void bind(String type, String name, Object toBeBound) throws TypeRedefinedException {
		bind(new TypedName(type, name, getNameAuthority()), toBeBound);
	}

	@Override
	public synchronized void bind(TypedName name, Object type) throws TypeRedefinedException {
		if(loadOrNull(name) != null)
			throw new TypeRedefinedException(name.toString());
		assertModifiable();
		boundObjects.put(name, type);
	}

	@Override
	public void freeze() {
		frozen = true;
	}

	@Override
	public URI getNameAuthority() {
		return RUNTIME_NAME_AUTHORITY;
	}

	@Override
	public synchronized Object load(TypedName name) throws NoSuchTypeException {
		Object type = loadOrNull(name);
		if(type == null)
			throw new NoSuchTypeException(name.toString());
		return type;
	}

	@Override
	public synchronized Object loadOrNull(TypedName name) throws NoSuchTypeException {
		return boundObjects.get(name);
	}

	private void assertModifiable() {
		if(frozen)
			throw new IllegalStateException("Attempt to modify frozen loader");
	}
}
