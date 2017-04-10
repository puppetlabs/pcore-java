package com.puppet.pcore.semver;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class that implements a <a href="http://semver.org/spec/v1.0.0.html">Semantic Versioning 1.0.0</a>.
 */
public class Version implements Comparable<Version>, Serializable {
	public static final Version MAX = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, null, null);
	public static final List<Comparable<?>> MIN_PRE_RELEASE = Collections.EMPTY_LIST;
	public static final Version MIN = new Version(0, 0, 0, MIN_PRE_RELEASE, null);

	private static final String PART = "[0-9A-Za-z-]+";
	private static final String PARTS = PART + "(?:\\." + PART + ")*";
	private static final String PRERELEASE = "(?:-(" + PARTS + "))?";
	private static final String BUILD = "(?:\\+(" + PARTS + "))?";
	private static final String QUALIFIER = PRERELEASE + BUILD;
	private static final String NR = "(0|[1-9][0-9]*)";

	private static final Pattern PARTS_PATTERN = Pattern.compile("\\A" + PARTS + "\\z");
	private static final Pattern VERSION_PATTERN = Pattern.compile("\\A" + NR + "\\." + NR + "\\." + NR + QUALIFIER + "\\z");

	private static final WeakCache<Version> instanceCache = new WeakCache<>();
	private static final long serialVersionUID = 1L;
	private final int major;
	private final int minor;
	private final int patch;
	private final List<Comparable<?>> preRelease;
	private final List<Comparable<?>> build;

	public enum Level { Patch, Minor, Major }

	private Version(int major, int minor, int patch, List<Comparable<?>> preRelease, List<Comparable<?>> build) {
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.preRelease = preRelease;
		this.build = build;
	}

	public Version toStable() {
		return isStable() ? this : new Version(major, minor, patch, null, build);
	}

	static int comparePreReleases(List<Comparable<?>> p1, List<Comparable<?>> p2) {
		if(p1 == null)
			return p2 == null ? 0 : 1;

		if(p2 == null)
			return -1;

		int p1Size = p1.size();
		int p2Size = p2.size();
		int commonMax = p1Size > p2Size ? p2Size : p1Size;
		for(int idx = 0; idx < commonMax; ++idx) {
			Object v1 = p1.get(idx);
			Object v2 = p2.get(idx);
			if(v1 instanceof Integer) {
				if(v2 instanceof Integer) {
					int cmp = ((Integer)v1).compareTo((Integer)v2);
					if(cmp != 0)
						return cmp;
					continue;
				}
				return -1;
			}

			if(v2 instanceof Integer)
				return 1;

			int cmp = ((String)v1).compareTo((String)v2);
			if(cmp != 0)
				return cmp;
		}
		return p1Size - p2Size;
	}

	public static Version create(int major, int minor, int patch) {
		return create(major, minor, patch, null, null);
	}

	public static Version create(int major, int minor, int patch, String preRelease) {
		return create(major, minor, patch, preRelease, null);
	}

	public static Version create(int major, int minor, int patch, String preRelease, String build) {
		if(major < 0 || minor < 0 || patch < 0)
			throw new IllegalArgumentException("Negative numbers not accepted in version");

		return instanceCache.cache(
				new Version(major, minor, patch,
						splitParts("pre-release", preRelease),
						splitParts("build", build)));
	}

	private static List<Comparable<?>> splitParts(String tag, String parts) {
		if(parts == null || parts.length() == 0)
			return null;

		if(!PARTS_PATTERN.matcher(parts).matches())
			throw new IllegalArgumentException("Illegal characters in " + tag);

		int dotIdx = parts.indexOf('.');
		if(dotIdx < 0)
			return Collections.singletonList(parts);

		List<Comparable<?>> result = new ArrayList<>();
		int start = 0;
		final int end = parts.length();
		for(;;) {
			String part = parts.substring(start, dotIdx);
			result.add(isNumber(part) ? Integer.parseUnsignedInt(part) : part);
			start = dotIdx + 1;
			if(start >= end)
				return result;
			dotIdx = parts.indexOf('.', start);
			if(dotIdx < 0)
				dotIdx = end;
		}
	}

	boolean tripletEquals(Version version) {
		return major == version.major && minor == version.minor && patch == version.patch;
	}

	Version nextPatch() {
		return new Version(major, minor, patch + 1, null, null);
	}

	private final void joinParts(List<?> parts, StringBuilder bld) {
		if(parts == null)
			return;
		int top = parts.size();
		if(top > 0) {
			bld.append(parts.get(0));
			for(int idx = 1; idx < top; ++idx) {
				bld.append('.');
				bld.append(parts.get(idx));
			}
		}
	}

	static boolean isNumber(String number) {
		int idx = number.length();
		if(idx == 0 || idx > 1 && number.charAt(0) == '0')
			return false;

		while(--idx >= 0) {
			char c = number.charAt(idx);
			if(c < '0' || c > '9')
				return false;
		}
		return true;
	}

	/**
	 * Creates a new instance from the given <code>version</code> string. This method will return <code>null</code> on
	 * <code>null</code>
	 * input.
	 *
	 * @param version The version in string form
	 * @return The created version.
	 * @throws IllegalArgumentException if the version string is not a valid SemVer version.
	 */
	public static Version create(String version) throws IllegalArgumentException {
		if(version == null || version.length() == 0)
			return null;

		Matcher m = VERSION_PATTERN.matcher(version);
		if(m.matches())
			return fromMatch(m);
		throw new IllegalArgumentException("The string '" + version + "' does not represent a valid semantic version");
	}

	/**
	 * Creates a new instance from the given <code>version</code> string. This method will return <code>null</code> on
	 * <code>null</code> or
	 * invalid input.
	 *
	 * @param version The version in string form
	 * @return The created version.
	 */
	public static Version fromStringOrNull(String version) {
		if(version == null || version.length() == 0)
			return null;

		Matcher m = VERSION_PATTERN.matcher(version);
		return m.matches() ? fromMatch(m) : null;
	}

	/**
	 * Checks if the given <code>version</code> is a valid SemVer version.
	 *
	 * @param version The version to check. Passing <code>null</code> yields a response of <code>false</code>.
	 * @return <code>true</code> if the given <code>version</code> is valid.
	 */
	public static boolean isValid(String version) {
		return version != null && VERSION_PATTERN.matcher(version).matches();
	}

	@Override
	public int compareTo(Version o) {
		int cmp = major - o.major;
		if(cmp == 0) {
			cmp = minor - o.minor;
			if(cmp == 0) {
				cmp = patch - o.patch;
				if(cmp == 0)
					cmp = comparePreReleases(preRelease, o.preRelease);
			}
		}
		return cmp;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof Version) {
			Version v = (Version)o;
			return major == v.major && minor == v.minor && patch == v.patch
					&& Objects.equals(preRelease, v.preRelease)
					&& Objects.equals(build, v.build);
		}
		return false;
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPatch() {
		return patch;
	}

	public String getPreRelease() {
		if(preRelease == null)
			return null;
		StringBuilder bld = new StringBuilder();
		joinParts(preRelease, bld);
		return bld.toString();
	}

	public String getBuild() {
		if(build == null)
			return null;
		StringBuilder bld = new StringBuilder();
		joinParts(build, bld);
		return bld.toString();
	}

	@Override
	public int hashCode() {
		int hash = major;
		hash = 31 * hash + minor;
		hash = 31 * hash + patch;
		if(preRelease != null)
			hash = 31 * hash + preRelease.hashCode();
		if(build != null)
			hash = 31 * hash + build.hashCode();
		return hash;
	}

	public boolean isStable() {
		return preRelease == null;
	}

	@Override
	public String toString() {
		StringBuilder bld = new StringBuilder();
		toString(bld);
		return bld.toString();
	}

	public void toString(StringBuilder bld) {
		bld.append(major);
		bld.append('.');
		bld.append(minor);
		bld.append('.');
		bld.append(patch);
		if(preRelease != null) {
			bld.append('-');
			joinParts(preRelease, bld);
		}
		if(build != null) {
			bld.append('+');
			joinParts(build, bld);
		}
	}

	// Parse a string that is known to consists of only digits (stems from regexp group)
	private static int parseInt(String g) {
		int top = g.length();
		int val = 0;
		for(int idx = 0; idx < top; ++idx)
			val = val * 10 + (g.charAt(idx) - '0');
		return val;
	}

	private static Version fromMatch(Matcher m) {
		return instanceCache.cache(new Version(parseInt(m.group(1)), parseInt(m.group(2)), parseInt(m.group(3)),
				splitParts("pre-release", m.group(4)), splitParts("build", m.group(5))));
	}
}
