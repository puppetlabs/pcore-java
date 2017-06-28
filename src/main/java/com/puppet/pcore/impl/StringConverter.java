package com.puppet.pcore.impl;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Default;
import com.puppet.pcore.impl.types.*;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.time.DurationFormat;
import com.puppet.pcore.time.InstantFormat;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.*;
import static com.puppet.pcore.impl.Polymorphic.initPolymorphicDispatch;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.lang.String.format;
import static java.util.Collections.singletonMap;

public class StringConverter extends Polymorphic<String> {
	static class StringFormatException extends IllegalArgumentException {
		StringFormatException(String typeString, char actual, String expected) {
			super(format("Illegal format '%c' specified for value of %s type - expected one of the characters '%s'", actual, typeString, expected));
		}
	}

	static class Indentation {
		final int level;
		final boolean first;
		final boolean isIndending;
		final char[] padding;

		Indentation(int level, boolean first, boolean isIndending) {
			this.level = level;
			this.first = first;
			this.isIndending = isIndending;
			this.padding = new char[level*2];
			Arrays.fill(padding, ' ');
		}

		Indentation subsequent() {
			return first ? new Indentation(level, false, isIndending) : this;
		}

		Indentation indenting(boolean isIndenting) {
			return this.isIndending == isIndenting ? this : new Indentation(level, first, isIndenting);
		}

		Indentation increase(boolean isIndenting) {
			return new Indentation(level + 1, true, isIndenting);
		}

		boolean breaks() {
			return isIndending && level > 0 && !first;
		}
	}

	static class Format {
		/**
		 * Alternate form (varies in meaning
		 */
		final boolean isAlt;

		/**
		 * Optional width of field > 0
		 */
		final Integer width;

		/**
		 * Optional precision
		 */
		final Integer prec;

		/**
		 * Character denoting the format
		 */
		final char fmt;

		/**
		 * space, plus, or binary zero (ignore)
		 */
		final char plus;

		/**
		 * left adjust in given width or not
		 */
		final boolean isLeft;

		/**
		 * left_pad with zero instead of space
		 */
		final boolean isZeroPad;

		/**
		 * Delimiters for containers, a "left" char representing the pair <[{(
		 */
		final char leftDelimiter;

		/**
		 * Map of type to format for elements contained in an object this format applies to
		 */
		final Map<AnyType, Format> containerFormats;

		/**
		 * Separator string inserted between elements in a container
		 */
		final String separator;

		/**
		 * Separator string inserted between sub elements in a container
		 */
		final String separator2;

		/**
		 * Original format string
		 */
		final String origFmt;

		static final char[] delimiters = { '[', '{', '(', '<', '|' };
		static final Map<Character, char[]> delimiterPairs = asMap(
				'[', new char[] { '[', ']' },
				'{', new char[] { '{', '}' },
				'(', new char[] { '(', ')' },
				'<', new char[] { '<', '>' },
				'|', new char[] { '|', '|' },
				' ', new char[] { 0, 0 }
		);

		Format(String format) {
			this(format, null, null, null);
		}

		Format(String format, String separator, String separator2, Map<AnyType, Format> containerFormats) {
			Matcher m = StringType.FORMAT_PATTERN.matcher(format);
			if(!m.matches())
				throw new IllegalArgumentException(format("The format '%s' is not a valid format on the form '%%<flags><width>.<prec><format>'", format));

			origFmt = format;
			fmt = m.group(4).charAt(0);
			String flags = m.group(1);
			isLeft = hasDelimOnce(flags, format, '-');
			isAlt = hasDelimOnce(flags, format, '#');
			plus = hasDelimOnce(flags, format, ' ') ? ' ' : (hasDelimOnce(flags, format, '+') ? '+' : 0);
			isZeroPad = hasDelimOnce(flags, format, '0');

			char foundDelim = 0;
			for(char delim : delimiters)
				if(hasDelimOnce(flags, format, delim)) {
					if(foundDelim != 0)
						throw new IllegalArgumentException(format("Only one of the delimiters [ { ( < | can be given in the format flags, got '%s'", format));
					foundDelim = delim;
				}
			leftDelimiter = foundDelim;
			String tmp = m.group(2);
			width = tmp == null ? null : Integer.valueOf(tmp);
			tmp = m.group(3);
			prec = tmp == null ? null : Integer.valueOf(tmp);

			this.separator = separator;
			this.separator2 = separator2;
			this.containerFormats = containerFormats;
		}

		private static boolean hasDelimOnce(String flags, String format, char flag) {
			int flagIndex = flags.indexOf(flag);
			if(flagIndex >= 0) {
				if(flags.indexOf(flag, flagIndex + 1) > flagIndex)
					throw new IllegalArgumentException(format("The same flag can only be used once, got '%s'", format));
				return true;
			}
			return false;
		}

		char[] delimiterPair(char[] defaultDelimiters) {
			char delim = leftDelimiter;
			if(delim == 0 && plus == ' ')
				delim = ' ';
			char[] delims = delimiterPairs.get(delim);
			if(delims == null)
				delims = defaultDelimiters;
			return delims;
		}

		static Format merge(Format lower, Format higher) {
			if(lower == null)
				return higher;
			if(higher == null)
				return lower;
			return lower.merge(higher);
		}

		Format merge(Format other) {
			return new Format(
					other.origFmt,
					other.separator == null ? separator : other.separator,
					other.separator2 == null ? separator2 : other.separator2,
					mergeFormats(containerFormats, other.containerFormats));
		}

		private static int typeRank(AnyType type) {
			if(type instanceof NumericType)
				return 13;
			if(type instanceof StringType)
				return 12;
			if(type instanceof EnumType)
				return 11;
			if(type instanceof PatternType)
				return 10;
			if(type instanceof ArrayType)
				return 4;
			if(type instanceof TupleType)
				return 3;
			if(type instanceof HashType)
				return 2;
			if(type instanceof StructType)
				return 1;
			return 0;
		}

		private static Map<AnyType, Format> mergeFormats(Map<AnyType, Format> lower, Map<AnyType, Format> higher) {
			if(lower == null || lower.isEmpty())
				return higher;
			if(higher == null || higher.isEmpty())
				return lower;
			Map<AnyType, Format> normLower = reject(lower, (le) -> any(higher.keySet(), (hk) -> !hk.equals(le.getKey()) && hk.isAssignable(le.getKey())));

			Map<AnyType, Format> merged = new TreeMap<>((a, b) -> {
				if(a.equals(b))
					return 0;

				boolean ab = b.isAssignable(a);
				boolean ba = a.isAssignable(b);
				if(ab && !ba)
					return -1;
				if(!ab && ba)
					return 1;
				int ra = typeRank(a);
				int rb = typeRank(b);
				return ra == rb
					? a.toString().compareTo(b.toString())
				  : (ra < rb ? -1 : 1);
			});
			for(AnyType k : mergeUnique(normLower.keySet(), higher.keySet())) {
				Format f = merge(normLower.get(k), higher.get(k));
				merged.put(k, f);
			}

			return merged;
		}
	}

	public static final Indentation DEFAULT_INDENTATION = new Indentation(0, true, false);
	public static final Map<AnyType, Format> DEFAULT_CONTAINER_FORMATS = asMap(
			anyType(), new Format("%p")
	);

	public static final Format DEFAULT_ARRAY_FORMAT = new Format("%a", ",", ",", DEFAULT_CONTAINER_FORMATS);
	public static final Format DEFAULT_HASH_FORMAT = new Format("%h", ",", " => ", DEFAULT_CONTAINER_FORMATS);

	public static final char[] DEFAULT_ARRAY_DELMITERS = new char[] { '[', ']' };
	public static final char[] DEFAULT_HASH_DELMITERS = new char[] { '{', '}' };

	public static final Map<AnyType, Format> DEFAULT_FORMATS = asMap(
			objectType(), new Format("%p"),
			floatType(), new Format("%f"),
			numericType(), new Format("%d"),
			arrayType(), DEFAULT_ARRAY_FORMAT,
			hashType(), DEFAULT_HASH_FORMAT,
			binaryType(), new Format("%B"),
			anyType(), new Format("%s")
	);

	public static final StringConverter singleton = new StringConverter();

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(StringConverter.class, "string", 3);

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	public String convert(Object value) {
		return convert(value, Default.SINGLETON);
	}

	public String convert(Object value, Object stringFormats) {
		AnyType valueType = infer(value);
		Map<AnyType, Format> formats = stringFormats instanceof String
		  ? singletonMap(valueType, new Format((String)stringFormats))
		  : (Default.SINGLETON.equals(stringFormats)
				? DEFAULT_FORMATS
				: Format.mergeFormats(DEFAULT_FORMATS, validateFormats(stringFormats)));
		return convert(valueType, value, formats, DEFAULT_INDENTATION);
	}

	private String convert(AnyType valueType, Object value, Map<AnyType, Format> stringFormats, Indentation indent) {
		return dispatch(valueType, value, stringFormats, indent);
	}

	@SuppressWarnings("unchecked")
	Map<AnyType, Format> validateFormats(Object formats) {
		if(formats == null)
			return null;

		if(!(formats instanceof Map<?, ?>))
			throw new IllegalArgumentException(format(
					"string conversion expects a Default value, A String, or a Hash of type to format mappings, got a '%s'",
					formats.getClass().getName()));

		Map<AnyType, Format> result = new LinkedHashMap<>();
		for(Map.Entry<Object, Object> fmt : ((Map<Object, Object>)formats).entrySet()) {
			Object key = fmt.getKey();
			if(key instanceof AnyType) {
				AnyType type = (AnyType)key;
				Object value = fmt.getValue();
				if(value instanceof Map<?,?>)
					result.put(type, validateFormatMap((Map<String, Object>)value));
				else
					result.put(type, new Format((String)value));
			} else
				throw new IllegalArgumentException(format(
						"top level keys in the format hash must be data types, got instance of '%s'", key.getClass().getName()));
		}
		return result;
	}

	Format validateFormatMap(Map<String, Object> formatMap) {
		return new Format(
				(String)formatMap.get("format"),
				(String)formatMap.get("separator"),
				(String)formatMap.get("separator2"),
				validateFormats(formatMap.get("string_formats")));
	}

	private static boolean isContainer(AnyType type) {
		return type instanceof CollectionType || type instanceof TupleType || type instanceof StructType || type instanceof ObjectType;
	}

	private static boolean isArrayOrHash(Object v) {
		return v instanceof List || v instanceof Map;
	}

	String string(AnyType valType, Object val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		return format(f.origFmt.replace('p', 's'), val);
	}

	String string(ArrayType valType, List<?> val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 'a':
		case 's':
		case 'p':
			break;
		default:
			throw new StringFormatException("Array", f.fmt, "asp");
		}

		String sep = f.separator == null ? DEFAULT_ARRAY_FORMAT.separator : f.separator;
		Map<AnyType, Format> containerFormats = f.containerFormats == null ? DEFAULT_CONTAINER_FORMATS : f.containerFormats;
		indent = indent.indenting(f.isAlt || indent.isIndending);
		StringBuilder bld = new StringBuilder();
		if(indent.breaks()) {
			bld.append('\n');
			bld.append(indent.padding);
		}
		char[] delims = f.delimiterPair(DEFAULT_ARRAY_DELMITERS);
		if(delims[0] != 0)
			bld.append(delims[0]);

		Indentation childrenIndent = indent.increase(f.isAlt);

		// Make a first pass to format each element
		final int top = val.size();
		List<String> mapped = new ArrayList<>(top);
		boolean[] hashOrArray = new boolean[top];

		Indentation firstPassIndent = childrenIndent;
		for(int idx = 0; idx < top; ++idx) {
			Object v = val.get(idx);
			hashOrArray[idx] = isArrayOrHash(v);
			if(firstPassIndent.first)
				firstPassIndent = firstPassIndent.subsequent();
			AnyType childType = infer(v);
			mapped.add(convert(childType, v, isContainer(childType) ? formatMap : containerFormats, firstPassIndent));
		}

		boolean szBreak = false;
		if(f.isAlt && f.width != null) {
			int widest = 0;
			for(int idx = 0; idx < top; ++idx) {
				Object v = val.get(idx);
				if(hashOrArray[idx])
					widest = 0;
				else {
					widest += mapped.get(idx).length();
					if(widest > f.width) {
						szBreak = true;
						break;
					}
				}
			}
		}
		Indentation secondPassIndent = childrenIndent;
		for(int idx = 0; idx < top; ++idx) {
			if(secondPassIndent.first) {
				secondPassIndent = secondPassIndent.subsequent();
				// if breaking, indent first element by one
				if(szBreak && !hashOrArray[idx])
					bld.append(' ');
			} else {
				bld.append(sep);
        // if break on each (and breaking will not occur because next is an array or hash)
        // or, if indenting, and previous was an array or hash, then break and continue on next line
        // indented.
				if(!hashOrArray[idx] && (szBreak || f.isAlt && idx > 0 && hashOrArray[idx - 1])) {
					bld.append("\n");
					bld.append(secondPassIndent.padding);
				} else if(!(f.isAlt && hashOrArray[idx]))
					bld.append(' ');
			}
			bld.append(mapped.get(idx));
		}
		if(delims[1] != 0)
			bld.append(delims[1]);
		return bld.toString();
	}

	String string(BinaryType valType, Binary val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		String str;
		switch(f.fmt) {
		case 's':
			str = val.toUTF8String();
			break;
		case 'p':
			str = "Binary('" + val.toString() + "')";
			break;
		case 'b':
			str = val.toMimeString();
			break;
		case 'B':
			str = val.toString();
			break;
		case 'u':
			str = val.toURLSafeString();
			break;
		case 't':
			str = "Binary";
			break;
		case 'T':
			str = "BINARY";
			break;
		default:
			throw new StringFormatException("Binary", f.fmt, "bButTsp");
		}
		if(f.isAlt)
			str = puppetQuote(str);
		return applyStringFlags(f, str);
	}

	String string(BooleanType valType, Boolean val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		String str;
		switch(f.fmt) {
		case 't':
			str = val.toString();
			return applyStringFlags(f, f.isAlt ? str.substring(0, 1) : str);
		case 'T':
			str = val.toString();
			return applyStringFlags(f, capitalizeSegment(f.isAlt ? str.substring(0, 1) : str));
		case 'y':
			str = val ? "yes" : "no";
			return applyStringFlags(f, f.isAlt ? str.substring(0, 1) : str);
		case 'Y':
			str = val ? "Yes" : "No";
			return applyStringFlags(f, f.isAlt ? str.substring(0, 1) : str);
		case 'd':case 'x':case 'X':case 'o':case 'b':case 'B':
			return convert(integerType(0, 1), val ? 1 : 0, singletonMap(integerType(), f), indent);
		case 'e':case 'E':case 'f':case 'g':case 'G':case 'a':case 'A':
			return convert(floatType(0.0, 1.0), val ? 1.0 : 0.0, singletonMap(floatType(), f), indent);
		case 's':case 'p':
			str = val.toString();
			return applyStringFlags(f, f.isAlt ? puppetQuote(str) : str);
		default:
			throw new StringFormatException("Boolean", f.fmt, "tTyYdxXobBeEfgGaAsp");
		}
	}

	String string(DefaultType valType, Object val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		String v;
		switch(f.fmt) {
		case 'd':case 's':case 'p':
			v = f.isAlt ? "'default'" : "default";
			break;
		case 'D':
			v = f.isAlt ? "'Default'" : "Default";
			break;
		default:
			throw new StringFormatException("Default", f.fmt, "dDsp");
		}
		return applyStringFlags(f, v);
	}

	String string(FloatType valType, Number val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 'd':
		case 'x':
		case 'X':
		case 'o':
		case 'b':
		case 'B':
			long iv = val.longValue();
			AnyType ivt = infer(iv);
			return convert(ivt, iv, singletonMap(ivt, f), indent);
		case 'p':
			return format(f.origFmt.replace('p', 'g'), val);
		case 'e':
		case 'E':
		case 'f':
		case 'g':
		case 'G':
		case 'a':
		case 'A':
			return format(f.origFmt, val);
		case 's':
			return applyStringFlags(f, f.isAlt ? puppetQuote(val.toString()) : val.toString());
		default:
			throw new StringFormatException("Float", f.fmt, "dxXobBeEfgGaAsp");
		}
	}

	String string(HashType valType, Map<?, ?> val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);

		switch(f.fmt) {
		case 'a':
			List<?> arrayHash = mapAsPairs(val);
			return convert(infer(arrayHash), arrayHash, formatMap, indent);
		case 'h':
		case 's':
		case 'p':
			String sep = f.separator == null ? DEFAULT_HASH_FORMAT.separator : f.separator;
			String assoc = f.separator2 == null ? DEFAULT_HASH_FORMAT.separator2 : f.separator2;
			Map<AnyType, Format> containerFormats = f.containerFormats == null ? DEFAULT_CONTAINER_FORMATS : f.containerFormats;
			char[] delims = f.delimiterPair(DEFAULT_HASH_DELMITERS);
			sep += f.isAlt ? '\n' : ' ';

			indent = indent.indenting(f.isAlt || indent.isIndending);
			StringBuilder bld = new StringBuilder();
			if(indent.breaks()) {
				bld.append('\n');
				bld.append(indent.padding);
			}

			Indentation childrenIndent = indent.increase(false);

			char[] padding = new char[0];
			String condBreak = "";
			if(f.isAlt) {
				condBreak = "\n";
				padding = childrenIndent.padding;
			}

			if(delims[0] != 0)
				bld.append(delims[0]);
			bld.append(condBreak);
			if(!val.isEmpty()) {
				for(Map.Entry<?,?> entry : val.entrySet()) {
					Object k = entry.getKey();
					Object v = entry.getValue();
					AnyType kt = infer(k);
					AnyType vt = infer(v);
					bld.append(padding);
					bld.append(convert(kt, k, isContainer(kt) ? formatMap : containerFormats, childrenIndent));
					bld.append(assoc);
					bld.append(convert(vt, v, isContainer(vt) ? formatMap : containerFormats, childrenIndent));
					bld.append(sep);
				}
				// Remove last separator
				bld.setLength(bld.length() - sep.length());
			}

			if(f.isAlt) {
				bld.append(condBreak);
				bld.append(indent.padding);
			}
			if(delims[1] != 0)
				bld.append(delims[1]);
			return bld.toString();
		default:
			throw new StringFormatException("Hash", f.fmt, "hasp");
		}
	}

	int integerRadix(char fmt) {
		switch(fmt) {
		case 'x':
		case 'X':
			return 16;
		case 'o':
			return 8;
		case 'b':
		case 'B':
			return 2;
		default:
			return 0;
		}
	}

	String integerPrefixRadix(char fmt) {
		switch(fmt) {
		case 'x':
			return "0x";
		case 'X':
			return "0X";
		case 'o':
			return "0";
		case 'b':
			return "0b";
		case 'B':
			return "0B";
		default:
			return "";
		}
	}

	String string(IntegerType valType, Number val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 'x':
		case 'X':
		case 'p':
		case 'd':
		case 'o':
		case 'b':
		case 'B':
			long longVal = val.longValue();
			String intString = Long.toString(longVal, integerRadix(f.fmt));
			int totWidth = f.width == null ? 0 : f.width;
			int numWidth = f.prec == null ? 0 : f.prec;

			if(numWidth > 0 && numWidth < intString.length() && f.fmt == 'p')
				intString = intString.substring(0, numWidth);

			int zeroPad = numWidth - intString.length();

			String pfx = f.isAlt && longVal != 0 && !(f.fmt == 'o' && zeroPad > 0) ? integerPrefixRadix(f.fmt) : "";
			int computedFieldWidth = pfx.length() + Math.max(numWidth, intString.length());
			int spacePad = totWidth - computedFieldWidth;

			StringBuilder bld = new StringBuilder();
			while(--spacePad >= 0)
				bld.append(' ');

			bld.append(pfx);
			if(zeroPad > 0) {
				char padChar = f.fmt == 'p' ? ' ' : '0';
				while(--zeroPad >= 0)
					bld.append(padChar);
			}

			bld.append(intString);
			return bld.toString();
		case 'e':
		case 'E':
		case 'f':
		case 'g':
		case 'G':
		case 'a':
		case 'A':
			return format(f.origFmt, val.doubleValue());
		case 'c':
			String s = new String(Character.toChars(val.intValue()));
			return f.isAlt ? applyStringFlags(f, puppetQuote(s)) : format(f.origFmt.replace('c', 's'), s);
		case 's':
			return applyStringFlags(f, f.isAlt ? puppetQuote(val.toString()) : val.toString());
		default:
			throw new StringFormatException("Integer", f.fmt, "dxXobBeEfgGaAspc");
		}
	}

	String string(IteratorType valType, Iterator<?> val, Map<AnyType, Format> formatMap, Indentation indent) {
		List<Object> list = new ArrayList<>();
		Format f = getFormat(valType, formatMap);
		while(val.hasNext())
			list.add(val.next());

		AnyType arrayType = infer(list);
		return convert(arrayType, list, Format.mergeFormats(formatMap, singletonMap(arrayType, f)), indent);
	}

	String string(ObjectType valType, Object val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 'p':
			indent = indent.indenting(f.isAlt || indent.isIndending);
			StringBuilder bld = new StringBuilder();
			TypeFormatter tf = indent.isIndending
					? new TypeFormatter(bld, indent.level, 2, false, false)
					: new TypeFormatter(bld, 0, 0, false, false);
			tf.format(val);
			return bld.toString();
		case 's':case 'q':
			return val.toString();
		default:
			throw new StringFormatException("Object", f.fmt, "spq");
		}
	}

	String string(RuntimeType valType, Object val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			return val.toString();
		case 'p':case 'q':
			return puppetQuote(val.toString());
		default:
			throw new StringFormatException("Object", f.fmt, "spq");
		}
	}

	String string(RegexpType valType, Pattern val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		String rxString = RegexpType.patternWithFlagsExpanded(val);
		switch(f.fmt) {
		case 'p':
			StringBuilder bld = new StringBuilder("/");
			int top = rxString.length();
			boolean escaped = true;
			for(int idx = 0; idx < top; ++idx) {
				char c = rxString.charAt(idx);
				if(escaped)
					escaped = false;
				else {
					if(c == '\\')
						escaped = true;
					else if(c == '/')
						bld.append('\\');
				}
				bld.append(c);
			}
			if(escaped)
				bld.append('\\');
			bld.append('/');
			return applyStringFlags(f, bld.toString());
		case 's':
			return applyStringFlags(f, f.isAlt ? puppetQuote(rxString) : rxString);
		default:
			throw new StringFormatException("Regexp", f.fmt, "ps");
		}
	}

	String string(StringType valType, String val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			return format(f.origFmt, val);
		case 'p':
			return applyStringFlags(f, puppetQuote(val));
		case 'c':
			val = capitalizeSegment(val);
			return f.isAlt ? applyStringFlags(f, puppetQuote(val)) : format(f.origFmt.replace('c', 's'), val);
		case 'C':
			val = capitalizeSegments(val);
			return f.isAlt ? applyStringFlags(f, puppetQuote(val)) : format(f.origFmt.replace('C', 's'), val);
		case 'u':
			val = val.toUpperCase();
			return f.isAlt ? applyStringFlags(f, puppetQuote(val)) : format(f.origFmt.replace('u', 's'), val);
		case 'd':
			val = val.toLowerCase();
			return f.isAlt ? applyStringFlags(f, puppetQuote(val)) : format(f.origFmt.replace('d', 's'), val);
		case 't':
			val = val.trim();
			return f.isAlt ? applyStringFlags(f, puppetQuote(val)) : format(f.origFmt.replace('t', 's'), val);
		default:
			throw new StringFormatException("String", f.fmt, "cCudspt");
		}
	}

	String string(StructType valType, Map<?,?> val, Map<AnyType, Format> formatMap, Indentation indent) {
		return string(hashType(), val, formatMap, indent);
	}

	String string(UndefType valType, Object val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		String v;
		switch(f.fmt) {
		case 's':
			v = f.isAlt ? "''" : "";
			break;
		case 'd':case 'x':case 'X':case 'o':case 'b':case 'B':case 'e':case 'E':case 'f':case 'g':case 'G':case 'a':case 'A':
			v = "NaN";
			break;
		case 'n':
			v = f.isAlt ? "null" : "nil";
			break;
		case 'u':
			v = f.isAlt ? "undefined" : "undef";
			break;
		case 'p':
			v = f.isAlt ? "'undef'" : "undef";
			break;
		case 'v':
			v = "n/a";
			break;
		case 'V':
			v = "N/A";
			break;
		default:
			throw new StringFormatException("String", f.fmt, "nudxXobBeEfgGaAvVsp");
		}
		return applyStringFlags(f, v);
	}

	String string(TypeType valType, AnyType val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			return applyStringFlags(f, f.isAlt ? puppetQuote(val.toString()) : val.toString());
		case 'p':
			return applyStringFlags(f, val.toString());
		default:
			throw new StringFormatException("Type", f.fmt, "sp");
		}
	}

	String string(TimeSpanType valType, Duration val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			String str = DurationFormat.defaultFormat(val);
			return applyStringFlags(f, f.isAlt ? puppetQuote(str) : str);
		case 'p':
			return applyStringFlags(f, "TimeSpan('" + DurationFormat.defaultFormat(val) + "')");
		default:
			throw new StringFormatException("TimeSpan", f.fmt, "sp");
		}
	}

	String string(TimestampType valType, Instant val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			String str = InstantFormat.defaultFormat(val);
			return applyStringFlags(f, f.isAlt ? puppetQuote(str) : str);
		case 'p':
			return applyStringFlags(f, "Timestamp('" + InstantFormat.defaultFormat(val) + "')");
		default:
			throw new StringFormatException("Timestamp", f.fmt, "sp");
		}
	}

	String string(TupleType valType, List<?> val, Map<AnyType, Format> formatMap, Indentation indent) {
		return string(arrayType(), val, formatMap, indent);
	}

	String string(SemVerType valType, Version val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			String str = val.toString();
			return applyStringFlags(f, f.isAlt ? puppetQuote(str) : str);
		case 'p':
			return applyStringFlags(f, "SemVer('" + val.toString() + "')");
		default:
			throw new StringFormatException("SemVer", f.fmt, "sp");
		}
	}

	String string(SemVerRangeType valType, VersionRange val, Map<AnyType, Format> formatMap, Indentation indent) {
		Format f = getFormat(valType, formatMap);
		switch(f.fmt) {
		case 's':
			String str = val.toString();
			return applyStringFlags(f, f.isAlt ? puppetQuote(str) : str);
		case 'p':
			return applyStringFlags(f, "SemVerRange('" + val.toString() + "')");
		default:
			throw new StringFormatException("SemVerRange", f.fmt, "sp");
		}
	}

	static String applyStringFlags(Format f, String str) {
		if(f.isLeft || f.width != null || f.prec != null) {
			StringBuilder bld = new StringBuilder("%");
			if(f.isLeft)
				bld.append('-');
			if(f.width != null)
				bld.append(f.width);
			if(f.prec != null) {
				bld.append('.');
				bld.append(f.prec);
			}
			bld.append('s');
			return format(bld.toString(), str);
		}
		return str;
	}

	private static Format getFormat(AnyType type, Map<AnyType, Format> formatMap) {
		for(Map.Entry<AnyType, Format> entry : formatMap.entrySet())
			if(entry.getKey().isAssignable(type))
				return entry.getValue();
		return DEFAULT_FORMATS.get(anyType());
	}

	public static String puppetQuote(String s) {
		StringBuilder bld = new StringBuilder();
		puppetQuote(s, bld);
		return bld.toString();
	}

	public static void puppetQuote(String s, StringBuilder bld) {
		int top = s.length();
		boolean escaped = false;
		int start = bld.length();
		bld.append('\'');
		for(int idx = 0; idx < top; ++idx) {
			char c = s.charAt(idx);
			if(c < 0x20) {
				bld.setLength(start);
				puppetDoubleQuote(s, bld);
				return;
			}

			if(escaped) {
				bld.append('\\');
				bld.append(c);
				escaped = false;
			} else if(c == '\'') {
				bld.append('\\');
				bld.append(c);
			} else if(c == '\\')
				escaped = true;
			else
				bld.append(c);
		}
		if(escaped)
			bld.append('\\');
		bld.append('\'');
	}

	private static void puppetDoubleQuote(String s, StringBuilder bld) {
		int top = s.length();
		bld.append('"');
		for(int idx = 0; idx < top; ++idx) {
			char c = s.charAt(idx);
			switch(c) {
			case '\t':
				bld.append("\\t");
				break;
			case '\n':
				bld.append("\\n");
				break;
			case '\r':
				bld.append("\\r");
				break;
			case '"':
				bld.append("\\\"");
				break;
			case '\\':
				bld.append("\\\\");
				break;
			case '$':
				bld.append("\\");
				bld.append(c);
				break;
			default:
				if(c < 0x20) {
					bld.append(format("\\u{%X}", (int)c));
				} else
					bld.append(c);
			}
		}
		bld.append('"');
	}
}
