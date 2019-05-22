package com.puppet.pcore.impl.types;

import com.puppet.pcore.TypeResolverException;
import com.puppet.pcore.regex.Regexp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.puppet.pcore.impl.types.TypeFactory.typeAliasType;
import static com.puppet.pcore.impl.types.TypeFactory.typeReferenceType;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unused")
@DisplayName("A Pcore Type Alias")
public class TypeAliasTypeTest extends PcoreTestBase {

	@Test
	@DisplayName("resolves nested objects using self recursion")
	public void test1() {
		declareType("Tree", "Hash[String,Variant[String,Tree]]");
		assertTrue(resolveType("Tree").isInstance(singletonMap("a", singletonMap("b", singletonMap("c", "d")))));
	}

	@Test
	@DisplayName("finds mismatches using self recursion")
	public void test2() {
		declareType("Tree", "Hash[String,Variant[String,Tree]]");
		assertFalse(resolveType("Tree").isInstance(singletonMap("a", singletonMap("b", singletonMap("c", 1)))));
	}

	@Test
	@DisplayName("can directly reference itself in a variant with other types")
	public void test3() {
		declareType("Foo", "Variant[Foo,String]");
		assertTrue(resolveType("Foo").isInstance("a"));
	}

	@Test
	@DisplayName("can reference a variant that references an alias with a variant that references itself")
	public void test4() {
		declareType("X", "Variant[Y,Integer]");
		declareType("Y", "Variant[X,String]");
		AnyType theX = resolveType("X");
		AnyType theY = resolveType("Y");
		assertTrue(theX.isAssignable(theY));
		assertTrue(theY.isAssignable(theX));
	}

	@Test
	@DisplayName("detects a mismatch in an alias that directly references itself in a variant with other types")
	public void test5() {
		declareType("Foo", "Variant[Foo,String,Integer]");
		assertFalse(resolveType("Foo").isInstance(Regexp.compile("x")));
	}

	@Test
	@DisplayName("detects a non scalar correctly in combinations of nested aliased array with nested variants")
	public void test6() {
		declareType("Bar", "Variant[Foo,Integer]");
		declareType("Foo", "Array[Variant[Bar,String]]");
		AnyType theFoo = resolveType("Foo");
		assertTrue(theFoo.isInstance(singletonList("a")));
		assertTrue(theFoo.isInstance(singletonList(1)));
		assertFalse(theFoo.isInstance(Regexp.compile("x")));
	}

	@Test
	@DisplayName("detects a non scalar correctly in combinations of nested aliased variants with array")
	public void test7() {
		declareType("Bar", "Variant[Foo,Array[Integer]]");
		declareType("Foo", "Variant[Bar,Array[String]]");
		AnyType theFoo = resolveType("Foo");
		assertTrue(theFoo.isInstance(singletonList("a")));
		assertTrue(theFoo.isInstance(singletonList(1)));
		assertFalse(theFoo.isInstance(Regexp.compile("x")));
	}

	@Test
	@DisplayName("detects recursion in via Struct member")
	public void test8() {
		declareType("Foo", "Struct[{a => Foo}]");
		assertTrue(resolveType("Foo").isRecursive());
	}

	@Test
	@DisplayName("detects recursion in via Tuple member")
	public void test8a() {
		declareType("Foo", "Tuple[String, Foo, 2, 2]");
		assertTrue(resolveType("Foo").isRecursive());
	}

	@Test
	@DisplayName("can be resolved using a TypeReference")
	public void test9() {
		AnyType ta = typeAliasType("Foo", typeReferenceType("Integer[0]")).resolve(pcore());
		assertTrue(ta.isInstance(3));
		assertFalse(ta.isInstance(-3));
	}

	@Nested
	@DisplayName("cannot be resolved when")
	class Failures {
		@Test
		@DisplayName("an alias chain contains only aliases")
		public void test1() {
			declareType("Foo", "Bar");
			declareType("Fee", "Foo");
			declareType("Bar", "Fee");
			assertThrows(TypeResolverException.class, () -> resolveType("Bar"));
		}

		@Test
		@DisplayName("an alias chain contains nothing but aliases and variants")
		public void test2() {
			declareType("Foo", "Bar");
			declareType("Fee", "Foo");
			declareType("Bar", "Variant[Fee,Foo]");
			assertThrows(TypeResolverException.class, () -> resolveType("Bar"));
		}

		@Test
		@DisplayName("an alias directly references itself")
		public void test3() {
			declareType("Foo", "Foo");
			assertThrows(TypeResolverException.class, () -> resolveType("Foo"));
		}
	}
}
