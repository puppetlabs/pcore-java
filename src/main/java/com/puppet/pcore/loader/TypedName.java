package com.puppet.pcore.loader;

import java.net.URI;
import java.util.Locale;

import static com.puppet.pcore.impl.Constants.RUNTIME_NAME_AUTHORITY;
import static com.puppet.pcore.impl.Helpers.splitName;

/**
 * A typed name consists of a type, a case insensitive name, and a name authority. This class is optimized
 * to use as a key in a hash lookup.
 */
public class TypedName {
	/**
	 * The name (case preserved)
	 */
	public final String name;

	/**
	 * The name authority
	 */
	public final URI nameAuthority;

	/**
	 * The type.
	 */
	public final String type;

	private final String compoundName;
	private final int hash;
	private final String[] nameParts;

	public TypedName(String type, String name) {
		this(type, name, RUNTIME_NAME_AUTHORITY);
	}

	public TypedName(String type, String name, URI nameAuthority) {
		this.type = type;
		this.nameAuthority = nameAuthority;
		String[] parts = splitName(name);
		if(parts.length > 0 && parts[0].isEmpty()) {
			String[] np = new String[parts.length - 1];
			System.arraycopy(parts, 1, np, 0, np.length);
			parts = np;
			name = name.substring(2);
		}
		this.name = name;
		this.nameParts = parts;
		this.compoundName = (name + '/' + type + '/' + nameAuthority).toLowerCase(Locale.ENGLISH);
		this.hash = compoundName.hashCode();
	}

	/**
	 * Performs a case insensitive compare of type, name, and name authority.
	 * @param o the object to compare with
	 * @return {@code true} if the objects are equal
	 */
	public boolean equals(Object o) {
		return o instanceof TypedName && compoundName.equals(((TypedName)o).compoundName);
	}

	/**
	 * Returns a case insensitive hash for this typed name.
	 * @return the hash code
	 */
	public int hashCode() {
		return hash;
	}

	/**
	 * @return {@code true} if the name part of this typed name is a qualified name (uses :: separator)
	 */
	public boolean isQualified() {
		return nameParts.length > 1;
	}

	/**
	 * Returns the case preserved representation of this instance.
	 *
	 * @return a string with {@code "<name authority>/<type>/<name>"}
	 */
	public String toString() {
		return nameAuthority.toString() + '/' + type + '/' + name;
	}
}
