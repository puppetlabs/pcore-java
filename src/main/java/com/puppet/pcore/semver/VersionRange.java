/*
  Copyright (c) 2013 Puppet Labs, Inc. and other contributors, as listed below.
  All rights reserved. This program and the accompanying materials
  are made available under the terms of the Eclipse Public License v1.0
  which accompanies this distribution, and is available at
  http://www.eclipse.org/legal/epl-v10.html
  <p>
  Contributors:
  Puppet Labs
 */
package com.puppet.pcore.semver;

import com.puppet.pcore.impl.MergableRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.asList;

/**
 * <p>
 * This class represents an set of min/max ranges of semantic versions. The min/max range can be inclusive or
 * non-inclusive at both ends.
 * </p>
 * A VersionRange can also be created from a string. The string is parsed according to the following rules:
 * <ul>
 * <li>1.2.3 — A specific version.</li>
 * <li>&gt;1.2.3 — Greater than a specific version.</li>
 * <li>&lt;1.2.3 — Less than a specific version.</li>
 * <li>&gt;=1.2.3 — Greater than or equal to a specific version.</li>
 * <li>&lt;=1.2.3 — Less than or equal to a specific version.</li>
 * <li>&gt;=1.0.0 &lt;2.0.0 — Range of versions; both conditions must be satisfied. (This example would match 1.0.1
 * but not 2.0.1)</li>
 * <li>1.x — A semantic major version. (This example would match 1.0.1 but not 2.0.1, and is shorthand for &gt;=1.0.0
 * &lt;2.0.0-)</li>
 * <li>1.2.x — A semantic major &amp; minor version. (This example would match 1.2.3 but not 1.3.0, and is shorthand
 * for &gt;=1.2.0
 * &lt;1.3.0-)</li>
 * <li>* — Matches any version</li>
 * </ul>
 * <p>
 * A range specifier starting with a tilde ~ character is matched against a version in the following fashion:
 * <ul>
 * <li>The version must be at least as high as the range.</li>
 * <li>The version must be less than the next minor revision above the range, or major revision
 * if minor isn't given.</li>
 * </ul>
 * For example, the following are equivalent:
 * <table border="1">
 *   <tr>
 *     <td>~1.2.3</td>
 *     <td>&gt;=1.2.3 &lt;1.3.0</td>
 *   </tr>
 *   <tr>
 *     <td>~1.2</td>
 *     <td>&gt;=1.2.0 &lt;1.3.0</td>
 *   </tr>
 *   <tr>
 *     <td>~1</td>
 *     <td>&gt;=1.0.0 &lt;2.0.0</td>
 *   </tr>
 * </table>
 * </p>
 * <p>
 * A range specifier starting with a caret ^ character is matched against a version in the following fashion:
 * <ul>
 * <li>If the major number is zero, then apply the same rules as for the tilde character.</li>
 * <li>For major numbers above zero, the version must be less than the next major revision above the range.</li>
 * </ul>
 * For example, the following are equivalent:
 * <table border="1">
 *   <tr>
 *     <td>^0.2.3</td>
 *     <td>&gt;=0.2.3 &lt;0.3.0</td>
 *   </tr>
 *   <tr>
 *     <td>^1.2.3</td>
 *     <td>&gt;=1.2.3 &lt;2.0.0</td>
 *   </tr>
 *   <tr>
 *     <td>^1.2</td>
 *     <td>&gt;=1.2.0 &lt;2.0.0</td>
 *   </tr>
 *   <tr>
 *     <td>^1</td>
 *     <td>&gt;=1.0.0 &lt;2.0.0</td>
 *   </tr>
 * </table>
 * </p>
 * <p>
 * A set of ranges separated by &lt;white space&gt;||&lt;white space&gt; forms a VersionRange that matches versions
 * matching at least one of the ranges in the set. Example:
 * <ul>
 * <li>>=1.0.0 <2.0.0 || >=2.3.0 <3.0.0</li>
 * <li>>~1.2 || ~1.4 || 2</li>
 * </ul>
 * </p>
 */
public class VersionRange implements MergableRange<VersionRange>, Serializable {
	private static final String NR = "0|[1-9][0-9]*";
	private static final String XR = "(x|X|\\*|" + NR + ")";

	private static final String PART = "(?:[0-9A-Za-z-]+)";
	private static final String PARTS = PART + "(?:\\." + PART + ")*";
	private static final String QUALIFIER = "(?:-(" + PARTS + "))?(?:\\+(" + PARTS + "))?";

	private static final String PARTIAL = XR + "(?:\\." + XR + "(?:\\." + XR + QUALIFIER + ")?)?";

  // The ~> isn"t in the spec but allowed
  private static final String SIMPLE = "([<>=~^]|<=|>=|~>|~=)?(?:" + PARTIAL + ")";
	private static final Pattern SIMPLE_PATTERN = Pattern.compile("\\A" + SIMPLE + "\\z");


	private static final Pattern OR_SPLIT = Pattern.compile("\\s*\\|\\|\\s*");
	private static final Pattern SIMPLE_SPLIT = Pattern.compile("\\s+");

	private static final Pattern OP_WS_PATTERN = Pattern.compile("([><=~^])(?:\\s+|\\s*v)");

	private static final String HYPHEN = "(?:" + PARTIAL + ")\\s+-\\s+(?:" + PARTIAL + ")";
	private static final Pattern HYPHEN_PATTERN = Pattern.compile("\\A" + HYPHEN + "\\z");

	private static final SimpleRange LOWEST_LB = new GtEqRange(Version.MIN);
	private static final SimpleRange HIGHEST_LB = new GtRange(Version.MAX);
	private static final SimpleRange LOWEST_UB = new LtRange(Version.MIN);

	public static final VersionRange ALL_INCLUSIVE = new VersionRange("*", Collections.singletonList(LOWEST_LB));
	public static final VersionRange EMPTY_RANGE = new VersionRange("<0.0.0", asList(LOWEST_UB));

	private static final long serialVersionUID = 1L;

	private final List<AbstractRange> ranges;
	private final String originalString;

	private VersionRange(String originalString, List<AbstractRange> ranges) {
		boolean mergeHappened = true;
		while(ranges.size() > 1 && mergeHappened) {
			mergeHappened = false;
			List<AbstractRange> result = new ArrayList<>();
			while(ranges.size() > 1) {
				List<AbstractRange> unmerged = new ArrayList<>();
				AbstractRange x = ranges.remove(ranges.size() - 1);
				for(AbstractRange y : ranges) {
					AbstractRange merged = x.union(y);
					if(merged == null)
						unmerged.add(y);
					else {
						mergeHappened = true;
						x = merged;
					}
				}
				result.add(x);
				ranges = unmerged;
			}
			if(!ranges.isEmpty())
				result.add(ranges.get(0));
      Collections.reverse(result);
			ranges = result;
		}
		if(ranges.isEmpty())
			ranges = asList(LOWEST_UB);
		this.ranges = ranges;
		this.originalString = originalString;
	}

	public boolean isAbove(Version v) {
		for(AbstractRange range : ranges)
			if(!range.isAbove(v))
				return false;
		return true;
	}

	public boolean isBelow(Version v) {
		for(AbstractRange range : ranges)
			if(!range.isBelow(v))
				return false;
		return true;
	}

	/**
	 * Creates a new VersionRange according to detailed specification.
	 *
	 * @param lower               the lower version
	 * @param lowerBoundInclusive true if lower version is included in the range
	 * @param upper               the upper version
	 * @param upperBoundInclusive true if upper version is included in the range
	 * @return the created range
	 */
	public static VersionRange create(
			Version lower, boolean lowerBoundInclusive, Version upper, boolean
			upperBoundInclusive) {
		return new VersionRange(null, asList(
				(lowerBoundInclusive ? new GtEqRange(lower) : new GtRange(lower)).intersection(
						upperBoundInclusive ? new LtEqRange(upper) : new LtRange(upper))));

	}

	/**
	 * Returns a range that will be an exact match for the given version.
	 *
	 * @param version The version that the range must match
	 * @return The created range
	 */
	public static VersionRange exact(Version version) {
		return version == null ? null : new VersionRange(null, asList(new EqRange(version)));
	}

	private static Integer xDigit(String str) {
		if(str == null || "x".equals(str) || "X".equals(str) || "*".equals(str))
			return null;
		if(Version.isNumber(str))
			return Integer.parseInt(str);
		throw new IllegalArgumentException("Illegal version triplet");
	}

	private static abstract class AbstractRange {

		abstract SimpleRange asLowerBound();

		abstract SimpleRange asUpperBound();

		abstract boolean includes(Version version);

		public abstract boolean isAbove(Version v);

		public abstract boolean isBelow(Version v);

		boolean isOverlap(AbstractRange vr) {
			int cmp = min().compareTo(vr.max());
			if(cmp < 0 || cmp == 0 && !(isExcludeMin() || vr.isExcludeMax())) {
				cmp = vr.min().compareTo(max());
				return cmp < 0 || cmp == 0 && !(vr.isExcludeMin() || isExcludeMax());
			}
			return false;
		}

		/**
		 * Compares the given range with this range and returns <code>true</code>
		 * if this requirement is equally or more restrictive in appointing a range
		 * of versions. More restrictive means that the appointed range equal or
		 * smaller and completely within the range appointed by the other version.
		 *
		 * @param vr The requirement to compare with
		 * @return <tt>true</tt> if this requirement is as restrictive as the argument
		 */
		boolean isAsRestrictiveAs(AbstractRange vr) {
			int cmp = vr.min().compareTo(min());
			if(cmp > 0 || (cmp == 0 && !isExcludeMin() && vr.isExcludeMin()))
				return false;

			cmp = vr.max().compareTo(max());
			return !(cmp < 0 || (cmp == 0 && !isExcludeMax() && vr.isExcludeMax()));
		}

		/**
		 * Merge two ranges so that the result matches the intersection of all matching versions.
		 *
		 * @param range the range to intersect with
		 * @return the intersection between the ranges
		 */
		AbstractRange intersection(AbstractRange range) {
			int cmp = min().compareTo(range.max());
			if(cmp > 0)
				return null;

			if(cmp == 0)
				return isExcludeMin() || range.isExcludeMax() ? null : new EqRange(min());

			cmp = range.min().compareTo(max());
			if(cmp > 0)
				return null;

			if(cmp == 0)
				return range.isExcludeMin() || isExcludeMax() ? null : new EqRange(range.min());

			cmp = min().compareTo(range.min());
			AbstractRange min;
			if(cmp < 0)
				min = range;
			else if(cmp > 0)
				min = this;
			else
				min = isExcludeMin() ? this : range;

			cmp = max().compareTo(range.max());
			AbstractRange max;
			if(cmp > 0)
				max = range;
			else if(cmp < 0)
				max = this;
			else
				max = isExcludeMax() ? this : range;

			if(!max.isUpperBound())
				return min;

			if(!min.isLowerBound())
				return max;

			return new MinMaxRange(min.asLowerBound(), max.asUpperBound());
		}

		/**
		 * Merge two ranges so that the result matches the sum of all matching versions. A merge
		 * is only possible when the ranges are either adjacent or have an overlap.
		 *
		 * @param other the range to merge with
		 * @return the result of the merge
		 */
		AbstractRange union(AbstractRange other) {
			if(includes(other.min()) || other.includes(min())) {
        Version min;
        boolean excl_min;
				int cmp = min().compareTo(other.min());
				if(cmp < 0) {
					min = min();
					excl_min = isExcludeMin();
				} else if(cmp > 0) {
					min = other.min();
					excl_min = other.isExcludeMin();
				} else {
					min = min();
					excl_min = isExcludeMin() && other.isExcludeMin();
				}

				Version max;
				boolean excl_max;
				cmp = max().compareTo(other.max());
				if(cmp > 0) {
					max = max();
					excl_max = isExcludeMax();
				} else if(cmp < 0) {
					max = other.max();
					excl_max = other.isExcludeMax();
				} else {
					max = max();
					excl_max = isExcludeMax() && other.isExcludeMax();
				}
				return new MinMaxRange(
						excl_min ? new GtRange(min) : new GtEqRange(min),
						excl_max ? new LtRange(max) : new LtEqRange(max));
			}
			if(isExcludeMin() && other.isExcludeMin() && min().compareTo(other.min()) == 0)
				return from_to(this, other);
			if(isExcludeMax() && !other.isExcludeMin() && max().compareTo(other.min()) == 0)
				return from_to(this, other);
			if(other.isExcludeMax() && !isExcludeMin() && other.max().compareTo(min()) == 0)
				return from_to(other, this);
			if(!isExcludeMax() && !other.isExcludeMin() && max().nextPatch().compareTo(other.min()) == 0)
				return from_to(this, other);
			if(!other.isExcludeMax() && !isExcludeMin() && other.max().nextPatch().compareTo(min()) == 0)
				return from_to(other, this);
			return null;
		}

		AbstractRange from_to(AbstractRange a, AbstractRange b) {
			return new MinMaxRange(
			  a.isExcludeMin() ? new GtRange(a.min()) : new GtEqRange(a.min()),
			  b.isExcludeMax() ? new LtRange(b.max()) : new LtEqRange(b.max()));
		}

		abstract boolean isExcludeMin();

		abstract boolean isExcludeMax();

		abstract boolean isLowerBound();

		abstract boolean isUpperBound();

		abstract Version min();

		abstract Version max();

		abstract boolean testPrerelease(Version version);

		abstract public void toString(StringBuilder bld);
	}

	private static class MinMaxRange extends AbstractRange {
		private final SimpleRange minCompare;

		private final SimpleRange maxCompare;

		MinMaxRange(SimpleRange minCompare, SimpleRange maxCompare) {
			this.minCompare = minCompare;
			this.maxCompare = maxCompare;
		}

		@Override
		SimpleRange asLowerBound() {
			return minCompare;
		}

		@Override
		SimpleRange asUpperBound() {
			return maxCompare;
		}

		@Override
		boolean includes(Version version) {
			return minCompare.includes(version) && maxCompare.includes(version);
		}

		@Override
		public boolean isAbove(Version v) {
			return minCompare.isAbove(v);
		}

		@Override
		public boolean isBelow(Version v) {
			return maxCompare.isBelow(v);
		}

		@Override
		boolean isExcludeMin() {
			return minCompare.isExcludeMin();
		}

		@Override
		Version min() {
			return minCompare.min();
		}

		@Override
		public boolean equals(Object other) {
			if(other instanceof MinMaxRange) {
				MinMaxRange o = (MinMaxRange)other;
				return minCompare.equals(o.minCompare) && maxCompare.equals(o.maxCompare);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return minCompare.hashCode() ^ maxCompare.hashCode();
		}

		@Override
		boolean isExcludeMax() {
			return maxCompare.isExcludeMax();
		}

		@Override
		boolean isLowerBound() {
			return minCompare.isLowerBound();
		}

		@Override
		boolean isUpperBound() {
			return maxCompare.isUpperBound();
		}

		@Override
		Version max() {
			return maxCompare.max();
		}

		@Override
		boolean testPrerelease(Version version) {
			return minCompare.testPrerelease(version) || maxCompare.testPrerelease(version);
		}

		@Override
		public void toString(StringBuilder bld) {
			minCompare.toString(bld);
			bld.append(' ');
			maxCompare.toString(bld);
		}
	}

	private static abstract class SimpleRange extends AbstractRange {
		final Version version;

		SimpleRange(Version version) {
			this.version = version;
		}

		@Override
		SimpleRange asLowerBound() {
			return HIGHEST_LB;
		}

		@Override
		public boolean equals(Object other) {
			return getClass().equals(other.getClass()) && ((SimpleRange)other).version.equals(version);
		}

		@Override
		public int hashCode() {
			return getClass().hashCode() * 31 + version.hashCode();
		}

		@Override
		boolean isUpperBound() {
			return false;
		}

		@Override
		boolean isLowerBound() {
			return false;
		}

		@Override
		SimpleRange asUpperBound() {
			return LOWEST_UB;
		}

		@Override
		boolean isExcludeMin() {
			return false;
		}

		@Override
		boolean isExcludeMax() {
			return false;
		}

		@Override
		public boolean isAbove(Version v) {
			return false;
		}

		@Override
		public boolean isBelow(Version v) {
			return false;
		}

		@Override
		Version min() {
			return Version.MIN;
		}

		@Override
		Version max() {
			return Version.MAX;
		}

		@Override
		boolean testPrerelease(Version version) {
			return !this.version.isStable() && this.version.tripletEquals(version);
		}
	}

	private static class GtRange extends SimpleRange {
		GtRange(Version version) {
			super(version);
		}

		@Override
		boolean includes(Version version) {
			return version.compareTo(this.version) > 0;
		}

		@Override
		public boolean isAbove(Version v) {
			if(this.version.isStable())
				v = v.toStable();
			return this.version.compareTo(v) >= 0;
		}

		@Override
		SimpleRange asLowerBound() {
			return this;
		}

		@Override
		boolean isExcludeMin() {
			return true;
		}

		@Override
		boolean isLowerBound() {
			return true;
		}

		@Override
		Version min() {
			return version;
		}

		@Override
		public void toString(StringBuilder bld) {
			bld.append('>');
			version.toString(bld);
		}
	}

	private static class GtEqRange extends SimpleRange {
		GtEqRange(Version version) {
			super(version);
		}

		@Override
		SimpleRange asLowerBound() {
			return this;
		}

		@Override
		boolean includes(Version version) {
			return version.compareTo(this.version) >= 0;
		}

		@Override
		public boolean isAbove(Version v) {
			return this.version.compareTo(v) > 0;
		}

		@Override
		boolean isLowerBound() {
			return !version.equals(Version.MIN);
		}

		@Override
		Version min() {
			return version;
		}

		@Override
		public void toString(StringBuilder bld) {
			bld.append(">=");
			version.toString(bld);
		}
	}

	private static class LtRange extends SimpleRange {
		LtRange(Version version) {
			super(version);
		}

		@Override
		SimpleRange asUpperBound() {
			return this;
		}

		@Override
		boolean includes(Version version) {
			return version.compareTo(this.version) < 0;
		}

		@Override
		public boolean isBelow(Version v) {
			if(this.version.isStable())
				v = v.toStable();
			return this.version.compareTo(v) <= 0;
		}

		@Override
		boolean isExcludeMax() {
			return true;
		}

		@Override
		boolean isUpperBound() {
			return true;
		}

		@Override
		Version max() {
			return version;
		}

		@Override
		public void toString(StringBuilder bld) {
			bld.append('<');
			version.toString(bld);
		}
	}

	private static class LtEqRange extends SimpleRange {
		LtEqRange(Version version) {
			super(version);
		}

		@Override
		SimpleRange asUpperBound() {
			return this;
		}

		@Override
		boolean includes(Version version) {
			return version.compareTo(this.version) <= 0;
		}

		@Override
		public boolean isBelow(Version v) {
			return this.version.compareTo(v) < 0;
		}

		@Override
		boolean isUpperBound() {
			return !version.equals(Version.MAX);
		}

		@Override
		Version max() {
			return version;
		}

		@Override
		public void toString(StringBuilder bld) {
			bld.append("<=");
			version.toString(bld);
		}
	}

	private static class EqRange extends SimpleRange {
		EqRange(Version version) {
			super(version);
		}

		@Override
		SimpleRange asLowerBound() {
			return this;
		}

		@Override
		SimpleRange asUpperBound() {
			return this;
		}

		@Override
		boolean includes(Version version) {
			return this.version.compareTo(version) == 0;
		}

		@Override
		public boolean isAbove(Version v) {
			return this.version.compareTo(v) > 0;
		}

		@Override
		public boolean isBelow(Version v) {
			return this.version.compareTo(v) < 0;
		}

		@Override
		boolean isLowerBound() {
			return !version.equals(Version.MIN);
		}

		@Override
		boolean isUpperBound() {
			return !version.equals(Version.MAX);
		}

		@Override
		Version min() {
			return version;
		}

		@Override
		Version max() {
			return version;
		}

		@Override
		public void toString(StringBuilder bld) {
			version.toString(bld);
		}
	}

	private static AbstractRange createGtEqRange(Matcher matcher, int startInMatcher) {
		Integer major = xDigit(matcher.group(startInMatcher++));
		if(major == null)
			return LOWEST_LB;

		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			minor = 0;

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			patch = 0;

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return new GtEqRange(Version.create(major, minor, patch, preRelease, build));
	}

	private static AbstractRange createGtRange(Matcher matcher, int startInMatcher) {
		Integer major = xDigit(matcher.group(startInMatcher++));
		if(major == null)
			return LOWEST_LB;

		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			return new GtEqRange(Version.create(major + 1, 0, 0));

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			return new GtEqRange(Version.create(major, minor + 1, 0));

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return new GtRange(Version.create(major, minor, patch, preRelease, build));
	}

	private static AbstractRange createLtEqRange(Matcher matcher, int startInMatcher) {
		Integer major = xDigit(matcher.group(startInMatcher++));
		if(major == null)
			return LOWEST_UB;

		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			return new LtRange(Version.create(major + 1, 0, 0));

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			return new LtRange(Version.create(major, minor + 1, 0));

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return new LtEqRange(Version.create(major, minor, patch, preRelease, build));
	}

	private static AbstractRange createLtRange(Matcher matcher, int startInMatcher) {
		Integer major = xDigit(matcher.group(startInMatcher++));
		if(major == null)
			return LOWEST_UB;

		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			minor = 0;

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			patch = 0;

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return new LtRange(Version.create(major, minor, patch, preRelease, build));
	}

	private static AbstractRange createTildeRange(Matcher matcher, int startInMatcher) {
		return allowPatchUpdates(matcher, startInMatcher, true);
	}

	private static AbstractRange createCaretRange(Matcher matcher, int startInMatcher) {
		Integer major = xDigit(matcher.group(startInMatcher));
		if(major == null)
			return LOWEST_LB;
		return major == 0 ? allowPatchUpdates(matcher, startInMatcher, true) : allowMinorUpdates(matcher, major, ++startInMatcher);
	}

	private static AbstractRange createXRange(Matcher matcher, int startInMatcher) {
		return allowPatchUpdates(matcher, startInMatcher, false);
	}

	private static AbstractRange allowPatchUpdates(Matcher matcher, int startInMatcher, boolean tildeOrCaret) {
		Integer major = xDigit(matcher.group(startInMatcher++));
		if(major == null)
			return LOWEST_LB;

		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			return new MinMaxRange(
					new GtEqRange(Version.create(major, 0, 0)),
					new LtRange(Version.create(major + 1, 0, 0)));

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			return new MinMaxRange(
					new GtEqRange(Version.create(major, minor, 0)),
					new LtRange(Version.create(major, minor + 1, 0)));

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return tildeOrCaret ? new MinMaxRange(
				new GtEqRange(Version.create(major, minor, patch, preRelease, build)),
				new LtRange(Version.create(major, minor + 1, 0)))
				: new EqRange(Version.create(major, minor, patch, preRelease, build));
	}

	private static AbstractRange allowMinorUpdates(Matcher matcher, int major, int startInMatcher) {
		Integer minor = xDigit(matcher.group(startInMatcher++));
		if(minor == null)
			minor = 0;

		Integer patch = xDigit(matcher.group(startInMatcher++));
		if(patch == null)
			patch = 0;

		String preRelease = matcher.group(startInMatcher++);
		String build = matcher.group(startInMatcher);
		return new MinMaxRange(
				new GtEqRange(Version.create(major, minor, patch, preRelease, build)),
				new LtRange(Version.create(major + 1, 0, 0)));
	}

	/**
	 * Returns a range based on the given string. See class documentation
	 * for details.
	 *
	 * @param versionRequirement The string form of the version requirement
	 * @return The created range
	 */
	public static VersionRange create(String versionRequirement) {
		if(versionRequirement == null)
			return null;

		Matcher m = OP_WS_PATTERN.matcher(versionRequirement);
		if(m.find()) {
			StringBuffer sb = new StringBuffer();
			do
				m.appendReplacement(sb, m.group(1));
			while(m.find());
			m.appendTail(sb);
			versionRequirement = sb.toString();
		}

		String[] rangeStrings = OR_SPLIT.split(versionRequirement);
		if(rangeStrings.length == 0)
			return ALL_INCLUSIVE;

		List<AbstractRange> ranges = new ArrayList<>();
		for(String rangeStr : rangeStrings) {
			if(rangeStr.isEmpty()) {
				ranges.add(LOWEST_LB);
				continue;
			}

			m = HYPHEN_PATTERN.matcher(rangeStr);
			if(m.matches()) {
				ranges.add(createGtEqRange(m, 1).intersection(createLtEqRange(m, 6)));
				continue;
			}

			AbstractRange simpleRange = null;
			for(String simple : SIMPLE_SPLIT.split(rangeStr)) {
				m = SIMPLE_PATTERN.matcher(simple);
				if(!m.matches())
					throw vomit('\'' + simple + "' is not a valid version range", versionRequirement);
				String operator = m.group(1);
				AbstractRange range;
				switch(operator == null ? "=" : operator) {
				case "~":
				case "~>":
					range = createTildeRange(m, 2);
					break;
				case "^":
					range = createCaretRange(m, 2);
					break;
				case ">":
					range = createGtRange(m, 2);
					break;
				case ">=":
					range = createGtEqRange(m, 2);
					break;
				case "<":
					range = createLtRange(m, 2);
					break;
				case "<=":
					range = createLtEqRange(m, 2);
					break;
				default:
					range = createXRange(m, 2);
				}
				if(simpleRange == null)
					simpleRange = range;
				else
					simpleRange = simpleRange.intersection(range);
			}
			if(simpleRange != null)
				ranges.add(simpleRange);
		}
		return new VersionRange(versionRequirement, ranges);
	}

	/**
	 * Returns a range that will match versions greater than the given version.
	 *
	 * @param version The version that serves as the non inclusive lower bound
	 * @return The created range
	 */
	public static VersionRange greater(Version version) {
		return version == null ? null : new VersionRange(null, asList(new GtRange(version)));
	}

	/**
	 * Returns a range that will match versions greater than or equal the given version.
	 *
	 * @param version The version that serves as the inclusive lower bound
	 * @return The created range
	 */
	public static VersionRange greaterOrEqual(Version version) {
		return version == null ? null : new VersionRange(null, asList(new GtEqRange(version)));
	}

	/**
	 * Returns a range that will match versions less than the given version.
	 *
	 * @param version The version that serves as the non inclusive upper bound
	 * @return The created range
	 */
	public static VersionRange less(Version version) {
		return version == null ? null : new VersionRange(null, asList(new LtRange(version)));
	}

	/**
	 * Returns a range that will match versions less than or equal to the given version.
	 *
	 * @param version The version that serves as the non inclusive upper bound
	 * @return The created range
	 */
	public static VersionRange lessOrEqual(Version version) {
		return version == null ? null : new VersionRange(null, asList(new LtEqRange(version)));
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VersionRange && ((VersionRange)o).ranges.equals(ranges);
	}

	/**
	 * Scans the provided collection of candidates and returns the highest version
	 * that is included in this range.
	 *
	 * @param candidateVersions The collection of candidate versions
	 * @return The best match or <tt>null</tt> if no match was found
	 */
	public Version findBestMatch(Iterable<Version> candidateVersions) {
		Version best = null;
		for(Version candidate : candidateVersions)
			if((best == null || candidate.compareTo(best) > 0) && includes(candidate))
				best = candidate;
		return best;
	}

	/**
	 * Returns the end version of the range.
	 *
	 * Since this really is an OR between disparate ranges, it may have multiple ends. This method
	 * returns <code>null</code> if that is the case.
	 *
	 * @return the version that represents the end of the range, or <code>null</code> if there are multiple ends.
	 */
	public Version getMaxVersion() {
		return ranges.size() == 1 ? ranges.get(0).max() : null;
	}

	/**
	 * Returns the beginning version of the range.
	 *
	 * Since this really is an OR between disparate ranges, it may have multiple beginnings. This method
	 * returns <code>null</code> if that is the case.
	 *
	 * @return the version that represents the beginning of the range, or <code>null</code> if there are multiple beginnings.
	 */
	public Version getMinVersion() {
		return ranges.size() == 1 ? ranges.get(0).min() : null;
	}

	@Override
	public int hashCode() {
		return ranges.hashCode();
	}

	public VersionRange intersection(VersionRange other) {
    ArrayList<AbstractRange> result = new ArrayList<>();
    for(AbstractRange range : ranges) {
      for(AbstractRange o_range : other.ranges) {
	      AbstractRange is = range.intersection(o_range);
	      if(is != null && !result.contains(is))
	      	result.add(is);
      }
    }
    return result.isEmpty() ? null : new VersionRange(null, result);
	}

	/**
	 * Compares the given range with this range and returns true
	 * if this requirement is equally or more restrictive in appointing a range
	 * of versions. More restrictive means that the appointed range equal or
	 * smaller and completely within the range appointed by the other version.
	 *
	 * @param vr The requirement to compare with
	 * @return <tt>true</tt> if this requirement is as restrictive as the argument
	 */
	public boolean isAsRestrictiveAs(VersionRange vr) {
		// No range in ranges intersect so in order to be more restrictive, each
		// range must have an intersecting range in vr and the range causing the
		// intersection must be more restrictive than the vr range.
		for(AbstractRange range : ranges) {
			boolean foundMatch = false;
			for(AbstractRange o_range : vr.ranges) {
				AbstractRange is = range.intersection(o_range);
				if(is != null && range.isAsRestrictiveAs(o_range))
					foundMatch = true;
			}
			if(!foundMatch)
				return false;
		}
		return true;
	}

	/**
	 * Checks if <tt>version</tt> is included in this range.
	 *
	 * @param version the version to test.
	 * @return <tt>true</tt> if the version is include. <tt>false</tt> if the version
	 * was <tt>null</tt> or not included in this range.
	 */
	public boolean includes(Version version) {
		if(version == null)
			return false;
		for(AbstractRange range : ranges)
			if(range.includes(version) && (version.isStable() || range.testPrerelease(version)))
				return true;
		return false;
	}

	/**
	 * Returns <code>true</code> if the beginning version is excluded from the range.
	 *
	 * Since this really is an OR between disparate ranges, it may have multiple beginnings. This method
	 * returns <code>null</code> if that is the case.
	 *
	 * @return <code>true</code> if the beginning is excluded from the range, <code>false</code> if not,
	 *   or <code>null</code> if there are multiple beginnings.
	 */
	public Boolean isExcludeBegin() {
		return ranges.size() == 1 ? ranges.get(0).isExcludeMin() : null;
	}

	/**
	 * Returns true if the end version is excluded from the range.
	 *
	 * Since this really is an OR between disparate ranges, it may have multiple ends. This method
	 * returns `nil` if that is the case.
	 *
	 * @return <code>true</code> if the end is excluded from the range, <code>false</code> if not,
	 *   or <code>null</code> if there are multiple ends.
	 */
	public Boolean isExcludeEnd() {
		return ranges.size() == 1 ? ranges.get(0).isExcludeMax() : null;
	}

	@Override
	public boolean isOverlap(VersionRange o) {
		for(AbstractRange range : ranges)
			for(AbstractRange o_range : o.ranges)
				if(range.isOverlap(o_range))
					return true;
		return false;
	}

	@Override
	public VersionRange merge(VersionRange other) {
		ArrayList<AbstractRange> allRanges = new ArrayList<>();
		allRanges.addAll(ranges);
		allRanges.addAll(other.ranges);
		return new VersionRange(null, allRanges);
	}

	public String toString() {
		return originalString == null ? toNormalizedString() : originalString;
	}

	public String toNormalizedString() {
		StringBuilder bld = new StringBuilder();
		toNormalizedString(bld);
		return bld.toString();
	}

	public void toNormalizedString(StringBuilder bld) {
		int top = ranges.size();
		ranges.get(0).toString(bld);
		for(int idx = 1; idx < top; ++idx) {
			bld.append(" || ");
			ranges.get(idx).toString(bld);
		}
	}

	private static IllegalArgumentException vomit(String reason, String range) {
		return new IllegalArgumentException(reason + " in range '" + range + '\'');
	}
}
