package com.puppet.pcore.impl.types;

import com.puppet.pcore.Pcore;
import com.puppet.pcore.Type;
import com.puppet.pcore.TypeAssertionException;
import com.puppet.pcore.TypeResolverException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static com.puppet.pcore.TestHelper.assertIncludes;
import static com.puppet.pcore.TestHelper.assertMatches;
import static com.puppet.pcore.impl.Constants.RUNTIME_NAME_AUTHORITY;
import static java.lang.String.format;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("A TypeSetType")
public class TypeSetTypeTest extends DeclaredTypeTest {

	@BeforeEach
	public void init() {
		Pcore.reset();
	}

	@Nested
	@DisplayName("when validating the initialization hash")
	public class ValidatingI12n {
		@Nested
		@DisplayName("accepts")
		public class Accepts {
			@Test
			@DisplayName("no types and no references")
			public void noTypesAndNoRefs() {
				assertTrue(declareTypeSet("version => '1.0.0', pcore_version => '1.0.0'") != null);
			}

			@Test
			@DisplayName("only references")
			public void onlyRefs() {
				declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { Car => Object[{}] }");
				declareTypeSet("SecondSet", "version => '1.0.0', pcore_version => '1.0.0', references => {" +
						"First => { name => 'FirstSet', version_range => '1.x' }}");
				TypeSetType second = resolveTypeSet("SecondSet");
				assertTrue(second.get("First::Car") instanceof ObjectType);
			}

			@Test
			@DisplayName("multiple references to equally named TypeSets using different name authorities")
			public void refsSameNameDifferentNS() {
				declareTypeSet( "FirstSet", "version => '1.0.0', pcore_version => '1.0.0', types => { Car => Object[{}] }",
						URI.create("http://example.com/ns1"));
				declareTypeSet( "FirstSet", "version => '1.0.0', pcore_version => '1.0.0', types => { Car => Object[{}] }",
						URI.create("http://example.com/ns2"));
				declareTypeSet("SecondSet", "version => '1.0.0', pcore_version => '1.0.0', references => {" +
						"First_1 => { name_authority => 'http://example.com/ns1',name => 'FirstSet', version_range => '1.x' }," +
						"First_2 => { name_authority => 'http://example.com/ns2',name => 'FirstSet', version_range => '1.x' }" +
						"}");
				TypeSetType second = resolveTypeSet("SecondSet");
				assertTrue(second.get("First_1::Car") instanceof ObjectType);
				assertTrue(second.get("First_2::Car") instanceof ObjectType);
			}
		}

		@Nested
		@DisplayName("raises an error when")
		public class WillFail {
			@Test
			@DisplayName("pcore_version is missing")
			public void pcoreVersionMissing() {
				declareTypeSet("version => '1.0.0'");
				Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
				assertEquals("TypeSet initializer expects a value for key 'pcore_version'", ex.getMessage());
			}

			@Test
			@DisplayName("version is missing")
			public void versionMissing() {
				declareTypeSet("pcore_version => '1.0.0'");
				Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
				assertEquals("TypeSet initializer expects a value for key 'version'", ex.getMessage());
			}

			@Test
			@DisplayName("version is an invalid semantic version")
			public void versionInvalid() {
				declareTypeSet("version => '1.x', pcore_version => '1.0.0'");
				Throwable ex = assertThrows(IllegalArgumentException.class, () -> resolveTypeSet());
				assertEquals("The string '1.x' does not represent a valid semantic version", ex.getMessage());
			}

			@Test
			@DisplayName("pcore_version is an invalid semantic version")
			public void pcoreVersionInvalid() {
				declareTypeSet("version => '1.0.0', pcore_version => '1.x'");
				Throwable ex = assertThrows(IllegalArgumentException.class, () -> resolveTypeSet());
				assertEquals("The string '1.x' does not represent a valid semantic version", ex.getMessage());
			}

			@Test
			@DisplayName("the pcore_version is outside of the range of that is parsable by this runtime")
			public void pcoreVersionOutOfRange() {
				declareTypeSet("version => '1.0.0', pcore_version => '2.0.0'");
				Throwable ex = assertThrows(TypeResolverException.class, () -> resolveTypeSet());
				assertMatches("The pcore version .* Expected range 1\\.0\\.0, got 2\\.0\\.0", ex.getMessage());
			}

			@Test
			@DisplayName("the name authority is an invalid URI")
			public void nameAuthInvalid() {
				Throwable ex = assertThrows(IllegalArgumentException.class, () ->
						declareTypeSet("FirstSet", "version => '1.0.0', pcore_version => '1.0.0', name_authority => 'not a valid URI'", null));
				assertMatches("not a valid URI", ex.getMessage());
			}

			@Nested
			@DisplayName("the types hash")
			public class TypesHash {
				@Test
				@DisplayName("is empty")
				public void isEmpty() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => {}");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertEquals("TypeSet initializer entry 'types' expects attribute count to be at least 1, got 0", ex.getMessage());
				}

				@Test
				@DisplayName("is not a map")
				public void isNotAMap() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => []");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertEquals("TypeSet initializer entry 'types' expects a Hash value, got Array", ex.getMessage());
				}

				@Test
				@DisplayName("contains values that are not types")
				public void containsNonTypeValues() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { Car => 'brum' }");
					Throwable ex = assertThrows(TypeResolverException.class, () -> resolveTypeSet());
					assertEquals("''brum'' did not resolve to a Pcore type", ex.getMessage());
				}

				@Test
				@DisplayName("contains names that are not SimpleNames")
				public void complexNames() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { car => Integer }");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("expects a match for Pattern[/\\\\A[A-Z]\\\\w*\\\\z/], got 'car'", ex.getMessage());
				}
			}

			@Nested
			@DisplayName("the references hash")
			public class ReferencesHash {
				@Test
				@DisplayName("is empty")
				public void isEmpty() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => {}");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("expects attribute count to be at least 1, got 0", ex.getMessage());
				}

				@Test
				@DisplayName("is not a map")
				public void isNotAMap() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => []");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("expects a Hash value, got Array", ex.getMessage());
				}

				@Test
				@DisplayName("contains something other than reference initialization maps")
				public void isNotRefI12n() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => {Ref => 2}");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("entry 'Ref' expects a Struct value, got Integer", ex.getMessage());
				}

				@Test
				@DisplayName("contains several initialization that refers to the same TypeSet")
				public void dupRef() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', " +
							"references => {" +
							" A => { name => 'Vehicle::Cars', version_range => '1.x' }," +
							" V => { name => 'Vehicle::Cars', version_range => '1.x' }," +
							"}");
					Throwable ex = assertThrows(TypeResolverException.class, () -> resolveTypeSet());
					assertIncludes("TypeSet 'http://puppet.com/2016.1/runtime/Vehicle::Cars' more than once using overlapping version ranges", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization maps with an alias that collides with a type name")
				public void dupRefWithType() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', " +
							"types => {" +
							"  Car => Object[{}]" +
							"}," +
							"references => {" +
							"  Car => { name => 'Vehicle::Car', version_range => '1.x' }" +
							"}");
					Throwable ex = assertThrows(TypeResolverException.class, () -> resolveTypeSet());
					assertIncludes("TypeSet using alias 'Car'. The alias collides with the name of a declared type", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization map that has no version range")
				public void refNoRange() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => { Ref => { name => 'X' } }");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("entry 'Ref' expects a value for key 'version_range'", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization map that has no name")
				public void refNoName() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => { Ref => { version_range => '1.x' } }");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("entry 'Ref' expects a value for key 'name'", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization map that has a name that is not a QRef")
				public void refNameNotQRef() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => { Ref => { name => 'cars', version_range => '1.x' } }");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("entry 'Ref' entry 'name' expects a match for Pattern[/\\\\A[A-Z][\\\\w]*(?:::[A-Z][\\\\w]*)*\\\\z/], got 'cars'", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization map that has a version_range that is not a valid SemVer range")
				public void refRangeInvalid() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => { Ref => { name => 'Cars', version_range => 'X' } }");
					Throwable ex = assertThrows(IllegalArgumentException.class, () -> resolveTypeSet());
					assertIncludes("Expected one of '<', '>' or digit at position 0 in range 'X'", ex.getMessage());
				}

				@Test
				@DisplayName("contains an initialization map that has an alias that is not a SimpleName")
				public void refAliasInvalid() {
					declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', references => { 'cars' => { name => 'X', version_range => '1.x' } }");
					Throwable ex = assertThrows(TypeAssertionException.class, () -> resolveTypeSet());
					assertIncludes("key of entry 'cars' expects a match for Pattern[/\\\\A[A-Z]\\\\w*\\\\z/], got 'cars'", ex.getMessage());
				}
			}
		}
	}


	@Nested
	@DisplayName("when declaring types")
	public class DeclaringTypes {
		@Test
		@DisplayName("declares a type Alias")
		public void declaresAlias() {
			declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { PositiveInt => Integer[0, default] }");
			assertTrue(resolveTypeSet().get("PositiveInt") instanceof TypeAliasType);
		}

		@Test
		@DisplayName("declares an Object type")
		public void declaresObject() {
			declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { Complex => Object[{}] }");
			assertTrue(resolveTypeSet().get("Complex") instanceof ObjectType);
		}

		@Test
		@DisplayName("declares Object type that references other types in the same set")
		public void declaresObjectWithSetRefs() {
			declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', " +
					"types => {" +
					"  Real => Float," +
					"  Complex => Object[{" +
					"    attributes => {" +
					"      real => Real," +
					"      imaginary => Real" +
					"    }" +
					"  }]" +
					"}");
			TypeSetType ts = resolveTypeSet();
			assertTrue(ts.get("Complex") instanceof ObjectType);
			AnyType realType = ((ObjectType)ts.get("Complex")).getAttribute("real").type;
			assertTrue(realType instanceof TypeAliasType);
			assertTrue(realType.resolvedType() instanceof FloatType);
		}

		@Test
		@DisplayName("declares self referencing alias")
		public void declaresSelfRefAlias() {
			declareTypeSet("version => '1.0.0', pcore_version => '1.0.0', types => { Tree => Hash[String,Variant[String,Tree]] }");
			TypeSetType ts = resolveTypeSet();
			AnyType treeType = ts.get("Tree");
			assertTrue(treeType instanceof TypeAliasType);
			AnyType hashType = treeType.resolvedType();
			assertEquals(HashType.class, hashType.getClass());
			AnyType variantType = ((HashType)hashType).type;
			assertEquals(VariantType.class, variantType.getClass());
			assertTrue(((VariantType)variantType).types.contains(treeType));
		}

		@Test
		@DisplayName("declares type that references types in another TypeSet")
		public void declaresRefToOtherTS() {
			declareTypeSet("Vehicles", "version => '1.0.0', pcore_version => '1.0.0'," +
					"types => {" +
					"  Car => Object[{}]," +
					"  Bicycle => Object[{}]" +
					"}");
			declareTypeSet("TheSet", "version => '1.0.0', pcore_version => '1.0.0'," +
					"types => {" +
					"  Transports => Variant[Vecs::Car,Vecs::Bicycle]" +
					"}," +
					"references => {" +
					"  Vecs => {" +
					"    name => 'Vehicles'," +
					"    version_range => '1.x'" +
					"  }" +
					"}");
			TypeSetType ts = resolveTypeSet("TheSet");
			AnyType transportsType = ts.get("Transports").resolvedType();
			assertEquals(VariantType.class, transportsType.getClass());
			AnyType carType = ((VariantType)transportsType).types.get(0);
			assertEquals(ObjectType.class, carType.getClass());
		}

		@Test
		@DisplayName("declares type that references types in another TypeSet")
		public void declaresRefToTSInOtherTS() {
			declareTypeSet("Vehicles", "version => '1.0.0', pcore_version => '1.0.0'," +
					"types => {" +
					"  Car => Object[{}]," +
					"  Bicycle => Object[{}]" +
					"}");
			declareTypeSet("Transports", "version => '1.0.0', pcore_version => '1.0.0'," +
					"types => {" +
					"  Transports => Variant[Vecs::Car,Vecs::Bicycle]" +
					"}," +
					"references => {" +
					"  Vecs => {" +
					"    name => 'Vehicles'," +
					"    version_range => '1.x'" +
					"  }" +
					"}");
			declareTypeSet("TheSet", "version => '1.0.0', pcore_version => '1.0.0'," +
					"types => {" +
					"  MotorPowered => Variant[T::Vecs::Car]," +
					"  Pedaled => Variant[T::Vecs::Bicycle]," +
					"  All => T::Transports" +
					"}," +
					"references => {" +
					"  T => {" +
					"    name => 'Transports'," +
					"    version_range => '1.x'" +
					"  }" +
					"}");
			TypeSetType ts = resolveTypeSet("TheSet");
			AnyType transportsType = ts.get("All").resolvedType();
			assertEquals(VariantType.class, transportsType.getClass());
			AnyType bicycleType = ((VariantType)transportsType).types.get(1);
			assertEquals(ObjectType.class, bicycleType.getClass());
		}
	}

	TypeSetType declareTypeSet(String typeSetHash) {
		return declareTypeSet("FirstSet", typeSetHash, RUNTIME_NAME_AUTHORITY);
	}

	TypeSetType declareTypeSet(String name, String typeSetHash) {
		return declareTypeSet(name, typeSetHash, RUNTIME_NAME_AUTHORITY);
	}

	TypeSetType declareTypeSet(String name, String typeSetHash, URI ns) {
		return (TypeSetType)declareType(name, format("TypeSet[{%s}]", typeSetHash), ns);
	}

	TypeSetType resolveTypeSet(String name) {
		AnyType type = resolveType(name);
		if(type instanceof TypeSetType)
			return (TypeSetType)type;

		fail("resolveType did not result in an TypeSetType");
		return null;
	}

	TypeSetType resolveTypeSet() {
		return resolveTypeSet("FirstSet");
	}

}
