package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.Deserializer;
import com.puppet.pcore.serialization.SerializationFactory;
import com.puppet.pcore.serialization.Serializer;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.Pcore.typeEvaluator;
import static com.puppet.pcore.impl.Helpers.asMap;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
@DisplayName("The Serializer/Deserializer")
public class SerializationTest {

	@BeforeEach
	public void init() {
		Pcore.reset();
	}

	interface RuntimeTypes {
		@Test
		@DisplayName("Binary")
		default void rwBinary() throws IOException {
			Binary binary = new Binary(new byte[] { (byte)139, 12, (byte)233, 5, 42 });
			assertEquals(binary, writeAndRead(binary));
		}

		@Test
		@DisplayName("Boolean")
		default void rwBoolean() throws IOException {
			assertEquals(true, writeAndRead(true));
			assertEquals(false, writeAndRead(false));
		}

		@Test
		@DisplayName("Float")
		default void rwFloat() throws IOException {
			assertEquals(123.45, writeAndRead(123.45));
			assertEquals(123456.456789, writeAndRead(123456.456789));
		}

		@Test
		@DisplayName("Integer")
		default void rwInteger() throws IOException {
			assertEquals(12L, writeAndRead((byte)12));
			assertEquals(12345L, writeAndRead((short)12345));
			assertEquals(12345678L, writeAndRead((long)12345678));
			assertEquals(543123456789L, writeAndRead(543123456789L));
			assertEquals(12L, writeAndRead(12));
			assertEquals(12345L, writeAndRead(12345));
			assertEquals(12345678L, writeAndRead(12345678));
			assertEquals(543123456789L, writeAndRead(543123456789L));
		}

		@Test
		@DisplayName("List")
		default void rwArray() throws IOException {
			List<Object> list = asList("top-level", "sub-level", asList("a", "b", "c", "d"));
			assertEquals(list, writeAndRead(list));
		}

		@Test
		@DisplayName("Map")
		default void rwMap() throws IOException {
			Map<String,Object> map = asMap("top-level", 32L, "sub-level", asMap("a", "sub-a", "b", "sub-b"));
			assertEquals(map, writeAndRead(map));
		}

		@Test
		@DisplayName("Sensitive")
		default void rwSensitive() throws IOException {
			Sensitive sensitive = new Sensitive("a sensitive string");
			assertEquals(sensitive, writeAndRead(sensitive));
		}

		@Test
		@DisplayName("String")
		default void rwString() throws IOException {
			assertEquals("Blue Öyster Cult", writeAndRead("Blue Öyster Cult"));
		}

		@Test
		@DisplayName("Symbol")
		default void rwSymbol() throws IOException {
			Symbol sym = new Symbol("someSymbol");
			assertEquals(sym, writeAndRead(sym));
		}

		@Test
		@DisplayName("TimeSpan")
		default void rwTimeStamp() throws IOException {
			Duration d = Duration.ofDays(23481).minusHours(12).negated();
			assertEquals(d, writeAndRead(d));
		}

		@Test
		@DisplayName("Timestamp")
		default void rwTimestamp() throws IOException {
			Instant now = Instant.now();
			assertEquals(now, writeAndRead(now));
		}

		@Test
		@DisplayName("Undef")
		default void rwUndef() throws IOException {
			assertEquals(null, writeAndRead(null));
		}

		@Test
		@DisplayName("Version")
		default void rwVersion() throws IOException {
			Version version = Version.create(10,20,30,"beta2");
			assertEquals(version, writeAndRead(version));
		}

		@Test
		@DisplayName("VersionRange")
		default void rwVersionRange() throws IOException {
			VersionRange versionRange = VersionRange.create("1.0.0 - 2.0.0");
			assertEquals(versionRange, writeAndRead(versionRange));
		}

		Object writeAndRead(Object value) throws IOException;
	}

	interface PcoreTypes {
		@Test
		@DisplayName("Any")
		default void rwAnyType() throws IOException {
			assertWriteAndRead("Any");
		}

		@Test
		@DisplayName("Array")
		default void rwArrayType1() throws IOException {
			assertWriteAndRead("Array");
		}

		@Test
		@DisplayName("Array[Integer]")
		default void rwArrayType2() throws IOException {
			assertWriteAndRead("Array[Integer]");
		}

		@Test
		@DisplayName("Array[0, 3]")
		default void rwArrayType3() throws IOException {
			assertWriteAndRead("Array[0, 3]");
		}

		@Test
		@DisplayName("Array[Integer, 0, 15]")
		default void rwArrayType4() throws IOException {
			assertWriteAndRead("Array[Integer, 0, 15]");
		}

		@Test
		@DisplayName("Binary")
		default void rwBinaryType() throws IOException {
			assertWriteAndRead("Binary");
		}

		@Test
		@DisplayName("Boolean")
		default void rwBooleanType() throws IOException {
			assertWriteAndRead("Boolean");
		}

		@Test
		@DisplayName("Callable[String,Integer,Callable[[String],String]]")
		default void rwCallableType() throws IOException {
			assertWriteAndRead("Callable[String,Integer,Callable[[String],String]]");
		}

		@Test
		@DisplayName("Callable[[String,Integer,Callable[[String],String]],Float]")
		default void rwCallableType2() throws IOException {
			assertWriteAndRead("Callable[[String,Integer,Callable[[String],String]],Float]");
		}

		@Test
		@DisplayName("CatalogEntry")
		default void rwCatalogEntryType() throws IOException {
			assertWriteAndRead("CatalogEntry");
		}

		@Test
		@DisplayName("Class['MyClass']")
		default void rwClassType() throws IOException {
			assertWriteAndRead("Class['MyClass']");
		}

		@Test
		@DisplayName("Collection[10,20]")
		default void rwCollectionType() throws IOException {
			assertWriteAndRead("Collection[10,20]");
		}

		@Test
		@DisplayName("Data")
		default void rwDataType() throws IOException {
			assertWriteAndRead("Data");
		}

		@Test
		@DisplayName("Default")
		default void rwDefaultType() throws IOException {
			assertWriteAndRead("Default");
		}

		@Test
		@DisplayName("Enum[x,y,z]")
		default void rwEnumType() throws IOException {
			assertWriteAndRead("Enum[x,y,z]");
		}

		@Test
		@DisplayName("Float[-4.0,3.8]")
		default void rwFloatType() throws IOException {
			assertWriteAndRead("Float[-4.0,3.8]");
		}

		@Test
		@DisplayName("Hash[String,Integer]")
		default void rwHashType() throws IOException {
			assertWriteAndRead("Hash[String,Float]");
		}

		@Test
		@DisplayName("Hash[String,Integer,3,5]")
		default void rwHashType2() throws IOException {
			assertWriteAndRead("Hash[String,Type,3,5]");
		}

		@Test
		@DisplayName("Integer[-3,5]")
		default void rwIntegerType() throws IOException {
			assertWriteAndRead("Integer[-3,5]");
		}

		@Test
		@DisplayName("Iterable[Float]")
		default void rwIterableType() throws IOException {
			assertWriteAndRead("Iterable[Float]");
		}

		@Test
		@DisplayName("Iterator[String]")
		default void rwIteratorType() throws IOException {
			assertWriteAndRead("Iterator[String]");
		}

		@Test
		@DisplayName("NotUndef[Unit]")
		default void rwNotUndefType() throws IOException {
			assertWriteAndRead("NotUndef[Unit]");
		}

		@Test
		@DisplayName("Numeric")
		default void rwNumericType() throws IOException {
			assertWriteAndRead("Numeric");
		}

		@Test
		@DisplayName("Object[{name => 'Hi', attributes => { 'world' => String[1] }}]")
		default void rwObjectType() throws IOException {
			assertWriteAndRead("Object[{name => 'Hi', attributes => { 'world' => String[1] }}]");
		}

		@Test
		@DisplayName("Optional[Boolean]")
		default void rwOptionalType() throws IOException {
			assertWriteAndRead("Optional[Boolean]");
		}

		@Test
		@DisplayName("Pattern[/rx1/, /rx2/]")
		default void rwPatternType() throws IOException {
			assertWriteAndRead("Pattern[/rx1/, /rx2/]");
		}

		@Test
		@DisplayName("Regexp['rx1']")
		default void rwRegexpType() throws IOException {
			assertWriteAndRead("Regexp['rx1']");
		}

		@Test
		@DisplayName("Resource['type', 'title']")
		default void rwResourceType() throws IOException {
			assertWriteAndRead("Resource['type', 'title']");
		}

		@Test
		@DisplayName("Runtime['ruby', 'Some::Type']")
		default void rwRuntimeType() throws IOException {
			assertWriteAndRead("Runtime['ruby', 'Some::Type']");
		}

		@Test
		@DisplayName("ScalarData")
		default void rwScalarDataType() throws IOException {
			assertWriteAndRead("ScalarData");
		}

		@Test
		@DisplayName("Scalar")
		default void rwScalarType() throws IOException {
			assertWriteAndRead("Scalar");
		}

		@Test
		@DisplayName("SemVerRange")
		default void rwSemVerRangeType() throws IOException {
			assertWriteAndRead("SemVerRange");
		}

		@Test
		@DisplayName("SemVer")
		default void rwSemVerType() throws IOException {
			assertWriteAndRead("SemVer");
		}

		@Test
		@DisplayName("SemVer['>=6.5.4']")
		default void rwSemVerType2() throws IOException {
			assertWriteAndRead("SemVer['>=6.5.4']");
		}

		@Test
		@DisplayName("String")
		default void rwStringType() throws IOException {
			assertWriteAndRead("String");
		}

		@Test
		@DisplayName("String[3, 10]")
		default void rwStringType2() throws IOException {
			assertWriteAndRead("String[3, 10]");
		}

		@Test
		@DisplayName("String['foo']")
		default void rwStringType3() throws IOException {
			assertWriteAndRead("String['foo']");
		}

		@Test
		@DisplayName("Struct[{Optional['a'] => String[1], 'b' => Optional[String[1]]}]")
		default void rwStructType() throws IOException {
			assertWriteAndRead("Struct[{Optional['a'] => String[1], 'b' => Optional[String[1]]}]");
		}

		@Test
		@DisplayName("TimeSpan['0-00:00:00', '0-11:00:00']")
		default void rwTimeSpanType() throws IOException {
			assertWriteAndRead("TimeSpan['0-00:00:00', '0-11:00:00']");
		}

		@Test
		@DisplayName("Timestamp['1962-11-24T23:11:10', '1979-03-08T12:10:01']")
		default void rwTimestampType() throws IOException {
			assertWriteAndRead("Timestamp['1962-11-24T23:11:10', '1979-03-08T12:10:01']");
		}

		@Test
		@DisplayName("Tuple[String,Integer,Timestamp,3,8]")
		default void rwTupleType() throws IOException {
			assertWriteAndRead("Tuple[String,Integer,Timestamp,3,8]");
		}

		@Test
		@DisplayName("MyAlias = Tuple[String,Integer,Timestamp,3,8]")
		default void rwTypeAliasType() throws IOException {
			assertWriteAndRead("MyAlias = Tuple[String,Integer,Timestamp,3,8]");
		}

		@Test
		@DisplayName("TypeReference['Some::NonExisting::Type']")
		default void rwTypeReferenceType() throws IOException {
			assertWriteAndRead("TypeReference['Some::NonExisting::Type']");
		}

		@Test
		@DisplayName("TypeSet[...]")
		default void rwTypeSetType() throws IOException {
			TypeEvaluator te = typeEvaluator();
			te.declareType("Transports", "TypeSet[{pcore_version => '1.0.0', version => '1.0.0'}]");

			assertWriteAndRead("TypeSet[{pcore_version => '1.0.0', version => '1.0.0', " +
					"types => {PositiveInt => Integer[0, default], NegativeInt => Integer[default, -1]}, " +
					"references => {T => {name => 'Transports', version_range => '1.x'}}}]");
		}

		@Test
		@DisplayName("Type[String]")
		default void rwTypeType() throws IOException {
			assertWriteAndRead("Type[String]");
		}

		@Test
		@DisplayName("Undef")
		default void rwUndefType() throws IOException {
			assertWriteAndRead("Undef");
		}

		@Test
		@DisplayName("Unit")
		default void rwUnitType() throws IOException {
			assertWriteAndRead("Unit");
		}

		@Test
		@DisplayName("Variant[String,Float,Boolean,Undef]")
		default void rwVariantType() throws IOException {
			assertWriteAndRead("Variant[String,Float,Boolean,Undef]");
		}

		void assertWriteAndRead(String typeString) throws IOException;
	}

	@Nested
	@DisplayName("using JSON")
	class JsonSerializationTest {
		@Nested
		@DisplayName("can write and read an object of type")
		class RuntimeTypesImpl implements RuntimeTypes {
			@Override
			public Object writeAndRead(Object value) throws IOException {
				return SerializationTest.this.writeAndRead(value, SerializationFactory.JSON);
			}
		}

		@Nested
		@DisplayName("Pcore Types")
		class PackPcoreTypesImpl implements PcoreTypes {
			@Override
			public void assertWriteAndRead(String typeString) throws IOException {
				SerializationTest.this.assertWriteAndRead(typeString, SerializationFactory.JSON);
			}
		}
	}

	@Nested
	@DisplayName("using MsgPack")
	class MsgPackSerializationTest {
		@Nested
		@DisplayName("can write and read an object of type")
		class RuntimeTypesImpl implements RuntimeTypes {
			@Override
			public Object writeAndRead(Object value) throws IOException {
				return SerializationTest.this.writeAndRead(value, SerializationFactory.MSGPACK);
			}
		}

		@Nested
		@DisplayName("Pcore Types")
		class PackPcoreTypesImpl implements PcoreTypes {
			@Override
			public void assertWriteAndRead(String typeString) throws IOException {
				SerializationTest.this.assertWriteAndRead(typeString, SerializationFactory.MSGPACK);
			}
		}
	}

	void assertWriteAndRead(String typeString, String factoryName) throws IOException {
		TypeEvaluator te = typeEvaluator();
		Type type = ((AnyType)te.resolveType(typeString)).resolve();
		assertEquals(type, ((AnyType)writeAndRead(type, factoryName)).resolve());
	}

	Object writeAndRead(Object value, String factoryName) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SerializationFactory factory = Pcore.serializationFactory(factoryName);
		Serializer writer = factory.forOutput(emptyMap(), out);
		writer.write(value);
		writer.finish();
		Deserializer reader = factory.forInput(new ByteArrayInputStream(out.toByteArray()));
		return reader.read();
	}
}

