package com.puppet.pcore.pspec;

import com.puppet.pcore.test.PSpecAssertions;
import org.junit.jupiter.api.*;

import java.util.List;

import static com.puppet.pcore.test.TestHelper.dynamicPSpecTest;
import static com.puppet.pcore.test.TestHelper.readResource;

@DisplayName("pspecs")
public class SpecEvaluatorTest {
	private static SpecEvaluator evaluator;

	@BeforeAll
	static void init() {
		evaluator = new SpecEvaluator(PSpecAssertions.SINGLETON);
	}

	@TestFactory
	@DisplayName("tests.pspec")
	public List<DynamicTest> testFileSpec() {
		return dynamicPSpecTest(
				evaluator.createTests("tests.pspec",
						readResource(getClass(), "/tests.pspec")));
	}
}
