package com.puppet.pcore.impl;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Default;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
@DisplayName("The TypeCalculator")
public class TypeCalculatorTest {
	@Nested
	@DisplayName("infers")
	class Infers {
		@Test
		@DisplayName("Binary")
		public void inferBinary() {
			assertInfer(binaryType(), Binary.fromBase64Strict("SGVsbG8gUHVwcGV0Cg=="));
		}

		@Test
		@DisplayName("Boolean")
		public void inferBoolean() {
			assertInfer(booleanType(), true);
		}

		@Test
		@DisplayName("Integer (from byte)")
		public void inferByte() {
			assertInfer(integerType(23, 23), (byte)23);
		}

		@Test
		@DisplayName("Collection (from Set)")
		public void inferCollection() {
			assertInfer(collectionType(0, 0), new HashSet());
		}

		@Test
		@DisplayName("Default")
		public void inferDefault() {
			assertInfer(defaultType(), Default.SINGLETON);
		}

		@Test
		@DisplayName("Float (from double)")
		public void inferDouble() {
			assertInfer(floatType(845438.9432, 845438.9432), 845438.9432);
		}

		@Test
		@DisplayName("TimeSpan (from Duration)")
		public void inferDuration() {
			Duration d = Duration.ofHours(4);
			assertInfer(timeSpanType(d, d), d);
		}

		@Test
		@DisplayName("Float (from float)")
		public void inferFloat() {
			assertInfer(floatType(38.94F, 38.94F), 38.94F);
		}

		@Test
		@DisplayName("Timestamp (from Instant)")
		public void inferInstant() {
			Instant t = Instant.ofEpochSecond(1484342220L);
			assertInfer(timestampType(t, t), t);
		}

		@Test
		@DisplayName("Integer (from int)")
		public void inferInteger() {
			assertInfer(integerType(278432, 278432), 278432);
		}

		@Test
		@DisplayName("Array[Numeric]")
		public void inferList1() {
			assertInfer(arrayType(numericType(), integerType(2, 2)), asList(2.8, 123));
		}

		@Test
		@DisplayName("Array[Scalar]")
		public void inferList2() {
			assertInfer(arrayType(scalarType(), integerType(2, 2)), asList("hello", Version.create(1, 2, 3)));
		}

		@Test
		@DisplayName("Array[ScalarData]")
		public void inferList3() {
			assertInfer(arrayType(scalarDataType(), integerType(2, 2)), asList("hello", 123));
		}

		@Test
		@DisplayName("Array[Data]")
		public void inferList4() {
			assertInfer(arrayType(dataType(), integerType(2, 2)), asList("hello", asList("world", 123)));
		}

		@Test
		@DisplayName("Array[Array[Enum]]")
		public void inferList5() {
			assertInfer(arrayType(arrayType(enumType("first", "array", "second")), integerType(2, 2)), asList(asList
					("first", "array"), asList("second", "array")));
		}

		@Test
		@DisplayName("Integer (from long)")
		public void inferLong() {
			assertInfer(integerType(6214278432L, 6214278432L), 6214278432L);
		}

		@Test
		@DisplayName("Hash[String,String]")
		public void inferMap1() {
			assertInfer(hashType(stringType("a"), stringType("b"), integerType(1, 1)), singletonMap("a", "b"));
		}

		@Test
		@DisplayName("Hash[0,0]")
		public void inferMap2() {
			assertInfer(hashType(unitType(), unitType(), integerType(0, 0)), emptyMap());
		}

		@Test
		@DisplayName("Undef")
		public void inferNull() {
			assertInfer(undefType(), null);
		}

		@Test
		@DisplayName("Regexp")
		public void inferRegexp() {
			assertInfer(regexpType("abc"), Pattern.compile("abc"));
		}

		@Test
		@DisplayName("Integer (from short)")
		public void inferShort() {
			assertInfer(integerType(3200, 3200), (short)3200);
		}

		@Test
		@DisplayName("String")
		public void inferString() {
			assertInfer(stringType("hello"), "hello");
		}

		@Test
		@DisplayName("Version")
		public void inferVersion() {
			Version v = Version.create(2, 3, 4);
			VersionRange vr = VersionRange.exact(v);
			assertInfer(semVerType(vr), v);
		}

		@Test
		@DisplayName("VersionRange")
		public void inferVersionRange() {
			VersionRange vr = VersionRange.create(">=3.2.1");
			assertInfer(semVerRangeType(), vr);
		}

		@Test
		@DisplayName("Basic types")
		public void inferBasicTypes() {
			for(AnyType type : TypeEvaluatorImpl.BASIC_TYPES.values())
				assertInfer(typeType(type), type);
		}

		@Test
		@DisplayName("Basic types _ptype")
		public void inferBasicTypesPtype() {
			for(AnyType type: TypeEvaluatorImpl.BASIC_TYPES.values())
				String.format("Pcore::%sType", type.name()).equals(type._pcoreType().name());
		}

		private void assertInfer(AnyType expected, Object value) {
			assertEquals(expected, infer(value));
		}
	}

	@Nested
	@DisplayName("infers detailed")
	class InfersDetailed {

		@Test
		@DisplayName("Struct[{'first' => Integer}]")
		public void inferMap1() {
			Map<String,Integer> m = new LinkedHashMap<>();
			m.put("first", 1);
			m.put("second", 2);
			assertInferSet(structType(
					structElement(stringType("first"), integerType(1, 1)),
					structElement(stringType("second"), integerType(2, 2))), m);
		}

		@Test
		@DisplayName("Struct[{'mode' => String, 'path' => Tuple}]")
		public void inferMap2() {
			Map<String,Object> m = new LinkedHashMap<>();
			m.put("mode", "read");
			m.put("path", asList("foo", "fee"));
			assertInferSet(structType(
					structElement(stringType("mode"), stringType("read")),
					structElement(stringType("path"), tupleType(asList(stringType("foo"), stringType("fee"))))), m);
		}

		@Test
		@DisplayName("Struct[{1 => String, 'second' => String}]")
		public void inferMap3() {
			Map<Object,Object> m = new LinkedHashMap<>();
			m.put(1, "first");
			m.put("second", "second");
			assertInferSet(hashType(variantType(integerType(1, 1), stringType("second")), enumType("first", "second"), 2, 2)
					, m);
		}

		private void assertInferSet(AnyType expected, Object value) {
			assertEquals(expected, inferSet(value));
		}
	}
}
