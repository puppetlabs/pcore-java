package com.puppet.pcore.impl;

import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;

import java.net.URI;

import static com.puppet.pcore.impl.types.TypeFactory.patternType;
import static com.puppet.pcore.impl.types.TypeFactory.regexpType;
import static java.util.regex.Pattern.compile;

public final class Constants {
	public static final String KEY_NAME = "name";
	public static final String KEY_NAME_AUTHORITY = "name_authority";
	public static final String KEY_VERSION = "version";
	public static final String KEY_VERSION_RANGE = "version_range";
	public static final String KEY_ATTRIBUTES = "attributes";
	public static final String KEY_TYPE_PARAMETERS = "type_parameters";
	public static final String KEY_CHECKS = "checks";
	public static final String KEY_EQUALITY = "equality";
	public static final String KEY_EQUALITY_INCLUDE_TYPE = "equality_include_type";
	public static final String KEY_FINAL = "final";
	public static final String KEY_FUNCTIONS = "functions";
	public static final String KEY_OVERRIDE = "override";
	public static final String KEY_PARENT = "parent";
	public static final String KEY_REFERENCES = "references";
	public static final String KEY_TYPE = "type";
	public static final String KEY_TYPES = "types";
	public static final String KEY_KIND = "kind";
	public static final String KEY_VALUE = "value";
	public static final String KEY_ANNOTATIONS = "annotations";
	public static final String KEY_SERIALIZATION = "serialization";

	public static final String KEY_PCORE_URI = "pcore_uri";
	public static final String KEY_PCORE_VERSION = "pcore_version";

	public static final URI PCORE_URI = URI.create("http://puppet.com/2016.1/pcore");
	public static final Version PCORE_VERSION = Version.create(1, 0, 0);
	public static final VersionRange PCORE_PARSABLE_VERSIONS = VersionRange.exact(PCORE_VERSION);
	public static final URI RUNTIME_NAME_AUTHORITY = URI.create("http://puppet.com/2016.1/runtime");

	public static final AnyType TYPE_QUALIFIED_REFERENCE = patternType(regexpType(compile("\\A[A-Z][\\w]*(?:::[A-Z][\\w]*)*\\z")));

	/**
	 * Regular expression for URI.
	 *
	 * @see <a href="https://tools.ietf.org/html/rfc3986#page-50">Uniform Resource Identifiers (URI): Generic Syntax</a>
	 */
	public static final AnyType TYPE_URI = patternType(regexpType(compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?")));

	public static final AnyType TYPE_SIMPLE_TYPE_NAME = patternType(regexpType(compile("\\A[A-Z]\\w*\\z")));

	public static final Object[] EMPTY_ARRAY = new Object[0];
}
