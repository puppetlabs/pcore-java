package com.puppet.pcore.impl.types;

import com.puppet.pcore.*;
import com.puppet.pcore.impl.types.ObjectType.Attribute;
import com.puppet.pcore.semver.VersionRange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static com.puppet.pcore.TestHelper.assertIncludes;
import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.types.TypeFactory.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("A Pcore Object Type")
public class ObjectTypeTest extends DeclaredTypeTestBase {
	@BeforeEach
	public void init() {
		Pcore.reset();
	}

	@Nested
	@DisplayName("when dealing with attributes")
	class Attributes {
		@Nested
		@DisplayName("raises an error when")
		class Failures {
			@Test
			@DisplayName("the attribute type is not a type")
			public void notType() {
				declareObject("attributes => { a => 23 }");
				Throwable ex = assertThrows(TypeAssertionException.class, ObjectTypeTest.this::resolveObject);
				assertEquals("initializer for attribute TestObj[a] expects a value of type Type or Struct, got Integer", ex.getMessage());
			}

			@Test
			@DisplayName("the attribute type is missing")
			public void typeMissing() {
				declareObject("attributes => { a=> { kind => derived }}");
				Throwable ex = assertThrows(TypeAssertionException.class, ObjectTypeTest.this::resolveObject);
				assertIncludes("expects a value for key 'type'", ex.getMessage());
			}

			@Test
			@DisplayName("value is of incompatible type")
			public void valueNotOfAttrType() {
				declareObject("attributes => { a=> { type => Integer, value => 'three' }}");
				Throwable ex = assertThrows(TypeAssertionException.class, ObjectTypeTest.this::resolveObject);
				assertEquals("attribute TestObj[a] value expects an Integer value, got String", ex.getMessage());
			}

			@Test
			@DisplayName("the kind is invalid'")
			public void kindInvalid() {
				declareObject("attributes => { a=> { type => String, kind => derivd }}");
				Throwable ex = assertThrows(TypeAssertionException.class, ObjectTypeTest.this::resolveObject);
				assertIncludes("entry 'kind' expects a match for Enum['constant', 'derived', 'given_or_derived'], got 'derivd'",
						ex.getMessage());
			}

			@Test
			@DisplayName("value is requested but no value has been declared'")
			public void noValue() {
				declareObject("attributes => { a=> Integer }");
				Attribute attr = resolveObject().getAttribute("a");
				Throwable ex = assertThrows(TypeResolverException.class, attr::value);
				assertEquals("attribute TestObj[a] has no value", ex.getMessage());
			}
		}

		@Test
		@DisplayName("stores value in attribute'")
		public void valueStored() {
			declareObject("attributes => { a=> { type => Integer, value => 3 }}");
			Attribute attr = resolveObject().getAttribute("a");
			assertEquals(ObjectType.Attribute.class, attr.getClass());
			assertEquals(attr.value(), 3L);
		}

		@Test
		@DisplayName("attribute with defined value responds true to hasValue()'")
		public void hasValue() {
			declareObject("attributes => { a=> { type => Integer, value => 3 }}");
			Attribute attr = resolveObject().getAttribute("a");
			assertTrue(attr.hasValue());
		}

		@Test
		@DisplayName("attribute without defined value responds false to hasValue()'")
		public void hasNoValue() {
			declareObject("attributes => { a=> Integer }");
			Attribute attr = resolveObject().getAttribute("a");
			assertFalse(attr.hasValue());
		}

		@Nested
		@DisplayName("that are constants")
		class Constants {
			@Test
			@DisplayName("sets final => true")
			public void isFinal() {
				declareObject("attributes => { a=> { type => Integer, kind => constant, value => 3 }}");
				Attribute attr = resolveObject().getAttribute("a");
				assertTrue(attr.isFinal());
			}

			@Test
			@DisplayName("raises an error when no value is declared")
			public void missingValue() {
				declareObject("attributes => { a=> { type => Integer, kind => constant }}");
				Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
				assertEquals("attribute TestObj[a] of kind  of kind 'constant' requires a value", ex.getMessage());
			}

			@Test
			@DisplayName("raises an error when final => false")
			public void finalFalse() {
				declareObject("attributes => { a=> { type => Integer, kind => constant, final => false }}");
				Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
				assertEquals("attribute TestObj[a] of kind 'constant' cannot be combined with final => false", ex.getMessage());
			}

			@Test
			@DisplayName("getAttribute returns an Attribute")
			public void getAttribute() {
				declareObject("attributes => { a=> { type => Integer }}");
				assertTrue(resolveObject().getAttribute("a") != null);
			}

			@Test
			@DisplayName("getFunction returns null")
			public void getFunction() {
				declareObject("attributes => { a=> { type => Integer }}");
				assertTrue(resolveObject().getFunction("a") == null);
			}

			@Test
			@DisplayName("getMember returns an Attribute")
			public void getMember() {
				declareObject("attributes => { a=> { type => Integer }}");
				assertTrue(resolveObject().getMember("a") instanceof Attribute);
			}
		}
	}

	@Nested
	@DisplayName("when dealing with functions")
	class Functions {
		@Nested
		@DisplayName("raises an error when")
		class Failures {
			@Test
			@DisplayName("function type is not a Type[Callable]")
			public void notCallableType() {
				declareObject("functions => { a => String }");
				Throwable ex = assertThrows(TypeAssertionException.class, ObjectTypeTest.this::resolveObject);
				assertIncludes("initializer for function TestObj[a] expects a value of type Type[Callable] or Struct", ex.getMessage());
			}

			@Test
			@DisplayName("function has same name as attribute")
			public void sameNameAsAttribute() {
				declareObject("attributes => { a => Integer }, functions => { a => Callable }");
				Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
				assertEquals("function TestObj[a] conflicts with attribute with the same name", ex.getMessage());
			}
		}

		@Test
		@DisplayName("getAttribute returns null")
		public void getAttribute() {
			declareObject("functions => { a=> Callable }");
			assertTrue(resolveObject().getAttribute("a") == null);
		}

		@Test
		@DisplayName("getAttribute returns an Function")
		public void getFunction() {
			declareObject("functions => { a=> Callable }");
			assertTrue(resolveObject().getFunction("a") != null);
		}

		@Test
		@DisplayName("getMember returns an Function")
		public void getMember() {
			declareObject("functions => { a=> Callable }");
			assertTrue(resolveObject().getMember("a") instanceof ObjectType.Function);
		}
	}

	@Nested
	@DisplayName("when dealing with overrides")
	class Overrides {
		@Test
		@DisplayName("can redefine inherited member to assignable type")
		public void redefineToAssignable() {
			declareObject("TestSub",
					declareObject("attributes => { a => Integer }"),
					"attributes => { a => { type => Integer[0,10], override => true }}");
			Attribute attr = resolveObject("TestSub").getAttribute("a");
			assertEquals(integerType(0,10), attr.type);
		}

		@Nested
		@DisplayName("raises an error")
		class Failures {
			@Test
			@DisplayName("when an attribute overrides a function")
			public void attributeOverridesFunction() {
				declareObject("TestSub",
						declareObject("attributes => { a => Integer }"),
						"functions => { a => { type => Callable, override => true }}");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("function TestSub[a] attempts to override attribute TestObj[a]", ex.getMessage());
			}

			@Test
			@DisplayName("when a function overrides an attribute")
			public void functionOverridesAttribute() {
				declareObject("TestSub",
						declareObject("functions => { a => Callable }"),
						"attributes => { a => { type => Integer, override => true }}");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("attribute TestSub[a] attempts to override function TestObj[a]", ex.getMessage());
			}

			@Test
			@DisplayName("on attempts to redefine inherited member to unassignable type")
			public void redefineToNotAssignable() {
				declareObject("TestSub",
						declareObject("attributes => { a => Integer }"),
						"attributes => { a => { type => String, override => true }}");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("attribute TestSub[a] attempts to override attribute TestObj[a] with a type that does not match", ex.getMessage());
			}

			@Test
			@DisplayName("when an attribute overrides a final attribute")
			public void overrideFinal() {
				declareObject("TestSub",
						declareObject("attributes => { a => { type => Integer, final => true }}"),
						"attributes => { a => { type => Integer, override => true }}");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("attribute TestSub[a] attempts to override final attribute TestObj[a]", ex.getMessage());
			}

			@Test
			@DisplayName("when an overriding attribute is not declared with override => true")
			public void overrideWithoutOverride() {
				declareObject("TestSub",
						declareObject("attributes => { a => Integer }"),
						"attributes => { a =>  Integer }");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("attribute TestSub[a] attempts to override attribute TestObj[a] without having override => true", ex.getMessage());
			}

			@Test
			@DisplayName("when an attribute declared with override => true does not override")
			public void withOverrideDoesNotOverride() {
				declareObject("TestSub",
						declareObject("attributes => { a => Integer }"),
						"attributes => { b => { type => Integer, override => true }}");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("TestSub"));
				assertEquals("expected attribute TestSub[b] to override inherited attribute, but no such attribute was found", ex.getMessage());
			}
		}
	}

	@Nested
	@DisplayName("when dealing with equality")
	class Equality {
		@Test
		@DisplayName("the attributes can be declared as an array of names")
		public void equalityArray() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => Integer\n" +
					"},\n" +
					"equality => [a,b]");
			ObjectType t = resolveObject();
			assertEquals(t.declaredEquality(), asList("a", "b"));
			assertEquals(t.equality(), asList("a", "b"));
		}

		@Test
		@DisplayName("a single [<name>] can be declared as <name>")
		public void equalitySingle() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => Integer\n" +
					"},\n" +
					"equality => a");
			ObjectType t = resolveObject();
			assertEquals(t.equality(), asList("a"));
		}

		@Test
		@DisplayName("includes all non-constant attributes by default")
		public void equalityDefault() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => { type => Integer, kind => constant, value => 3 }," +
          "  c => Integer\n" +
					"}");
			ObjectType t = resolveObject();
			assertEquals(t.equality(), asList("a", "c"));
		}

		@Test
		@DisplayName("quality_include_type is true by default")
		public void equalityIncludeTypeDefault() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer\n" +
					"},\n" +
					"equality => a");
			ObjectType t = resolveObject();
			assertTrue(t.isEqualityIncludeType());
		}

		@Test
		@DisplayName("will allow an empty list of attributes")
		public void equalityEmptyList() {
			declareObject(
					"attributes => {\n" +
							"  a => Integer,\n" +
							"  b => Integer\n" +
							"},\n" +
							"equality => []");
			ObjectType t = resolveObject();
			assertEquals(t.equality(), emptyList());
		}

		@Test
		@DisplayName("will extend default equality in parent")
		public void extendParentDefault() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => Integer\n" +
					"}");
			declareObject("Sub", resolveObject(),
					"attributes => {\n" +
					"  c => Integer,\n" +
					"  d => Integer\n" +
					"}");
			ObjectType t = resolveObject("Sub");
			assertEquals(t.equality(), asList("a", "b", "c", "d"));
		}

		@Test
		@DisplayName("extends equality declared in parent")
		public void extendParent() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => Integer\n" +
					"},\n" +
					"equality => a");
			declareObject("Sub", resolveObject(),
					"attributes => {\n" +
					"  c => Integer,\n" +
					"  d => Integer\n" +
					"}");
			ObjectType t = resolveObject("Sub");
			assertEquals(t.equality(), asList("a", "c", "d"));
		}

		@Test
		@DisplayName("parent defined attributes can be included in equality if not already included by a parent")
		public void parentDefinedAttributes() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => Integer\n" +
					"},\n" +
					"equality => a");
			declareObject("Sub", resolveObject(),
					"attributes => {\n" +
					"  c => Integer,\n" +
					"  d => Integer\n" +
					"}\n," +
					"equality => [b,c]");
			ObjectType t = resolveObject("Sub");
			assertEquals(t.equality(), asList("a", "b", "c"));
		}

		@Test
		@DisplayName("raises an error when attempting to extend default equality in parent")
		public void failRedefineDefault() {
			declareObject(
					"attributes => {\n" +
							"  a => Integer,\n" +
							"  b => Integer\n" +
							"}");
			declareObject("Sub", resolveObject(),
					"attributes => {\n" +
							"  c => Integer,\n" +
							"  d => Integer\n" +
							"}\n," +
							"equality => a");
			Throwable ex = assertThrows(TypeResolverException.class, () -> resolveObject("Sub"));
			assertEquals("Sub equality is referencing attribute TestObj[a] which is included in equality of TestObj", ex.getMessage());
		}

		@Test
		@DisplayName("raises an error when equality references a constant attribute")
		public void equalityReferencesConstant() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer,\n" +
					"  b => { type => Integer, kind => constant, value => 3 }\n" +
					"},\n" +
					"equality => [a,b]");
			Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
			assertEquals("TestObj equality is referencing constant attribute TestObj[b]. Reference to constant is not allowed in equality", ex.getMessage());
		}

		@Test
		@DisplayName("raises an error when equality references a function")
		public void equalityReferencesFunction() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer\n" +
					"},\n" +
					"functions => {\n" +
					"  b => Callable\n" +
					"},\n" +
					"equality => [a,b]");
			Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
			assertEquals("TestObj equality is referencing function TestObj[b]. Only attribute references are allowed", ex.getMessage());
		}

		@Test
		@DisplayName("raises an error when equality references a non existent attributes")
		public void equalityReferencesNonExistent() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer\n" +
					"},\n" +
					"equality => [a,b]");
			Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
			assertEquals("TestObj equality is referencing non existent attribute 'b'", ex.getMessage());
		}

		@Test
		@DisplayName("raises an error when equality_include_type = false and attributes are provided")
		public void equalityNoIncludeTypeButAttributes() {
			declareObject(
					"attributes => {\n" +
					"  a => Integer\n" +
					"},\n" +
					"equality => a,\n" +
			    "equality_include_type => false");
			Throwable ex = assertThrows(TypeResolverException.class, ObjectTypeTest.this::resolveObject);
			assertEquals("TestObj equality_include_type = false cannot be combined with non empty equality specification", ex.getMessage());
		}
	}

	@Nested
	@DisplayName("when creating instances")
	class Creating {
		@Nested
		@DisplayName("that are DynamicObjects")
		class DynamicObjects {
			@Test
			@DisplayName("they use correct equality computation")
			public void equalityComputation() {
				declareObject(
						"attributes => {\n" +
						"  a => Float,\n" +
						"  b => Integer\n" +
						"},\n" +
						"equality => [a]");
				ObjectType t = resolveObject();
				DynamicObject a = (DynamicObject)t.newInstance((float)3.0, 5);
				assertEquals(3.0, a.get("a"));
				assertEquals(5L, a.get("b"));

				Object b = t.newInstance(3.0, 8);
				assertEquals(a, b);

				Object c = t.newInstance(5.0, 5);
				assertNotEquals(a, c);
			}

			@Test
			@DisplayName("they get correct defaults")
			public void equalityArray() {
				declareObject(
						"attributes => {\n" +
						"  a => Integer,\n" +
						"  b => { type => Integer, value => 8 }\n" +
						"},\n" +
						"equality => [a]");
				ObjectType t = resolveObject();
				DynamicObject a = (DynamicObject)t.newInstance(3);
				assertEquals(3L, a.get("a"));
				assertEquals(8L, a.get("b"));
			}

			@Test
			@DisplayName("raises exception for too few arguments")
			public void tooFewArguments() {
				declareObject(
						"attributes => {\n" +
								"  a => Integer,\n" +
								"  b => Integer\n" +
								"},\n" +
								"equality => [a]");
				ObjectType t = resolveObject();
				Throwable ex = assertThrows(TypeResolverException.class, () -> t.newInstance(3));
				assertEquals("Invalid number of type parameters specified: 'TestObj' requires 2 parameters, 1 provided", ex.getMessage());
			}

			@Test
			@DisplayName("raises exception for too many arguments")
			public void tooManyArguments() {
				declareObject(
						"attributes => {\n" +
								"  a => Integer,\n" +
								"  b => Integer\n" +
								"},\n" +
								"equality => [a]");
				ObjectType t = resolveObject();
				Throwable ex = assertThrows(TypeResolverException.class, () -> t.newInstance(3, 4, 5));
				assertEquals("Invalid number of type parameters specified: 'TestObj' requires 2 parameters, 3 provided", ex.getMessage());
			}
		}

		@Nested
		@DisplayName("that are Pcore Types, creates")
		class PcoreTypes {
			ObjectType resolveType(String typeName) {
				Type t = Pcore.typeEvaluator().resolveType(typeName);
				if(t instanceof ObjectType)
					return (ObjectType)t;
				fail("resolveType did not result in an ObjectType");
				return null;
			}

			@Test
			@DisplayName("Any")
			public void pAny() {
				Object result = resolveType("Pcore::AnyType").newInstance();
				assertEquals("Any", result.toString());
			}

			@Test
			@DisplayName("Array[String,5,100]")
			public void pArray() {
				Object result = resolveType("Pcore::ArrayType").newInstance(stringType(), integerType(5, 100));
				assertEquals("Array[String, 5, 100]", result.toString());
			}

			@Test
			@DisplayName("Binary")
			public void pBinary() {
				Object result = resolveType("Pcore::BinaryType").newInstance();
				assertEquals("Binary", result.toString());
			}

			@Test
			@DisplayName("Callable[[String, Integer, Callable[[0, 0], String]], Double]")
			public void pCallableWithReturn() {
				ObjectType callableMetaType = resolveType("Pcore::CallableType");
				Object result = callableMetaType.newInstance(
						tupleType(asList(stringType(), integerType())),
						callableMetaType.newInstance(tupleType(asList()), null, stringType()),
						floatType());
				assertEquals("Callable[[String, Integer, Callable[[0, 0], String]], Float]", result.toString());
			}

			@Test
			@DisplayName("Class['name']")
			public void pClass() {
				Object result = resolveType("Pcore::ClassType").newInstance("name");
				assertEquals("Class['name']", result.toString());
			}

			@Test
			@DisplayName("Collection[1, 3]")
			public void pCollection() {
				Object result = resolveType("Pcore::CollectionType").newInstance(integerType(1, 3));
				assertEquals("Collection[1, 3]", result.toString());
			}

			@Test
			@DisplayName("Enum['a', 'b', 'c']")
			public void pEnum() {
				Object result = resolveType("Pcore::EnumType").newInstance(asList("a", "b", "c"));
				assertEquals("Enum['a', 'b', 'c']", result.toString());
			}

			@Test
			@DisplayName("Float[-3.4, -2.04]")
			public void pFloat() {
				Object result = resolveType("Pcore::FloatType").newInstance(-3.4, -2.04);
				assertEquals("Float[-3.4, -2.04]", result.toString());
			}

			@Test
			@DisplayName("Hash[String,Integer,5,100]")
			public void pHash() {
				Object result = resolveType("Pcore::HashType").newInstance(stringType(), integerType(), integerType(5, 100));
				assertEquals("Hash[String, Integer, 5, 100]", result.toString());
			}

			@Test
			@DisplayName("Integer[-10, -5]")
			public void pInteger() {
				Object result = resolveType("Pcore::IntegerType").newInstance(-10, -5);
				assertEquals("Integer[-10, -5]", result.toString());
			}

			@Test
			@DisplayName("Iterable[String]")
			public void pIterable() {
				Object result = resolveType("Pcore::IterableType").newInstance(stringType());
				assertEquals("Iterable[String]", result.toString());
			}

			@Test
			@DisplayName("Iterator[String]")
			public void pIterator() {
				Object result = resolveType("Pcore::IteratorType").newInstance(stringType());
				assertEquals("Iterator[String]", result.toString());
			}

			@Test
			@DisplayName("NotUndef[Unit]")
			public void pNotUndef() {
				Object result = resolveType("Pcore::NotUndefType").newInstance(unitType());
				assertEquals("NotUndef[Unit]", result.toString());
			}

			@Test
			@DisplayName("Object[{name => 'MyObj', attributes => {'a' => Integer}}]")
			public void pObjectType() {
				Object result = resolveType("Pcore::ObjectType").newInstance(asMap("name", "MyObj", "attributes", asMap("a", integerType())));
				assertEquals("Object[{name => 'MyObj', attributes => {'a' => Integer}}]", ((ObjectType)result).resolve().toExpandedString());
			}

			@Test
			@DisplayName("Optional[Iterator[String]]")
			public void pOptional() {
				Object result = resolveType("Pcore::OptionalType").newInstance(iteratorType(stringType()));
				assertEquals("Optional[Iterator[String]]", result.toString());
			}

			@Test
			@DisplayName("Pattern[/pat1/, /pat2/]")
			public void pPattern() {
				Object result = resolveType("Pcore::PatternType").newInstance(asList(regexpType("pat1"), regexpType("pat2")));
				assertEquals("Pattern[/pat1/, /pat2/]", result.toString());
			}

			@Test
			@DisplayName("Regexp[/pat1/]")
			public void pRegexp() {
				Object result = resolveType("Pcore::RegexpType").newInstance("pat1");
				assertEquals("Regexp[/pat1/]", result.toString());
			}

			@Test
			@DisplayName("Resource['name','title']")
			public void pResource() {
				Object result = resolveType("Pcore::ResourceType").newInstance("name", "title");
				assertEquals("Name['title']", result.toString());
			}

			@Test
			@DisplayName("Runtime['ruby', 'Some::Class::Name']")
			public void pRuntime() {
				Object result = resolveType("Pcore::RuntimeType").newInstance("ruby", "Some::Class::Name");
				assertEquals("Runtime['ruby', 'Some::Class::Name']", result.toString());
			}

			@Test
			@DisplayName("SemVer['>=1.2.3', '<4.0.0']")
			public void pSemVer() {
				Object result = resolveType("Pcore::SemVerType").newInstance(asList(VersionRange.create(">=1.2.3"), VersionRange.create("<4.0.0")));
				assertEquals("SemVer['>=1.2.3', '<4.0.0']", result.toString());
			}

			@Test
			@DisplayName("Sensitive[Binary]")
			public void pSensitive() {
				Object result = resolveType("Pcore::SensitiveType").newInstance(binaryType());
				assertEquals("Sensitive[Binary]", result.toString());
			}

			@Test
			@DisplayName("String[10, default]")
			public void pString() {
				Object result = resolveType("Pcore::StringType").newInstance(integerType(10));
				assertEquals("String[10, default]", result.toString());
			}

			@Test
			@DisplayName("String (based from string)")
			public void pString2() {
				Object result = resolveType("Pcore::StringType").newInstance("abc");
				assertEquals("String", result.toString());
				assertEquals("String['abc']", ((Type)result).toDebugString());
			}

			@Test
			@DisplayName("Struct[{'a' => Integer, 'b' => String}]")
			public void pStruct() {
				Object result = resolveType("Pcore::StructType").newInstance(asList(structElement("a", integerType()), structElement("b", stringType())));
				assertEquals("Struct[{'a' => Integer, 'b' => String}]", result.toString());
			}

			@Test
			@DisplayName("TimeSpan['0-00:00:00', '1-00:00:00']")
			public void pTimeSpan() {
				Object result = resolveType("Pcore::TimeSpanType").newInstance(Duration.ofDays(0), Duration.ofDays(1));
				assertEquals("TimeSpan['0-00:00:00.0', '1-00:00:00.0']", result.toString());
			}

			@Test
			@DisplayName("Timestamp['2000-01-01T00:00:00', '2010-12-31T23:59:59']")
			public void pTimestamp() {
				Object result = resolveType("Pcore::TimestampType").newInstance(Instant.parse("2000-01-01T00:00:00.00Z"), Instant.parse("2010-12-31T23:59:59.999Z"));
				assertEquals("Timestamp['2000-01-01T00:00:00Z', '2010-12-31T23:59:59.999Z']", result.toString());
			}

			@Test
			@DisplayName("Tuple[TimeSpan,String]")
			public void pTuple() {
				Object result = resolveType("Pcore::TupleType").newInstance(asList(timeSpanType(), stringType()));
				assertEquals("Tuple[TimeSpan, String]", result.toString());
			}

			@Test
			@DisplayName("MyAlias = String[1, 20]")
			public void pTypeAlias() {
				Object result = resolveType("Pcore::TypeAliasType").newInstance("MyAlias", stringType(1,20));
				assertEquals("MyAlias", result.toString());
				assertEquals("MyAlias = String[1, 20]", ((Type)result).toExpandedString());
			}

			@Test
			@DisplayName("TypeReference['Some::Type']")
			public void pTypeReference() {
				Object result = resolveType("Pcore::TypeReferenceType").newInstance("Some::Type");
				assertEquals("TypeReference['Some::Type']", result.toString());
			}

			@Test
			@DisplayName("Type[String]")
			public void pTypeType() {
				Object result = resolveType("Pcore::TypeType").newInstance(stringType());
				assertEquals("Type[String]", result.toString());
			}

			@Test
			@DisplayName("Variant[Enum['a', 'b', 'c'], Integer]")
			public void pVariant() {
				Object result = resolveType("Pcore::VariantType").newInstance(asList(enumType("a", "b", "c"), integerType()));
				assertEquals("Variant[Enum['a', 'b', 'c'], Integer]", result.toString());
			}
		}
	}

	ObjectType declareObject(String objectHash) {
		return (ObjectType)declareType("TestObj", String.format("Object[{%s}]", objectHash));
	}

	ObjectType declareObject(String name, ObjectType parent, String objectHash) {
		return (ObjectType)declareType(name, String.format("Object[{parent => %s, %s}]", parent.name(), objectHash));
	}

	ObjectType resolveObject(String name) {
		return (ObjectType)resolveType(name);
	}

	ObjectType resolveObject() {
		return (ObjectType)resolveType("TestObj");
	}
}
