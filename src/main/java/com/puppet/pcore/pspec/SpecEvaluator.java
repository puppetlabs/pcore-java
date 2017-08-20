package com.puppet.pcore.pspec;

import com.puppet.pcore.Issue;
import com.puppet.pcore.IssueException;
import com.puppet.pcore.ReportedIssue;
import com.puppet.pcore.Severity;
import com.puppet.pcore.impl.Polymorphic;
import com.puppet.pcore.impl.parser.ParseLocation;
import com.puppet.pcore.impl.parser.Parser;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.ParseIssue;
import com.puppet.pcore.parser.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Helpers.entry;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.pspec.SpecIssue.*;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class SpecEvaluator extends Polymorphic<Object> {
	// A parser that will handle back-ticked strings
	private final Parser parser = new Parser(true);

	private final Stack<Expression> path = new Stack<>();

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(SpecEvaluator.class, "eval");

	private final Map<String, SpecFunction> functions = asMap(
		entry("Example", this::example),
		entry("Parses_to", this::parses_to),
		entry("Given", this::given),
		entry("Source", this::source),
		entry("Validates_with", this::validates_with),
		entry("Error", this::error),
		entry("Warning", this::warning)
	);

	private final Assertions assertions;

	<T> T argument(String name, Expression semantic, Class<T> expectedClass, int index, List<Object> args) {
		Object v = args.get(index);
		if(v == null)
			throw specError(SPEC_MISSING_ARGUMENT, semantic, name, index + 1, expectedClass.getName());
		if(expectedClass.isInstance(v))
			return expectedClass.cast(v);
		throw specError(SPEC_ILLEGAL_ARGUMENT_TYPE, semantic, name, index + 1, expectedClass.getName(), v.getClass().getName());
	}

	<T> T argument(String name, Expression semantic, Class<T> expectedClass, T defaultValue, int index, List<Object> args) {
		Object v = args.get(index);
		if(v == null)
			return defaultValue;
		if(expectedClass.isInstance(v))
			return expectedClass.cast(v);
		throw specError(SPEC_ILLEGAL_ARGUMENT_TYPE, semantic, name, index + 1, expectedClass.getName(), v.getClass().getName());
	}

	public static class Test {
		public final String name;

		public final Executable test;

		public Test(String name, Executable test) {
			this.name = name;
			this.test = test;
		}
	}

	@FunctionalInterface
	public interface Executable {
		void execute() throws Throwable;
	}

	public interface Assertions {
		void assertEquals(Object a, Object b);

		void fail(String message);
	}

	public interface Result<T> {
		Executable createTest(T actual);
	}

	public interface Input {
		List<Executable> createOkTests(Result<Expression> expected);

		List<Executable> createIssuesTests(Result<List<ReportedIssue>> expected);
	}

	public static class ValidationResult {
		public final Issue issue;

		public final Severity severity;

		public static ValidationResult warning(Issue issue) {
			return new ValidationResult(Severity.WARNING, issue);
		}

		public static ValidationResult error(Issue issue) {
			return new ValidationResult(Severity.ERROR, issue);
		}

		private ValidationResult(Severity severity, Issue issue) {
			this.severity = severity;
			this.issue = issue;
		}
	}

	public class ValidatesWith implements Result<List<ReportedIssue>> {
		public final List<ValidationResult> expectedIssues;

		public ValidatesWith(List<ValidationResult> expectedIssues) {
			this.expectedIssues = expectedIssues;
		}

		@Override
		public Executable createTest(List<ReportedIssue> actual) {
			return () -> {
				StringBuilder bld = new StringBuilder();
				nextExpected: for(ValidationResult result : expectedIssues) {
					for(ReportedIssue issue : actual) {
						if(result.issue == issue.issue && result.severity == issue.severity)
							continue nextExpected;
					}
					bld.append("Expected ").append(result.severity).append(" issue ").append(result.issue).append(" but it was not produced");
				}

				nextIssue: for(ReportedIssue issue : actual) {
					for(ValidationResult result : expectedIssues) {
						if(result.issue == issue.issue && result.severity == issue.severity)
							continue nextIssue;
					}
					bld.append("Unexpected ").append(issue.severity).append(" issue ").append(issue.toString());
				}
				if(bld.length() > 0)
					assertions.fail(bld.toString());
			};
		}
	}

	public class ParseResult implements Result<Expression> {
		public final String result;

		public ParseResult(String result) {
			this.result = result;
		}

		@Override
		public Executable createTest(Expression actual) {
			return () -> assertions.assertEquals(result, actual.toPN().toString());
		}
	}

	public static class Given {
		public final List<Input> inputs;

		public Given(List<Input> inputs) {
			this.inputs = inputs;
		}
	}

	public static class Source implements Input {
		public final Parser parser = new Parser();

		public final List<String> sources;

		public Source(List<String> sources) {
			this.sources = sources;
		}

		@Override
		public List<Executable> createOkTests(Result<Expression> expected) {
			return map(sources, source -> createOkTest(source, expected));
		}

		@Override
		public List<Executable> createIssuesTests(Result<List<ReportedIssue>> expected) {
			return map(sources, source -> createIssueTest(source, expected));
		}

		protected Executable createOkTest(String source, Result<Expression> expected) {
			Expression parsed = parser.parse(null, source, false, true);
			return expected.createTest(parsed);
		}

		protected Executable createIssueTest(String source, Result<List<ReportedIssue>> expected) {
			try {
				parser.parse(null, source);
				return expected.createTest(emptyList());
			} catch(IssueException e) {
				return expected.createTest(singletonList(e.reportedIssue()));
			}
		}
	}

	public static class Example {
		public final String description;
		public final Given given;
		public final Result result;

		public Example(String description, Given given, Result<?> result) {
			this.description = description;
			this.given = given;
			this.result = result;
		}

		Test createTest() {
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
			return new Test(description, () -> {
				for(Executable test : tests)
					test.execute();
			});
		}
	}

	@FunctionalInterface
	public interface SpecFunction {
		Object apply(Expression semantic, List<Object> args);
	}

	public SpecEvaluator(Assertions assertions) {
		this.assertions = assertions;
	}

	public Object example(Expression s, List<Object> args) {
		if(path.size() != 3 || !(path.get(0) instanceof Program && path.get(1) instanceof BlockExpression))
			throw specError(SPEC_NOT_TOP_EXPRESSION, s, "Example");

		if(args.size() != 3)
			throw specError(SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS, s, "Example", 3, args.size());

		return new Example(
				argument("Example", s, String.class, null, 0, args),
				argument("Example", s, Given.class, 1, args),
				argument("Example", s, Result.class, 2, args));
	}

	public Object given(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Given", "Example");
		int top = args.size();
		List<Input> inputs = new ArrayList<>(top);
		for(int idx = 0; idx < top; ++idx)
			inputs.add(argument("Given", s, Input.class, idx, args));
		return new Given(inputs);
	}

	public Object source(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Source", "Given");
		int top = args.size();
		List<String> sources = new ArrayList<>(top);
		for(int idx = 0; idx < top; ++idx)
			sources.add(argument("Source", s, String.class, idx, args));
		return new Source(sources);
	}

	public Object parses_to(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Parses_to", "Example");
		if(args.size() != 1)
			throw specError(SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS, s, "Parses_to", 1, args.size());
		return new ParseResult(argument("Parses_to", s, String.class, 0, args));
	}

	public Object validates_with(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Validates_with", "Example");
		int top = args.size();
		List<ValidationResult> results = new ArrayList<>(top);
		for(int idx = 0; idx < top; ++idx)
			results.add(argument("ValidationResult", s, ValidationResult.class, idx, args));
		return new ValidatesWith(results);
	}

	public Object error(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Error", "Validates_with");
		if(args.size() != 1)
			throw specError(SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS, s, "Error", 1, args.size());
		return ValidationResult.error(argument("Error", s, Issue.class, 0, args));
	}

	public Object warning(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Warning", "Validates_with");
		if(args.size() != 1)
			throw specError(SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS, s, "Warning", 1, args.size());
		return ValidationResult.warning(argument("Warning", s, Issue.class, 0, args));
	}

	private void assertVariableOrParameterTo(String exprName, String callName) {
		int pathSize = path.size();
		Expression self = path.get(pathSize - 1);
		if(pathSize > 1) {
			Expression container = path.get(pathSize - 2);
			if(container instanceof CallNamedFunctionExpression) {
				Expression functor = ((CallNamedFunctionExpression)container).functor;
				if(functor instanceof QualifiedReference && ((QualifiedReference)functor).name.equals(callName))
					return;
			} else if(container instanceof AssignmentExpression) {
				Expression lhs = ((AssignmentExpression)container).lhs;
				if(lhs instanceof VariableExpression) {
					// rhs can be assigned to variable, so this is OK
					return;
				}
			}
		}
		throw specError(SPEC_EXPRESSION_NOT_PARAMETER_TO, self, exprName, callName);
	}

	@Override
	protected DispatchMap getDispatchMap() {
		return dispatchMap;
	}

	IssueException specError(Issue issue, Expression semantic, Object...args) {
		return new IssueException(issue, args, new ParseLocation(((Positioned)semantic).locator, semantic.offset()));
	}

	public List<Test> createTests(String sourceName, String sourceContents) {
		return createTests(parser.parse(sourceName, sourceContents));
	}

	public List<Test> createTests(Expression expression) {
		path.clear();
		Object evalResult = dispatch(expression);
		List<Test> tests = new ArrayList<>();
		if(evalResult instanceof List<?>) {
			for(Object v : ((List<?>)evalResult)) {
				if(v instanceof Example)
					tests.add(((Example)v).createTest());
			}
		}
		return tests;
	}

	public Object eval(Program program) {
		path.push(program);
		Object result =  dispatch(program.body);
		path.pop();
		return result;
	}

	public Object eval(QualifiedReference qr) {
		SpecFunction func = functions.get(qr.name);
		if(func != null)
			return func;

		try {
			return ParseIssue.valueOf(qr.name);
		} catch(IllegalArgumentException e) {
			throw specError(SPEC_UNKNOWN_IDENTIFIER, qr, qr.name);
		}
	}

	public Object eval(LiteralString str) {
		return str.value;
	}

	public Object eval(BlockExpression block) {
		path.push(block);
		List<Object> result = map(block.statements, this::dispatch);
		path.pop();
		return result;
	}

	public Object eval(CallNamedFunctionExpression call) {
		path.push(call);
		Object functor = dispatch(call.functor);
		if(!(functor instanceof SpecFunction))
			throw specError(SPEC_ILLEGAL_CALL_RECEIVER, call.functor);
		Object result = ((SpecFunction)functor).apply(call, map(call.arguments, this::dispatch));
		path.pop();
		return result;
	}
}
