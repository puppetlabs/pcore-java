package com.puppet.pcore.pspec;

import java.util.ArrayList;
import java.util.List;

public class Example extends Node {
	public final Given given;
	public final Result result;

	public Example(String description, Given given, Result<?> result) {
		super(description);
		this.given = given;
		this.result = result;
	}

	public Test createTest() {
		List<Executable> tests = new ArrayList<>();
		if(result instanceof ValidatesWith) {
			ValidatesWith vw = (ValidatesWith)result;
			for(Input input : given.inputs)
				tests.addAll(input.createIssuesTests(vw));
		} else {
			ParseResult pr = (ParseResult)result;
			for(Input input : given.inputs)
				tests.addAll(input.createOkTests(pr));
		}
		return new TestExecutable(description, () -> {
			for(Executable test : tests)
				test.execute();
		});
	}
}
