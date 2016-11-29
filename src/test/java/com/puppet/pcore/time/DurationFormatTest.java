package com.puppet.pcore.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DurationFormatTest {
	final Duration complex = Duration.parse("P1DT7H10M11S").plusNanos(123456789);
	final Duration simple = Duration.parse("P1DT3H10M11S");

	@Test
	public void errorOnfromStringWithMillisecondsAsBiggestQuantity() {
		assertThrows(IllegalArgumentException.class, () -> parse("123", "%L"));
	}

	@Test
	public void errorOnfromStringWithNanosecondsAsBiggestQuantity() {
		assertThrows(IllegalArgumentException.class, () -> parse("123", "%N"));
	}

	@Test
	public void fromString() {
		assertEquals(simple, parse("1-03:10:11", "%D-%H:%M:%S"));
	}

	@Test
	public void fromStringWhenBiggestQuantityNotFirst() {
		assertEquals(simple, parse("11:1630", "%S:%M"));
	}

	@Test
	public void fromStringWithHourAsBiggestQuantity() {
		assertEquals(simple, parse("27:10:11", "%H:%M:%S"));
	}

	@Test
	public void fromStringWithMinuteAsBiggestQuantity() {
		assertEquals(simple, parse("1630:11", "%M:%S"));
	}

	@Test
	public void fromStringWithNanoFragment() {
		assertEquals(complex, parse("1-7:10:11.123456789", "%D-%H:%M:%S.%N"));
	}

	@Test
	public void fromStringWithNanoFragmentNegative() {
		assertEquals(complex.negated(), parse("-1-7:10:11.123456789", "%D-%H:%M:%S.%N"));
	}

	@Test
	public void fromStringWithSecondAsBiggestQuantity() {
		assertEquals(simple, parse("97811", "%S"));
	}

	@Test
	public void producesLeadingDashOnNegative() {
		assertEquals("-1-07:10:11", format(complex.negated(), "%D-%H:%M:%S"));
	}

	@Test
	public void producesLiteralPercent() {
		assertEquals("1%07:10:11", format(complex, "%D%%%H:%M:%S"));
	}

	@Test
	public void producesNoTrailingSpacesFor_dash_L() {
		assertEquals("2.3", format(parse("2.3", "%S.%L"), "%-S.%-L"));
	}

	@Test
	public void producesNoTrailingSpacesFor_dash_N() {
		assertEquals("2.345", format(parse("2.345", "%S.%N"), "%-S.%-N"));
	}

	@Test
	public void producesStringWithAllComponents() {
		assertEquals("1-07:10:11.123456789", format(complex, "%D-%H:%M:%S.%N"));
	}

	@Test
	public void producesStringWithComponentsSpacePadded() {
		assertEquals("           1-   7:  10:  11", format(complex, "%_12D-%_4H:%_4M:%_4S"));
	}

	@Test
	public void producesStringWithComponentsZeroPadded() {
		assertEquals("0001-0007:0010:0011", format(complex, "%4D-%4H:%4M:%4S"));
	}

	@Test
	public void producesWithTrailingSpacesFor_N() {
		assertEquals("2.345      ", format(parse("2.345", "%S.%N"), "%-S.%_N"));
	}

	@Test
	public void producesWithTrailingSpacesOfGivenWidthFor_N() {
		assertEquals("2.345   ", format(parse("2.345", "%S.%N"), "%-S.%_6N"));
	}

	@Test
	public void producesWithTrailingZeroesFor_0N() {
		assertEquals("2.345000000", format(parse("2.345", "%S.%N"), "%-S.%0N"));
	}

	@Test
	public void producesWithTrailingZeroesFor_N() {
		assertEquals("2.345000000", format(parse("2.345", "%S.%N"), "%-S.%N"));
	}

	@Test
	public void where_L_isTreatedAsFractionsOfASecond() {
		assertEquals(Duration.ofMillis(400), parse("0.4", "%S.%L"));
	}

	@Test
	public void where_N_isTreatedAsFractionsOfASecond() {
		assertEquals(Duration.ofMillis(400), parse("0.4", "%S.%N"));
	}

	String format(Duration duration, String pattern) {
		return DurationFormat.FormatParser.singleton.parseFormat(pattern).format(duration);
	}

	Duration parse(String duration, String pattern) {
		return DurationFormat.FormatParser.singleton.parseFormat(pattern).parse(duration);
	}
}
