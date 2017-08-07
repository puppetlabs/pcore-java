package com.puppet.pcore.impl.types;

import com.puppet.pcore.Default;
import com.puppet.pcore.PcoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.puppet.pcore.impl.types.TypeFactory.integerType;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("Integer Type")
public class IntegerTypeTest {
	@Test
	@DisplayName("does not acccept min > max")
	public void rejectMaxLtMin() {
		assertThrows(PcoreException.class, () -> integerType(23, 10));
	}

	@Test
	@DisplayName("asSize rejects negative min")
	public void rejectNegativeSize() {
		assertThrows(PcoreException.class, () -> integerType(-5, 10).asSize());
	}

	@Test
	@DisplayName("default type is not finite")
	public void defaultIsNotFinite() {
		assertFalse(integerType().isFiniteRange());
	}

	@Test
	@DisplayName("without lower bound is not finite")
	public void noLowerNotFinite() {
		assertFalse(integerType(Long.MIN_VALUE, 2).isFiniteRange());
	}

	@Test
	@DisplayName("without upper bound is not finite")
	public void noUpperNotFinite() {
		assertFalse(integerType(0, Long.MAX_VALUE).isFiniteRange());
	}

	@Test
	@DisplayName("with both lower and upper bound is finite")
	public void bothLowerAndUpperFinite() {
		assertTrue(integerType(0, 32).isFiniteRange());
	}

	@Nested
	@DisplayName("when creating an instance")
	class NewInstance {
		@Test
		@DisplayName("creates Long from String")
		public void longFromString() {
			assertEquals(15L, integerType().newInstance("15"));
		}

		@Test
		@DisplayName("accepts leading +")
		public void longFromStringWithPlus() {
			assertEquals(15L, integerType().newInstance("+15"));
		}

		@Test
		@DisplayName("accepts space between leading sign and number")
		public void spaceAfterSign() {
			assertEquals((long)-15, integerType().newInstance("- 15"));
		}

		@Test
		@DisplayName("accepts octal number")
		public void acceptsOctalNumber() {
			assertEquals(13L, integerType().newInstance("015"));
		}

		@Test
		@DisplayName("accepts hex number using 0x")
		public void acceptsHexNumber() {
			assertEquals(21L, integerType().newInstance("0x15"));
		}

		@Test
		@DisplayName("accepts hex number using 0X")
		public void acceptsHeXNumber() {
			assertEquals(21L, integerType().newInstance("0X15"));
		}

		@Test
		@DisplayName("accepts binary number using 0b")
		public void acceptsBinaryNumber() {
			assertEquals(21L, integerType().newInstance("0b10101"));
		}

		@Test
		@DisplayName("accepts binary number using 0B")
		public void acceptsBinaryNumber2() {
			assertEquals(21L, integerType().newInstance("0B10101"));
		}

		@Test
		@DisplayName("accepts radix 2")
		public void acceptsRadix2() {
			assertEquals(21L, integerType().newInstance("10101", 2));
		}

		@Test
		@DisplayName("accepts radix 8")
		public void acceptsRadix8() {
			assertEquals(21L, integerType().newInstance("25", 8));
		}

		@Test
		@DisplayName("accepts radix 10")
		public void acceptsRadix10() {
			assertEquals(21L, integerType().newInstance("21", 10));
		}

		@Test
		@DisplayName("accepts radix 16")
		public void acceptsRadix16() {
			assertEquals(21L, integerType().newInstance("15", 16));
		}

		@Test
		@DisplayName("does not accept invalid radix")
		public void rejectInvalidRadix() {
			assertThrows(PcoreException.class, () -> integerType().newInstance("15", 12));
		}

		@Test
		@DisplayName("long from long returns same instance")
		public void longSameInstance() {
			Long v = 21L;
			assertSame(v, integerType().newInstance(v));
		}

		@Test
		@DisplayName("creates long from byte")
		public void convertsByteToLong() {
			assertEquals(21L, integerType().newInstance((byte)21));
		}

		@Test
		@DisplayName("creates long from short")
		public void convertsShortToLong() {
			assertEquals(21L, integerType().newInstance((short)21));
		}

		@Test
		@DisplayName("creates long from int")
		public void convertsIntToLong() {
			assertEquals(21L, integerType().newInstance(21));
		}

		@Test
		@DisplayName("creates long from float")
		public void convertsFloatToLong() {
			assertEquals(21L, integerType().newInstance((float)21.0));
		}

		@Test
		@DisplayName("creates long from double")
		public void convertsDoubleToLong() {
			assertEquals(21L, integerType().newInstance(21.0));
		}

		@Test
		@DisplayName("creates long from boolean")
		public void convertsBooleanToLong() {
			assertEquals(0L, integerType().newInstance(false));
			assertEquals(1L, integerType().newInstance(true));
		}

		@Test
		@DisplayName("creates long from Map")
		public void convertsMapValueToLong() {
			assertEquals(21L, integerType().newInstance(singletonMap("from", "0x15")));
		}

		@Test
		@DisplayName("creates long from Duration seconds")
		public void convertsDurationToLong() {
			assertEquals(21L, integerType().newInstance(Duration.ofSeconds(21)));
		}

		@Test
		@DisplayName("creates long from Instant seconds since epoch")
		public void convertsInstantToLong() {
			assertEquals(21L, integerType().newInstance(Instant.parse("1970-01-01T00:00:21.00Z")));
		}

		@Test
		@DisplayName("creates abs value")
		public void createsAbsValue() {
			assertEquals(21L, integerType().newInstance(-21, Default.SINGLETON, true));
		}

		@Test
		@DisplayName("does not accept null")
		public void rejectNull() {
			assertThrows(PcoreException.class, () -> integerType().newInstance((Object)null));
		}

		@Test
		@DisplayName("does not accept empty string")
		public void rejectEmpty() {
			assertThrows(PcoreException.class, () -> integerType().newInstance(""));
		}

		@Test
		@DisplayName("does not accept string with just sign")
		public void rejectSignButEmpty() {
			assertThrows(PcoreException.class, () -> integerType().newInstance("+"));
		}

		@Test
		@DisplayName("does not accept trailing characters")
		public void rejectTrailingChars() {
			assertThrows(PcoreException.class, () -> integerType().newInstance("2 "));
		}

		@Test
		@DisplayName("does not accept conflicting radix")
		public void rejectRadixConflict() {
			assertThrows(PcoreException.class, () -> integerType().newInstance("0x15", 10));
		}
	}
}
