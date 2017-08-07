package com.puppet.pcore.impl;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Pcore;
import com.puppet.pcore.TypeEvaluator;
import com.puppet.pcore.impl.types.AnyType;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TypeFormatterTest {

	@Test
	public void anyTypeS() {
		assertEquals("Any", anyType().toString());
	}

	@Test
	public void arrayS() {
		assertEquals("[1, 2, 3]", format(asList(1, 2, 3)));
	}

	@Test
	public void arrayTypeEmptyS() {
		assertEquals("Array[0, 0]", arrayTypeEmpty().toString());
	}

	@Test
	public void arrayTypeS() {
		assertEquals("Array", arrayType().toString());
	}

	@Test
	public void arrayTypeWithType() {
		assertEquals("Array[String]", arrayType(stringType()).toString());
	}

	@Test
	public void arrayTypeWithTypeAndMaxSize() {
		assertEquals("Array[String, 0, 10]", arrayType(stringType(), integerType(0, 10)).toString());
	}

	@Test
	public void arrayTypeWithTypeAndMinSize() {
		assertEquals("Array[String, 5, default]", arrayType(stringType(), integerType(5)).toString());
	}

	@Test
	public void binaryS() {
		assertEquals("'YmluYXI='", format(Binary.fromBase64("YmluYXI=")));
	}

	@Test
	public void binaryTypeS() {
		assertEquals("Binary", binaryType().toString());
	}

	@Test
	public void booleanTypeS() {
		assertEquals("Boolean", booleanType().toString());
	}

	@Test
	public void byteS() {
		assertEquals("23", format((byte)23));
	}

	@Test
	public void callableTypeEmpty() {
		assertEquals("Callable[0, 0]", callableType(tupleType(emptyList(), integerType(0, 0))).toString());
	}

	@Test
	public void callableTypeEmptyWithReturn() {
		assertEquals("Callable[[0, 0], Float]", callableType(tupleType(emptyList(),
				integerType(0, 0)), null,
				floatType()).toString());
	}

	@Test
	public void callableTypeS() {
		assertEquals("Callable", callableType().toString());
	}

	@Test
	public void callableTypeWithCappedMax() {
		assertEquals("Callable[String, 0, 3]", callableType(tupleType(singletonList(stringType()), integerType(0, 3)))
				.toString());
	}

	@Test
	public void callableTypeWithInfiniteMax() {
		assertEquals("Callable[String, 0, default]", callableType(tupleType(singletonList(stringType()), integerType(0,
				Long.MAX_VALUE))).toString());
	}

	@Test
	public void callableTypeWithMinMax() {
		assertEquals("Callable[1, 2]", callableType(tupleType(emptyList(), integerType(1, 2))).toString());
	}

	@Test
	public void callableTypeWithParams() {
		assertEquals("Callable[String, Integer]", callableType(tupleType(asList(stringType(), integerType()))).toString());
	}

	@Test
	public void callableTypeWithParamsAndReturn() {
		assertEquals("Callable[[String, Integer], Float]", callableType(tupleType(asList(stringType(), integerType())),
				null, floatType()).toString());
	}

	@Test
	public void callableTypeWithParamsSizeAndBlock() {
		assertEquals("Callable[String, 1, 2, Callable]", callableType(tupleType(singletonList(stringType()), integerType
				(1, 2)), callableType()).toString());
	}

	@Test
	public void callableTypeWithReturn() {
		assertEquals("Callable[[0, 0], Float]", callableType(tupleType(emptyList()),
				null, floatType()).toString());
	}

	@Test
	public void catalogEntryS() {
		assertEquals("CatalogEntry", catalogEntryType().toString());
	}

	@Test
	public void classTypeS() {
		assertEquals("Class", classType().toString());
	}

	@Test
	public void collectionTypeS() {
		assertEquals("Collection", collectionType().toString());
	}

	@Test
	public void collectionTypeWithSize() {
		assertEquals("Collection[0, 38]", collectionType(0, 38).toString());
	}

	@Test
	public void dataTypeS() {
		assertEquals("Data", dataType().toString());
	}

	@Test
	public void defaultTypeS() {
		assertEquals("Default", defaultType().toString());
	}

	@Test
	public void doubleS() {
		assertEquals("23.5", format(23.5D));
	}

	@Test
	public void durationS() {
		assertEquals("'0-13:00:00.0'", format(Duration.ofHours(13)));
	}

	@Test
	public void enumTypeS() {
		assertEquals("Enum", enumType().toString());
	}

	@Test
	public void enumWithStrings() {
		assertEquals("Enum['a', 'b', 'c']", enumType("a", "b", "c").toString());
	}

	@Test
	public void falseS() {
		assertEquals("false", format(false));
	}

	@Test
	public void floatS() {
		assertEquals("23.5", format(23.5F));
	}

	@Test
	public void floatTypeS() {
		assertEquals("Float", floatType().toString());
	}

	@Test
	public void floatTypeWithBounds() {
		assertEquals("Float[3.8, 88.2]", floatType(3.8, 88.2).toString());
	}

	@Test
	public void floatTypeWithLowerBound() {
		assertEquals("Float[3.2, default]", floatType(3.2).toString());
	}

	@Test
	public void floatTypeWithUpperBound() {
		assertEquals("Float[default, 88.2]", floatType(Double.NEGATIVE_INFINITY, 88.2).toString());
	}

	@Test
	public void hashS() {
		assertEquals("{'a' => 32, 'b' => [1, 2, 3]}", format(asMap("a", 32, "b", asList(1, 2, 3))));
	}

	@Test
	public void hashTypeEmptyS() {
		assertEquals("Hash[0, 0]", hashTypeEmpty().toString());
	}

	@Test
	public void hashTypeS() {
		assertEquals("Hash", hashType().toString());
	}

	@Test
	public void hashTypeWithTypes() {
		assertEquals("Hash[String, Integer]", hashType(stringType(), integerType()).toString());
	}

	@Test
	public void hashTypeWithTypesAndSize() {
		assertEquals("Hash[String, Integer, 4, 9]", hashType(stringType(), integerType(), 4, 9).toString());
	}

	@Test
	public void illegalType() {
		assertThrows(IllegalArgumentException.class, () -> format(new Date()));
	}

	@Test
	public void indentedHash() {
		String v = "" +
				"{\n" +
				"  'a' => 32,\n" +
				"  'b' => [1, 2, {\n" +
				"    'c' => 'd'\n" +
				"  }]\n" +
				"}";
		StringBuilder out = new StringBuilder();
		new TypeFormatter(out, 0, 2, false, false).format(asMap('a', 32, 'b', asList(1, 2, asMap('c', 'd'))));
		assertEquals(v, out.toString());
	}

	@Test
	public void indentedHashWithStartIndent() {
		String v = "" +
				"  {\n" +
				"    'a' => 32,\n" +
				"    'b' => [1, 2, {\n" +
				"      'c' => 'd'\n" +
				"    }]\n" +
				"  }";
		StringBuilder out = new StringBuilder();
		new TypeFormatter(out, 1, 2, false, false).format(asMap('a', 32, 'b', asList(1, 2, asMap('c', 'd'))));
		assertEquals(v, out.toString());
	}

	@Test
	public void instantS() {
		assertEquals("'2016-11-01T14:58:30Z'", format(Instant.parse("2016-11-01T14:58:30Z")));
	}

	@Test
	public void intS() {
		assertEquals("23", format(23));
	}

	@Test
	public void iterableTypeS() {
		assertEquals("Iterable", iterableType().toString());
	}

	@Test
	public void iterableTypeWithType() {
		assertEquals("Iterable[String]", iterableType(stringType()).toString());
	}

	@Test
	public void iteratorTypeS() {
		assertEquals("Iterator", iteratorType().toString());
	}

	@Test
	public void iteratorTypeWithType() {
		assertEquals("Iterator[String]", iteratorType(stringType()).toString());
	}

	@Test
	public void longS() {
		assertEquals("23", format((long)23));
	}

	@Test
	public void numericTypeS() {
		assertEquals("Numeric", numericType().toString());
	}

	@Test
	public void objectTypeS() {
		assertEquals("Object", objectType().toString());
	}

	@Test
	public void objectTypeWithName() {
		TypeEvaluator typeEvaluator = Pcore.typeEvaluator();
		Map<String,Object> initHash = new HashMap<>();
		initHash.put(Constants.KEY_NAME, "TestObj");
		assertEquals("TestObj", objectType(initHash).resolve().toString());
	}

	@Test
	public void objectTypeWithStuff() {
		Map<String,Object> initHash = new LinkedHashMap<>();
		Map<String,Object> attrHash = new LinkedHashMap<>();
		Map<String,Object> funcHash = new LinkedHashMap<>();
		initHash.put(Constants.KEY_NAME, "TestObj");
		initHash.put(Constants.KEY_ATTRIBUTES, attrHash);
		initHash.put(Constants.KEY_FUNCTIONS, funcHash);
		initHash.put(Constants.KEY_EQUALITY, asList("a", "n"));

		attrHash.put("a", integerType());
		attrHash.put("n", integerType());

		Map<String,Object> bHash = new LinkedHashMap<>();
		bHash.put("kind", "constant");
		bHash.put("type", integerType());
		bHash.put("value", 32);
		attrHash.put("b", bHash);

		funcHash.put("f", callableType());

		assertEquals(
				"Object[{" +
						"name => 'TestObj', " +
						"attributes => {'a' => Integer, 'n' => Integer, 'b' => {type => Integer, kind => constant, value => 32}}, " +
						"functions => {'f' => Callable}, " +
						"equality => ['a', 'n']}]",
				objectType(initHash).resolve().toExpandedString());
	}

	@Test
	public void patternTypeS() {
		assertEquals("Pattern", patternType().toString());
	}

	@Test
	public void patternWithRegexps() {
		assertEquals("Pattern[/first/, /second/]", patternType(regexpType("first"), regexpType("second")).toString());
	}

	@Test
	public void regexpS() {
		assertEquals("/expr/", format(Pattern.compile("expr")));
	}

	@Test
	public void regexpTypeS() {
		assertEquals("Regexp", regexpType().toString());
	}

	@Test
	public void regexpWithPattern() {
		assertEquals("Regexp[/first/]", regexpType("first").toString());
	}

	@Test
	public void resourceTypeS() {
		assertEquals("Resource", resourceType().toString());
	}

	@Test
	public void resourceTypeWithName() {
		assertEquals("Foo::Feebar::Fim", resourceType("foo::feebar::fim").toString());
	}

	@Test
	public void resourceTypeWithNameAndTitle() {
		assertEquals("Foo::Feebar::Fim['entitled']", resourceType("foo::feebar::fim", "entitled").toString());
	}

	@Test
	public void resourceTypeWithRuntime() {
		assertEquals("Runtime['java', 'java.util.Date']", runtimeType("java", "java.util.Date").toString());
	}

	@Test
	public void runtimeTypeS() {
		assertEquals("Runtime", runtimeType().toString());
	}

	@Test
	public void scalarDataTypeS() {
		assertEquals("ScalarData", scalarDataType().toString());
	}

	@Test
	public void scalarTypeS() {
		assertEquals("Scalar", scalarType().toString());
	}

	@Test
	public void semVerRangeTypeS() {
		assertEquals("SemVerRange", semVerRangeType().toString());
	}

	@Test
	public void semVerTypeS() {
		assertEquals("SemVer", semVerType().toString());
	}

	@Test
	public void semVerTypeWithRanges() {
		assertEquals("SemVer['1.0.0 - 1.3.8', '1.4.0 - 2.0.0']",
				semVerType(asList(
						VersionRange.create("1.0.0 - 1.3.8"),
						VersionRange.create("1.4.0 - 2.0.0"))).toString());
	}

	@Test
	public void sensitiveTypeS() {
		assertEquals("Sensitive", sensitiveType().toString());
	}

	@Test
	public void sensitiveTypeWithTypeS() {
		assertEquals("Sensitive[Binary]", sensitiveType(binaryType()).toString());
	}

	@Test
	public void shortS() {
		assertEquals("23", format((short)23));
	}

	@Test
	public void stringS() {
		assertEquals("'string'", format("string"));
	}

	@Test
	public void stringTypeS() {
		assertEquals("String", stringType().toString());
	}

	@Test
	public void stringTypeWithSize() {
		assertEquals("String[4, 9]", stringType(4, 9).toString());
	}

	@Test
	public void stringTypeWithValue() {
		assertEquals("String", format(stringType("value")));
	}

	@Test
	public void stringTypeWithValueDebug() {
		assertEquals("String['value']", formatDebug(stringType("value")));
	}

	@Test
	public void structTypeWithMembers() {
		assertEquals("Struct[{'name' => String}]", structType(structElement("name", stringType())).toString());
	}

	@Test
	public void structTypeWithOptionalKeyAndValue() {
		assertEquals("Struct[{'name' => Optional[String]}]", structType(structElement(optionalType
				("name"), optionalType
				(stringType()))).toString());
	}

	@Test
	public void structTypeWithOptionalKeys() {
		assertEquals("Struct[{Optional['name'] => String}]", structType(structElement(optionalType("name"), stringType()))
				.toString());
	}

	@Test
	public void structTypeWithOptionalValues() {
		assertEquals("Struct[{NotUndef['name'] => Optional[String]}]", structType(structElement("name", optionalType
				(stringType()))).toString());
	}

	@Test
	public void timeSpanTypeS() {
		assertEquals("TimeSpan", timeSpanType().toString());
	}

	@Test
	public void timestampTypeS() {
		assertEquals("Timestamp", timestampType().toString());
	}

	@Test
	public void trueS() {
		assertEquals("true", format(true));
	}

	@Test
	public void tupleTypeS() {
		assertEquals("Tuple", tupleType().toString());
	}

	@Test
	public void tupleTypeWithTypeAndSize() {
		assertEquals("Tuple[String, 1, 1]", tupleType(singletonList(stringType()), integerType(1, 1)).toString());
	}

	@Test
	public void tupleTypeWithTypeAndUnboundedMax() {
		assertEquals("Tuple[String, 2, default]", tupleType(singletonList(stringType()), integerType(2, Long.MAX_VALUE))
				.toString());
	}

	@Test
	public void tupleTypeWithTypes() {
		assertEquals("Tuple[Integer, Float, String]", tupleType(asList(integerType(), floatType(), stringType())).toString());
	}

	@Test
	public void typeReferenceTypeS() {
		assertEquals("TypeReference", typeReferenceType().toString());
	}

	@Test
	public void typeReferenceWithType() {
		assertEquals("TypeReference['The::Test[\\'hello\\']']", typeReferenceType("The::Test['hello']").toString());
	}

	@Test
	public void typeSetTypeS() {
		assertEquals(
				"TypeSet[{pcore_version => '1.0.0', name_authority => 'http://puppet.com/2016.1/runtime', version => '1.0.0', " +
				"types => {PositiveInt => Integer[0, default], NegativeInt => Integer[default, -1]}, " +
				"references => {T => {name => 'Transports', version_range => '1.x'}}}]",
				typeSetType(asMap(
					"version", "1.0.0",
					"pcore_version", "1.0.0",
					"types", asMap(
							"PositiveInt", integerType(0),
							"NegativeInt", integerType(Long.MIN_VALUE, -1)),
					"references", asMap(
							"T", asMap(
									"name", "Transports",
									"version_range", "1.x"
							)
						)
					)
				).toString()
		);
	}

	@Test
	public void typeTypeS() {
		assertEquals("Type", typeType().toString());
	}

	@Test
	public void typeTypeWithType() {
		assertEquals("Type[Integer[3, 5]]", typeType(integerType(3, 5)).toString());
	}

	@Test
	public void typeAlias() {
		TypeEvaluator typeEvaluator = Pcore.typeEvaluator();
		AnyType alias = typeAliasType("MyAlias", typeReferenceType("String[1,20]")).resolve();
		assertEquals("MyAlias", alias.toString());
		assertEquals("MyAlias = String[1, 20]", alias.toExpandedString());
	}

	@Test
	public void undefS() {
		assertEquals("?", format(null));
	}

	@Test
	public void undefTypeS() {
		assertEquals("Undef", undefType().toString());
	}

	@Test
	public void unitTypeS() {
		assertEquals("Unit", unitType().toString());
	}

	@Test
	public void variantTypeS() {
		assertEquals("Variant", variantType().toString());
	}

	@Test
	public void variantTypeWithVariants() {
		assertEquals("Variant[Integer, Float, Undef]", variantType(integerType(), floatType(), undefType()).toString());
	}

	@Test
	public void versionRangeS() {
		assertEquals("'>=1.2.3'", format(VersionRange.create(">=1.2.3")));
	}

	@Test
	public void versionS() {
		assertEquals("'1.2.3-alpha'", format(Version.create("1.2.3-alpha")));
	}

	private String format(Object object) {
		StringBuilder out = new StringBuilder();
		new TypeFormatter(out).format(object);
		return out.toString();
	}

	private String formatDebug(Object object) {
		StringBuilder out = new StringBuilder();
		new TypeFormatter(out, 0, 0, false, true).format(object);
		return out.toString();
	}
}
