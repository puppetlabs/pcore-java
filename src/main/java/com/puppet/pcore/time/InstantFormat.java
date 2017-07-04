package com.puppet.pcore.time;

import java.time.*;
import java.time.format.*;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.WeekFields;
import java.util.*;

import static com.puppet.pcore.impl.Helpers.join;
import static com.puppet.pcore.impl.Helpers.map;
import static java.time.temporal.ChronoField.YEAR;
import static java.time.temporal.TemporalQueries.*;
import static java.util.Arrays.asList;

public class InstantFormat {
	private enum Padding {Default, Zero, Blank, None}

	public static final InstantFormat SINGLETON = new InstantFormat();
	// TODO: Instant parsing and formatting
	public static final List<DateTimeFormatter> DEFAULTS_WO_TZ;
	public static final List<DateTimeFormatter> DEFAULTS;
	private static final Map<Long,String> ucMonthOfYear = new HashMap<>();
	private static final Map<Long,String> ucShortMonthOfYear = new HashMap<>();
	private static final Map<Long,String> lcAmPm = new HashMap<>();
	private static final Map<Long,String> ucWeekDay = new HashMap<>();
	private static final Map<Long,String> ucShortWeekDay = new HashMap<>();

	static {
		ucMonthOfYear.put(1L, "JANUARY");
		ucMonthOfYear.put(2L, "FEBRUARY");
		ucMonthOfYear.put(3L, "MARCH");
		ucMonthOfYear.put(4L, "APRIL");
		ucMonthOfYear.put(5L, "MAY");
		ucMonthOfYear.put(6L, "JUNE");
		ucMonthOfYear.put(7L, "JULY");
		ucMonthOfYear.put(8L, "AUGUST");
		ucMonthOfYear.put(9L, "SEPTEMBER");
		ucMonthOfYear.put(10L, "OCTOBER");
		ucMonthOfYear.put(11L, "NOVEMBER");
		ucMonthOfYear.put(12L, "DECEMBER");

		ucShortMonthOfYear.put(1L, "JAN");
		ucShortMonthOfYear.put(2L, "FEB");
		ucShortMonthOfYear.put(3L, "MAR");
		ucShortMonthOfYear.put(4L, "APR");
		ucShortMonthOfYear.put(5L, "MAY");
		ucShortMonthOfYear.put(6L, "JUN");
		ucShortMonthOfYear.put(7L, "JUL");
		ucShortMonthOfYear.put(8L, "AUG");
		ucShortMonthOfYear.put(9L, "SEP");
		ucShortMonthOfYear.put(10L, "OCT");
		ucShortMonthOfYear.put(11L, "NOV");
		ucShortMonthOfYear.put(12L, "DEC");

		lcAmPm.put(0L, "am");
		lcAmPm.put(1L, "pm");

		ucWeekDay.put(1L, "MONDAY");
		ucWeekDay.put(2L, "TUESDAY");
		ucWeekDay.put(3L, "WEDNESDAY");
		ucWeekDay.put(4L, "THURSDAY");
		ucWeekDay.put(5L, "FRIDAY");
		ucWeekDay.put(6L, "SATURDAY");
		ucWeekDay.put(7L, "SUNDAY");

		ucShortWeekDay.put(1L, "MON");
		ucShortWeekDay.put(2L, "TUE");
		ucShortWeekDay.put(3L, "WED");
		ucShortWeekDay.put(4L, "THU");
		ucShortWeekDay.put(5L, "FRI");
		ucShortWeekDay.put(6L, "SAT");
		ucShortWeekDay.put(7L, "SUN");

		DEFAULTS_WO_TZ = Collections.unmodifiableList(asList(
				SINGLETON.createDateTimeFormatter("%FT%T.%N"),
				SINGLETON.createDateTimeFormatter("%FT%T"),
				SINGLETON.createDateTimeFormatter("%F %T.%N"),
				SINGLETON.createDateTimeFormatter("%F %T"),
				SINGLETON.createDateTimeFormatter("%F"))
		);

		List<DateTimeFormatter> allDefaults = new ArrayList<>(asList(
				SINGLETON.createDateTimeFormatter("%FT%T.%N %Z"),
				SINGLETON.createDateTimeFormatter("%FT%T %Z"),
				SINGLETON.createDateTimeFormatter("%F %T.%N %Z"),
				SINGLETON.createDateTimeFormatter("%F %T %Z"),
				SINGLETON.createDateTimeFormatter("%F %Z")));
		allDefaults.addAll(DEFAULTS_WO_TZ);

		DEFAULTS = Collections.unmodifiableList(allDefaults);
	}

	private final Map<String,DateTimeFormatter> formatterCache = new HashMap<>();

	public static String defaultFormat(Instant instant) {
		return SINGLETON.format(instant, DEFAULTS.get(0), ZoneOffset.UTC);
	}

	public String format(Instant instant, String pattern, ZoneId zoneId) {
		return format(instant, new InstantFormat().createDateTimeFormatter(pattern), zoneId);
	}

	public String format(Instant instant, DateTimeFormatter formatter, ZoneId zoneId) {
		ZonedDateTime ldt = ZonedDateTime.ofInstant(instant, zoneId);
		return formatter.format(ldt);
	}

	public Instant parse(String timestamp) {
		for(DateTimeFormatter format : DEFAULTS) {
			try {
				return parse(timestamp, format);
			} catch(IllegalArgumentException ignored) {
			}
		}
		throw new IllegalArgumentException(String.format(
				"Unable to parse '%s' using any of the default formats",
				timestamp));
	}

	public Instant parse(String timestamp, List<String> formats) {
		return parse(timestamp, formats, null);
	}

	public Instant parse(String timestamp, List<String> formats, String timezone) {
		ZoneId zone = timezone == null ? null : ZoneId.of(timezone);
		for(String format : formats) {
			try {
				return parse(timestamp, new InstantFormat().createDateTimeFormatter(format), zone);
			} catch(IllegalArgumentException ignored) {
			}
		}
		throw new IllegalArgumentException(String.format(
				"Unable to parse '%s' using any of the formats %s", timestamp, join(", ", formats)));
	}

	public Instant parse(String timestamp, List<DateTimeFormatter> formats, ZoneId zone) {
		for(DateTimeFormatter format : formats) {
			try {
				return parse(timestamp, format, zone);
			} catch(IllegalArgumentException ignored) {
			}
		}
		throw new IllegalArgumentException(String.format(
				"Unable to parse '%s' using any of the formats %s", timestamp, join(", ", map(formats, (f) -> f.toString()))));
	}

	public Instant parse(String instant, DateTimeFormatter formatter) {
		return parse(instant, formatter, null);
	}

	public Instant parse(String instant, DateTimeFormatter formatter, ZoneId zone) {
		try {
			TemporalAccessor ta = formatter.parse(instant);
			LocalTime time = ta.query(localTime());
			LocalDate date = ta.query(localDate());
			ZoneId parsedZone = ta.query(zoneId());
			if(zone == null)
				zone = parsedZone;
			else if(parsedZone != null)
				throw new IllegalArgumentException(
						"Using a Timezone designator in format specification is mutually exclusive to providing an explicit timezone argument");

			LocalDateTime ldt;
			if(time == null) {
				if(date == null)
					throw new IllegalArgumentException(
							String.format("Unable to extract time and/or date from string '%s'", instant));
				ldt = date.atStartOfDay();
			} else {
				ldt = date == null ? time.atDate(LocalDate.now()) : LocalDateTime.of(date, time);
			}
			return ldt.toInstant(zone == null ? ZoneOffset.UTC : ZoneOffset.of(zone.getId()));
		} catch(DateTimeParseException e) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	public Instant parse(String instant, String pattern) {
		return parse(instant, pattern, null);
	}

	public Instant parse(String instant, String pattern, String timezone) {
		return parse(instant, new InstantFormat().createDateTimeFormatter(pattern), timezone == null ? null : ZoneId.of(timezone));
	}

	private IllegalArgumentException badFormatSpecifier(String format, int start, int end) {
		return new IllegalArgumentException(String.format("Bad format specifier '%s' in '%s' at position %d",
				format.substring(start, end), format, start));
	}

	/**
	 * Creates a DateTimeFormatter instance from a strftime format. All formats in the Ruby strftime are
	 * supported except '%C' (Century) and %Q (Milliseconds since Epoch) since they don't have any corresponding
	 * ChronoFields. The flag '#' (change case) is also not supported.
	 *
	 * @param formatString The strftime style format string
	 * @return the formatter instance.
	 */
	private synchronized DateTimeFormatter createDateTimeFormatter(String formatString) {
		DateTimeFormatter formatter = formatterCache.get(formatString);
		if(formatter != null)
			return formatter;

		DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive().parseLenient();

		int start = 0;
		boolean escaped = false;
		int top = formatString.length();

		for(int idx = 0; idx < top;) {
			char c = formatString.charAt(idx++);

			if(!escaped) {
				if(c == '%') {
					start = idx - 1;
					escaped = true;
				} else
					builder.appendLiteral(c);
				continue;
			}

			Padding padding = Padding.Default;
			boolean upcase = false;
			int zColons = 0;
			int width = -1;

			// Check flags
			switch(c) {
			case '_':
				padding = Padding.Blank;
				c = 0;
				break;
			case '-':
				padding = Padding.None;
				c = 0;
				break;
			case '0':
				padding = Padding.Zero;
				c = 0;
				break;
			case '^':
				upcase = true;
				c = 0;
				break;
			case ':':
				zColons = 1;
				c = 0;
				while(idx < top) {
					c = formatString.charAt(idx++);
					if(c != ':')
						break;
					++zColons;
					c = 0;
				}
				if(zColons > 2)
					throw badFormatSpecifier(formatString, start, idx - 1);
				break;
			}

			if(c == 0) {
				if(idx >= top)
					throw badFormatSpecifier(formatString, start, idx);
				c = formatString.charAt(idx++);
			}

			// Check width
			if(c >= '1' && c <= '9') {
				width = c - '0';
				c = 0;
				while(idx < top) {
					c = formatString.charAt(idx++);
					if(c < '0' || c > '9')
						break;
					width *= 10;
					width += c - '0';
					c = 0;
				}

				if(c == 0)
					throw badFormatSpecifier(formatString, start, idx);
			}

			if(zColons > 0 && c != 'z')
				throw badFormatSpecifier(formatString, start, idx);

			switch(c) {
			// Date (Year, Month, Day):
			case 'Y': // Year with century if provided, will pad result at least 4 digits. -0001, 0000, 1995, 2009, 14292,
				// etc.
				pad(padding, width, 4, 4, '0', builder);
				builder.appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD);
				break;
			case 'y': // year % 100 (00..99)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValueReduced(ChronoField.YEAR, 2, 2, LocalDate.now().minusYears(50));
				break;
			case 'm': // Month of the year, zero-padded (01..12)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.MONTH_OF_YEAR, 2);
				break;
			case 'B': // The full month name (``January'')
				pad(padding, width, 3, 3, ' ', builder);
				if(upcase)
					builder.appendText(ChronoField.MONTH_OF_YEAR, ucMonthOfYear);
				else
					builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.FULL);
				break;
			case 'b': //The abbreviated month name (``Jan'')
			case 'h':
				pad(padding, width, 3, 3, ' ', builder);
				if(upcase)
					builder.appendText(ChronoField.MONTH_OF_YEAR, ucShortMonthOfYear);
				else
					builder.appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT);
				break;
			case 'd': // Day of the month, zero-padded (01..31)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.DAY_OF_MONTH, 2);
				break;
			case 'e': // Day of the month, blank-padded ( 1..31)
				pad(padding, width, 1, 2, ' ', builder);
				builder.appendValue(ChronoField.DAY_OF_MONTH);
				break;
			case 'j': // Day of the year (001..366)
				pad(padding, width, 3, 3, '0', builder);
				builder.appendValue(ChronoField.DAY_OF_YEAR, 3);
				break;

			// Time (Hour, Minute, Second, Subsecond):
			case 'H': // Hour of the day, 24-hour clock, zero-padded (00..23)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.HOUR_OF_DAY, 2);
				break;
			case 'k': // Hour of the day, 24-hour clock, blank-padded ( 0..23)
				pad(padding, width, 1, 2, ' ', builder);
				builder.appendValue(ChronoField.HOUR_OF_DAY);
				break;
			case 'I': // Hour of the day, 12-hour clock, zero-padded (01..12)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.HOUR_OF_AMPM, 2);
				break;
			case 'l': // Hour of the day, 12-hour clock, blank-padded ( 1..12)
				pad(padding, width, 1, 2, ' ', builder);
				builder.appendValue(ChronoField.HOUR_OF_AMPM);
				break;
			case 'P': // Meridian indicator, lowercase (``am'' or ``pm'')
				pad(padding, width, 2, 2, ' ', builder);
				builder.appendText(ChronoField.AMPM_OF_DAY, lcAmPm);
				break;
			case 'p': // Meridian indicator, uppercase (``AM'' or ``PM'')
				pad(padding, width, 2, 2, ' ', builder);
				builder.appendText(ChronoField.AMPM_OF_DAY);
				break;
			case 'M': // Minute of the hour (00..59)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.MINUTE_OF_HOUR, 2);
				break;
			case 'S': // Second of the minute (00..59)
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(ChronoField.SECOND_OF_MINUTE, 2);
				break;
			case 'L': // Millisecond of the second (000..999)
				builder.appendFraction(ChronoField.MICRO_OF_SECOND, 1, 3, false);
				break;
			case 'N': // Nano of the second (0000000..999999999)
				builder.appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false);
				break;

			// Time zone:
			case 'z':
				switch(zColons) {
				case 0:
					pad(padding, width, 5, 5, ' ', builder);
					builder.appendOffset("+HHMM", "+0000");
					break;
				case 1:
					pad(padding, width, 6, 6, ' ', builder);
					builder.appendOffset("+HH:MM", "+00:00");
					break;
				default:
					pad(padding, width, 9, 9, ' ', builder);
					builder.appendOffset("+HH:MM:SS", "+00:00:00");
				}
				break;
			case 'Z':
				pad(padding, width, 3, 3, ' ', builder);
				builder.appendZoneOrOffsetId();
				break;

			// Weekday:
			case 'A':
				pad(padding, width, 6, 6, ' ', builder);
				if(upcase)
					builder.appendText(ChronoField.DAY_OF_WEEK, ucWeekDay);
				else
					builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.FULL);
				break;
			case 'a':
				pad(padding, width, 3, 3, ' ', builder);
				if(upcase)
					builder.appendText(ChronoField.DAY_OF_WEEK, ucShortWeekDay);
				else
					builder.appendText(ChronoField.DAY_OF_WEEK, TextStyle.SHORT);
				break;
			case 'u':
				pad(padding, width, 1, 1, '0', builder);
				builder.appendValue(ChronoField.DAY_OF_WEEK);
				break;
			case 'w':
				pad(padding, width, 1, 1, '0', builder);
				builder.appendValue(WeekFields.SUNDAY_START.dayOfWeek());
				break;

			// ISO 8601 week-based year and week number:
			// The first week of YYYY starts with a Monday and includes YYYY-01-04.
			// The days in the year before the first week are in the last week of
			// the previous year.
			case 'G':
				pad(padding, width, 4, 4, '0', builder);
				builder.appendValue(WeekFields.ISO.weekBasedYear(), 4, 10, SignStyle.EXCEEDS_PAD);
				break;
			case 'g':
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValueReduced(WeekFields.ISO.weekBasedYear(), 2, 2, LocalDate.now().minusYears(50));
				break;
			case 'V':
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(WeekFields.ISO.weekOfWeekBasedYear(), 2);
				break;

			// Week number:
			// The first week of YYYY that starts with a Sunday or Monday (according to %U or %W). The days in the year
			// before the first week are in week 0.
			case 'U':
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(WeekFields.SUNDAY_START.weekOfYear(), 2);
				break;
			case 'W':
				pad(padding, width, 2, 2, '0', builder);
				builder.appendValue(WeekFields.ISO.weekOfYear(), 2);
				break;

			// Seconds since the Epoch
			case 's':
				pad(padding, width, 1, 1, '0', builder);
				builder.appendValue(ChronoField.INSTANT_SECONDS);
				break;

			// Literal string
			case '%':
				pad(padding, width, 1, 1, ' ', builder);
				builder.appendLiteral('%');
				break;
			case 'n':
				pad(padding, width, 1, 1, ' ', builder);
				builder.appendLiteral('\n');
				break;
			case 't':
				pad(padding, width, 1, 1, ' ', builder);
				builder.appendLiteral('\t');
				break;

			// Combinations
			case 'c':
				builder.append(createDateTimeFormatter("%a %b %e %H:%M:%S %Y"));
				break;
			case 'x':
			case 'D':
				builder.append(createDateTimeFormatter("%m/%d/%y"));
				break;
			case 'F':
				builder.append(createDateTimeFormatter("%Y-%m-%d"));
				break;
			case 'v':
				builder.append(createDateTimeFormatter("%e-%^b-%4Y"));
				break;
			case 'X':
			case 'T':
				builder.append(createDateTimeFormatter("%H:%M:%S"));
				break;
			case 'r':
				builder.append(createDateTimeFormatter("%I:%M:%S %p"));
				break;
			case 'R':
				builder.append(createDateTimeFormatter("%H:%M"));
				break;
			default:
				throw badFormatSpecifier(formatString, start, idx);
			}
			escaped = false;
		}
		if(escaped)
			throw badFormatSpecifier(formatString, start, formatString.length());

		formatter = builder.toFormatter();
		formatterCache.put(formatString, formatter);
		return formatter;
	}

	private void pad(
			Padding padding, int desiredWidth, int minWidth, int defaultWidth, char defaultPadChar,
			DateTimeFormatterBuilder builder) {
		if(desiredWidth < 0)
			desiredWidth = defaultWidth;
		if(desiredWidth > minWidth) {
			switch(padding) {
			case Default:
				builder.padNext(desiredWidth, defaultPadChar);
				break;
			case Blank:
				builder.padNext(desiredWidth, ' ');
				break;
			case Zero:
				builder.padNext(desiredWidth, '0');
			}
		}
	}
}
