package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.model.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LocatorTest {
	@Test
	@DisplayName("line for offset")
	public void lineForGivenOffset() {
		Locator loc = new Locator(null, "\nFirst\nSecond\n");
		assertEquals(1, loc.lineforOffset(0), "Offset 0 is on line 1");
		assertEquals(2, loc.lineforOffset(1), "Offset 1 is on line 2");
		assertEquals(2, loc.lineforOffset(2), "Offset 2 is on line 2");
		assertEquals(2, loc.lineforOffset(6), "Offset 6 is on line 2");
		assertEquals(3, loc.lineforOffset(7), "Offset 7 is on line 3");
		assertEquals(3, loc.lineforOffset(13), "Offset 13 is on line 3");
		assertEquals(4, loc.lineforOffset(14), "Offset 14 is on line 4");
		assertEquals(4, loc.lineforOffset(15), "After EOT is on line 4");
	}

	@Test
	@DisplayName("pos for offset")
	public void posForGivenOffset() {
		Locator loc = new Locator(null, "\nFirst\nSecond\n");
		assertEquals(1, loc.posforOffset(0), "Offset 0 is at pos 1");
		assertEquals(1, loc.posforOffset(1), "Offset 1 is at pos 1");
		assertEquals(2, loc.posforOffset(2), "Offset 2 is at pos 2");
		assertEquals(6, loc.posforOffset(6), "Offset 6 is at pos 6");
		assertEquals(1, loc.posforOffset(7), "Offset 7 is at pos 1");
		assertEquals(7, loc.posforOffset(13), "Offset 13 is at pos 7");
		assertEquals(1, loc.posforOffset(14), "Offset 14 is at pos 1");
	}
}
