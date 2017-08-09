package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public class HashExpression extends Positioned {
	public final List<KeyedEntry> entries;

	public HashExpression(List<KeyedEntry> entries, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.entries = unmodifiableCopy(entries);
	}

	public boolean equals(Object o) {
		return super.equals(o) && entries.equals(((HashExpression)o).entries);
	}

	/**
	 * Finds an expression with that represents the string given by {@code key} and returns it.
	 *
	 * @param key the key to search for
	 * @return the found expression or {@code null} if no expression matches the given key.
	 */
	public Expression getValue(String key) {
		for(KeyedEntry entry : entries)
			if(entry.key instanceof NameExpression && ((NameExpression)entry.key).name().equals(key))
				return entry.value;
		return null;
	}
}
