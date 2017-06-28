package com.puppet.pcore.time;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.map;
import static java.util.Arrays.asList;

public class DurationFormat {
	static abstract class Segment {
		abstract void appendRegexp(StringBuilder bld);

		abstract void appendTo(StringBuilder bld, Duration d);

		long multiplier() {
			return 0;
		}

		long nanoseconds(String group) {
			return 0;
		}
	}

	static class LiteralSegment extends Segment {
		private final StringBuilder literal = new StringBuilder();

		LiteralSegment(char c) {
			append(c);
		}

		void append(char c) {
			literal.append(c);
		}

		@Override
		void appendRegexp(StringBuilder bld) {
			bld.append('(').append(Pattern.quote(literal.toString())).append(')');
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			bld.append(literal);
		}
	}

	static abstract class ValueSegment extends Segment {
		final int defaultWidth;
		final String format;
		final char padChar;
		final int width;
		boolean useTotal;

		ValueSegment(char padChar, int width, int defaultWidth) {
			this.useTotal = false;
			this.padChar = padChar;
			this.width = width;
			this.defaultWidth = defaultWidth;
			this.format = createFormat();
		}

		@Override
		void appendRegexp(StringBuilder bld) {
			if(width == 0) {
				if(padChar == ' ') {
					if(useTotal)
						bld.append("\\s*([0-9]+)");
					else
						bld.append("([0-9\\s]{1,").append(defaultWidth).append("})");
				} else {
					if(useTotal)
						bld.append("([0-9]+)");
					else
						bld.append("([0-9]{1,").append(defaultWidth).append("})");
				}
			} else {
				if(padChar == 0)
					bld.append("([0-9]{1,").append(width).append("})");
				else if(padChar == '0')
					bld.append("([0-9]{").append(width).append("})");
				else
					bld.append("([0-9\\s]{").append(width).append("})");
			}
		}

		void appendValue(StringBuilder bld, long n) {
			bld.append(String.format(format, n));
		}

		String createFormat() {
			if(padChar == 0)
				return "%d";
			StringBuilder bld = new StringBuilder("%");
			if(padChar != ' ')
				bld.append(padChar);
			return bld.append(width > 0 ? width : defaultWidth).append('d').toString();
		}

		@Override
		long nanoseconds(String group) {
			return Integer.parseInt(group) * multiplier();
		}
	}

	static class DaySegment extends ValueSegment {
		DaySegment(char padChar, int width) {
			super(padChar, width, 1);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			appendValue(bld, d.toDays());
		}

		@Override
		long multiplier() {
			return NSECS_PER_DAY;
		}
	}

	static class HourSegment extends ValueSegment {
		HourSegment(char padChar, int width) {
			super(padChar, width, 2);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			appendValue(bld, useTotal ? d.toHours() : d.toHours() % 24);
		}

		@Override
		long multiplier() {
			return NSECS_PER_HOUR;
		}
	}

	static class MinuteSegment extends ValueSegment {
		MinuteSegment(char padChar, int width) {
			super(padChar, width, 2);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			appendValue(bld, useTotal ? d.toMinutes() : d.toMinutes() % 60);
		}

		@Override
		long multiplier() {
			return NSECS_PER_MIN;
		}
	}

	static class SecondSegment extends ValueSegment {
		SecondSegment(char padChar, int width) {
			super(padChar, width, 2);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			appendValue(bld, useTotal ? d.getSeconds() : d.getSeconds() % 60);
		}

		@Override
		long multiplier() {
			return NSECS_PER_SEC;
		}
	}

	// Class that assumes that leading zeroes are significant and that trailing zeroes are not and left justifies when
	// formatting.
	// Applicable after a decimal point, and hence to the %L and %N formats.
	static abstract class FragmentSegment extends ValueSegment {
		static final Pattern DROP_TRAILING_ZEROES = Pattern.compile("\\A([0-9]+?)0*\\z");

		FragmentSegment(char padChar, int width, int defaultWidth) {
			super(padChar, width, defaultWidth);
		}

		static long pow(long a, int b) {
			long result = 1;
			for(int i = 1; i <= b; ++i)
				result *= a;
			return result;
		}

		@Override
		void appendValue(StringBuilder bld, long n) {
			if(!(useTotal || padChar == '0'))
				n = Long.parseLong(DROP_TRAILING_ZEROES.matcher(Long.toString(n)).replaceFirst("$1"));
			super.appendValue(bld, n);
		}

		@Override
		String createFormat() {
			return padChar == 0
					? "%d"
					: "%-" + (width > 0 ? width : defaultWidth) + 'd';
		}

		@Override
		long nanoseconds(String group) {
			// Using %L or %N to parse a string only makes sense when they are considered to be fractions. Using them
			// as a total quantity would introduce ambiguities.
			if(useTotal)
				throw new IllegalArgumentException(
						"Format specifiers %L and %N denotes fractions and must be used together with a specifier of higher magnitude");
			long n = Long.parseLong(group);
			int p = 9 - group.length();
			return p <= 0 ? n : n * pow(10, p);
		}
	}

	static class MillisecondSegment extends FragmentSegment {
		MillisecondSegment(char padChar, int width) {
			super(padChar, width, 3);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			appendValue(bld, useTotal ? d.toMillis() : d.toMillis() % 1000);
		}

		@Override
		long multiplier() {
			return NSECS_PER_MSEC;
		}
	}

	static class NanosecondSegment extends FragmentSegment {
		NanosecondSegment(char padChar, int width) {
			super(padChar, width, 9);
		}

		@Override
		void appendTo(StringBuilder bld, Duration d) {
			long ns = d.toNanos();
			int w = width == 0 ? defaultWidth : width;
			if(w < 9) {
				// Truncate digits to the right, i.e. let %6N reflect microseconds
				ns /= pow(10, 9 - w);
				if(!useTotal)
					ns %= pow(10, w);
			} else if(!useTotal)
				ns %= NSECS_PER_SEC;
			appendValue(bld, ns);
		}

		@Override
		long multiplier() {
			int w = width == 0 ? defaultWidth : width;
			return w < 9 ? pow(10, 9 - w) : 1;
		}
	}

	static class FormatParser {
		static final FormatParser singleton = new FormatParser();
		private static final int NSEC_MAX = 0;
		private static final int MSEC_MAX = 1;
		private static final int SEC_MAX = 2;
		private static final int MIN_MAX = 3;
		private static final int HOUR_MAX = 4;
		private static final int DAY_MAX = 5;
		private static final Class<?>[] SEGMENT_CLASS_BY_ORDINAL = {
				NanosecondSegment.class, MillisecondSegment.class, SecondSegment.class, MinuteSegment.class, HourSegment.class, DaySegment.class
		};
		// States used by the #internal_parser function
		private static final int STATE_LITERAL = 0; // expects literal or '%'
		private static final int STATE_PAD = 1; // expects pad, width, or format character
		private static final int STATE_WIDTH = 2; // expects width, or format character
		private final Map<String,DurationFormat> formats = new HashMap<>();

		DurationFormat parseFormat(String format) {
			return formats.computeIfAbsent(format, this::internalParse);
		}

		private void appendLiteral(List<Segment> bld, char c) {
			Segment last = null;
			int count = bld.size();
			if(count > 0)
				last = bld.get(count - 1);
			if(last instanceof LiteralSegment)
				((LiteralSegment)last).append(c);
			else
				bld.add(new LiteralSegment(c));
		}

		private String badFormatSpecifier(String format, int start, int position) {
			return String.format("Bad format specifier '%s' in '%s' at position %d", format.substring(start, position - start), format, position);
		}

		private DurationFormat internalParse(String str) {
			List<Segment> bld = new ArrayList<>();
			int highest = -1;
			int state = STATE_LITERAL;
			char padChar = '0';
			int width = 0;
			int start = 0;
			int top = str.length();
			int position = 0;
			for(; position < top; ++position) {
				char c = str.charAt(position);
				if(state == STATE_LITERAL) {
					if(c == '%') {
						state = STATE_PAD;
						start = position;
						padChar = '0';
						width = 0;
					} else
						appendLiteral(bld, c);
					continue;
				}

				switch(c) {
				case '%':
					appendLiteral(bld, c);
					state = STATE_LITERAL;
					break;
				case '-':
					if(state != STATE_PAD)
						throw new IllegalArgumentException(badFormatSpecifier(str, start, position));
					padChar = 0;
					state = STATE_WIDTH;
					break;
				case '_':
					if(state != STATE_PAD)
						throw new IllegalArgumentException(badFormatSpecifier(str, start, position));
					padChar = ' ';
					state = STATE_WIDTH;
					break;
				case 'D':
					highest = DAY_MAX;
					bld.add(new DaySegment(padChar, width));
					state = STATE_LITERAL;
					break;
				case 'H':
					if(highest < HOUR_MAX)
						highest = HOUR_MAX;
					bld.add(new HourSegment(padChar, width));
					state = STATE_LITERAL;
					break;
				case 'M':
					if(highest < MIN_MAX)
						highest = MIN_MAX;
					bld.add(new MinuteSegment(padChar, width));
					state = STATE_LITERAL;
					break;
				case 'S':
					if(highest < SEC_MAX)
						highest = SEC_MAX;
					bld.add(new SecondSegment(padChar, width));
					state = STATE_LITERAL;
					break;
				case 'L':
					if(highest < MSEC_MAX)
						highest = MSEC_MAX;
					bld.add(new MillisecondSegment(padChar, width));
					state = STATE_LITERAL;
					break;
				case 'N':
					if(highest < NSEC_MAX)
						highest = NSEC_MAX;
					bld.add(new NanosecondSegment(padChar, width));
					state = STATE_LITERAL;
					break;
				default:
					if(c < '0' || c > '9')
						throw new IllegalArgumentException(badFormatSpecifier(str, start, position));
					if(state == STATE_PAD && c == '0') {
						padChar = '0';
						break;
					}
					int n = c - '0';
					if(width == 0)
						width = n;
					else
						width = width * 10 + n;
					state = STATE_WIDTH;
				}
			}

			if(state != STATE_LITERAL)
				throw new IllegalArgumentException(badFormatSpecifier(str, start, position));
			if(highest != -1) {
				Class<?> hc = SEGMENT_CLASS_BY_ORDINAL[highest];
				for(Segment s : bld) {
					if(hc.isInstance(s))
						((ValueSegment)s).useTotal = true;
				}
			}
			return new DurationFormat(str, bld.toArray(new Segment[bld.size()]));
		}
	}

	public static final List<DurationFormat> DEFAULTS = Collections.unmodifiableList(
			map(asList("%D-%H:%M:%S.%-N", "%H:%M:%S.%-N", "%M:%S.%-N", "%S.%-N", "%D-%H:%M:%S", "%H:%M:%S", "%D-%H:%M", "%S"), FormatParser.singleton::parseFormat));
	private static final long NSECS_PER_USEC = 1000;
	private static final long NSECS_PER_MSEC = NSECS_PER_USEC * 1000;
	private static final long NSECS_PER_SEC = NSECS_PER_MSEC * 1000;
	private static final long NSECS_PER_MIN = NSECS_PER_SEC * 60;
	private static final long NSECS_PER_HOUR = NSECS_PER_MIN * 60;
	private static final long NSECS_PER_DAY = NSECS_PER_HOUR * 24;
	private final String format;
	private final Segment[] segments;
	private Pattern pattern;

	private DurationFormat(String format, Segment[] segments) {
		this.format = format;
		this.segments = segments;
	}

	public static Duration defaultParse(String timeSpan) {
		for(DurationFormat format : DEFAULTS) {
			try {
				return format.parse(timeSpan);
			} catch(IllegalArgumentException ignored) {
			}
		}
		throw new IllegalArgumentException(String.format(
				"Unable to parse '%s' using any of the default formats",
				timeSpan));
	}

	public static String defaultFormat(Duration timeSpan) {
		return DEFAULTS.get(0).format(timeSpan);
	}

	public static Duration parse(String timeSpan, List<String> formats) {
		for(String format : formats) {
			try {
				return parse(timeSpan, format);
			} catch(IllegalArgumentException ignored) {
			}
		}
		throw new IllegalArgumentException(String.format(
				"Unable to parse '%s' using any of the default formats",
				timeSpan));
	}

	public static String format(Duration timeSpan, String pattern) {
		return FormatParser.singleton.parseFormat(pattern).format(timeSpan);
	}

	public static void formatTo(Duration timeSpan, String pattern, StringBuilder bld) {
		FormatParser.singleton.parseFormat(pattern).formatTo(timeSpan, bld);
	}

	public static Duration parse(String timeSpan, String pattern) {
		return FormatParser.singleton.parseFormat(pattern).parse(timeSpan);
	}

	public String format(Duration duration) {
		StringBuilder bld = new StringBuilder();
		formatTo(duration, bld);
		return bld.toString();
	}

	public Duration parse(String timeSpan) {
		Matcher m = getPattern().matcher(timeSpan);
		if(!m.matches())
			throw new IllegalArgumentException(String.format("Unable to parse '%s' using format '%s'", timeSpan, format));
		long nanoseconds = 0;
		int top = m.groupCount();
		for(int idx = 0; idx < top; ++idx) {
			Segment segment = segments[idx];
			if(segment instanceof LiteralSegment)
				continue;
			nanoseconds += segment.nanoseconds(m.group(idx + 1).trim());
		}
		Duration d = Duration.ofNanos(nanoseconds);
		if(timeSpan.startsWith("-"))
			d = d.negated();
		return d;
	}

	private void formatTo(Duration duration, StringBuilder bld) {
		if(duration.isNegative()) {
			bld.append('-');
			duration = duration.negated();
		}
		for(Segment segment : segments)
			segment.appendTo(bld, duration);
	}

	private Pattern getPattern() {
		if(pattern == null) {
			StringBuilder bld = new StringBuilder();
			int top = segments.length;
			bld.append("\\A-?");
			for(Segment segment : segments)
				segment.appendRegexp(bld);
			bld.append("\\z");
			pattern = Pattern.compile(bld.toString());
		}
		return pattern;
	}
}
