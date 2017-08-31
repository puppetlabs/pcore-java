package com.puppet.pcore.pspec;

import com.puppet.pcore.Issue;
import com.puppet.pcore.IssueException;
import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.impl.Polymorphic;
import com.puppet.pcore.impl.parser.ParseLocation;
import com.puppet.pcore.impl.parser.Parser;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.ParseIssue;
import com.puppet.pcore.parser.model.*;

import java.util.*;

import static com.puppet.pcore.impl.Helpers.asMap;
import static com.puppet.pcore.impl.Helpers.entry;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.pspec.SpecIssue.*;

public class SpecEvaluator extends Polymorphic<Object> {
	// A parser that will handle back-ticked strings
	private final Parser parser = new Parser(true);

	private final Stack<Expression> path = new Stack<>();

	private static final DispatchMap dispatchMap = initPolymorphicDispatch(SpecEvaluator.class, "eval");

	private final Map<String, SpecFunction> functions = asMap(
		entry("Examples", this::examples),
		entry("Example", this::example),
		entry("Parses_to", this::parses_to),
		entry("Given", this::given),
		entry("Source", this::source),
		entry("Validates_with", this::validates_with),
		entry("Error", this::error),
		entry("Warning", this::warning),
		entry("Unindent", this::unindent)
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

	@FunctionalInterface
	public interface SpecFunction {
		Object apply(Expression semantic, List<Object> args);
	}

	public SpecEvaluator(Assertions assertions) {
		this.assertions = assertions;
	}

	protected boolean nested() {
		return !(path.size() == 3 && path.get(0) instanceof Program && path.get(1) instanceof BlockExpression);
	}

	public Object examples(Expression s, List<Object> args) {
		if(nested())
			assertVariableOrParameterTo("Examples", "Examples");

		String description = argument("Examples", s, String.class, 0, args);
		int top = args.size();
		List<Node> nodes = new ArrayList<>(top - 1);
		for(int idx = 1; idx < top; ++idx)
			nodes.add(argument("Examples", s, Node.class, idx, args));
		return new Examples(description, nodes);
	}

	public Object example(Expression s, List<Object> args) {
		if(nested())
			assertVariableOrParameterTo("Example", "Examples");

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
		for(int idx = 0; idx < top; ++idx) {
			Object arg = args.get(idx);
			inputs.add(
					arg instanceof String
							? new Source(Collections.singletonList((String)arg))
							: argument("Given", s, Input.class, idx, args));
		}
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
		return new ParseResult(assertions, argument("Parses_to", s, String.class, 0, args));
	}

	public Object validates_with(Expression s, List<Object> args) {
		assertVariableOrParameterTo("Validates_with", "Example");
		int top = args.size();
		List<ValidationResult> results = new ArrayList<>(top);
		for(int idx = 0; idx < top; ++idx)
			results.add(argument("ValidationResult", s, ValidationResult.class, idx, args));
		return new ValidatesWith(assertions, results);
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

	public Object unindent(Expression s, List<Object> args) {
		if(args.size() != 1)
			throw specError(SPEC_ILLEGAL_NUMBER_OF_ARGUMENTS, s, "Unindent", 1, args.size());
		return Helpers.unindent(argument("Unindent", s, String.class, 0, args));
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
				if(v instanceof Node)
					tests.add(((Node)v).createTest());
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
