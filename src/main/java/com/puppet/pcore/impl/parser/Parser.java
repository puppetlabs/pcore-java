package com.puppet.pcore.impl.parser;

import com.puppet.pcore.impl.Helpers;
import com.puppet.pcore.parser.Expression;
import com.puppet.pcore.parser.model.*;

import java.util.*;
import java.util.function.Supplier;

import static com.puppet.pcore.parser.ParseIssue.*;
import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;
import static com.puppet.pcore.impl.parser.LexTokens.*;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class Parser extends Lexer implements com.puppet.pcore.parser.ExpressionParser {

	private static class CommaSeparatedList extends ArrayExpression {
		public CommaSeparatedList(List<Expression> elements, Locator locator, int offset, int length) {
			super(elements, locator, offset, length);
		}
	}

	private final Stack<String> nameStack = new Stack<>();
	private final List<Definition> definitions = new ArrayList<>();

	public Parser() {
		super(false);
	}

	public Parser(boolean handleBacktickStrings) {
		super(handleBacktickStrings);
	}

	@Override
	public Expression parse(String exprString) {
		return parse(null, exprString);
	}

	@Override
	public Expression parse(String file, String exprString) {
		return parse(null, exprString, file != null && file.endsWith(".epp"), false);
	}

	@Override
	public synchronized Expression parse(String file, String exprString, boolean eppMode, boolean singleExpression) {
		definitions.clear();
		nameStack.clear();
		init(file, exprString, eppMode);
		Expression expr = parseTopBlock(file, exprString, eppMode, singleExpression);
		return singleExpression ? expr : new Program(expr, unmodifiableCopy(definitions), locator, 0, pos());
	}

	private Expression parseTopBlock(String file, String exprString, boolean eppMode, boolean singleExpression) {
		if(eppMode) {
			consumeEPP();

			String text = null;
			if(currentToken == TOKEN_RENDER_STRING) {
				text = tokenString();
				nextToken();
			}

			if(currentToken == TOKEN_END) {
				// No EPP in the source
				RenderString te = new RenderString(text, locator, 0, pos());
				return new BlockExpression(singletonList(te), locator, 0, pos());
			}

			if(currentToken == TOKEN_PIPE) {
				if(text != null && !text.isEmpty())
					throw parseIssue(PARSE_ILLEGAL_EPP_PARAMETERS);
				List<Parameter> eppParams = lambdaParameterList();
				return new BlockExpression(
						singletonList(new LambdaExpression(
								eppParams,
								null,
								new EppExpression(
										!eppParams.isEmpty(),
										parse(TOKEN_END, false),
										locator, 0, pos()),
								locator, 0, pos())),
						locator, 0, pos());
			}

			List<Expression> expressions = new ArrayList<>();
			if(text != null)
				expressions.add(new RenderString(text, locator, 0, pos()));

			for(;;) {
				if(currentToken == TOKEN_END) {
					return new BlockExpression(transformCalls(expressions, 0), locator, 0, pos());
				}
			}
		}

		nextToken();
		return parse(TOKEN_END, singleExpression);
	}

	@Override
	Expression parse(int expectedEnd, boolean singleExpression) {
		int start = tokenStartPos;
		if(singleExpression) {
			if(currentToken == expectedEnd)
				return new LiteralUndef(locator, start, pos() - start);
			Expression expr = assignment();
			assertToken(expectedEnd);
			return expr;
		}

		List<Expression> expressions = new ArrayList<>();
		while(currentToken != expectedEnd) {
			expressions.add(syntacticStatement());
			if(currentToken == TOKEN_SEMICOLON)
				nextToken();
		}
		return new BlockExpression(transformCalls(expressions, start), locator, start, pos() - start);
	}

	private static final Set<String> statementCalls = new HashSet<>(asList(
	  "require",
	  "realize",
	  "include",
	  "contain",
	  "tag",

	  "debug",
	  "info",
	  "notice",
	  "warning",
	  "err",

	  "fail",
	  "import",
	  "break",
	  "next",
	  "return"
	));

	private List<Expression> transformCalls(List<Expression> exprs, int start) {
		int top = exprs.size();
		if(top == 0)
			return exprs;

		Expression memo = exprs.get(0);
		List<Expression> result = new ArrayList<>(top);
		for(int idx = 1; idx < top; ++idx) {
			Expression expr = exprs.get(idx);
			if(memo instanceof QualifiedName && statementCalls.contains(((QualifiedName)memo).name)) {
				List<Expression> args;
				if(expr instanceof CommaSeparatedList)
					args = new ArrayList<>(((CommaSeparatedList)expr).elements);
				else {
					args = new ArrayList<>();
					args.add(expr);
				}

				for(int ax = args.size() - 1; ax >= 0; --ax) {
					Expression arg = args.get(ax);
					if(arg instanceof CallNamedFunctionExpression)
						args.set(ax, ((CallNamedFunctionExpression)arg).withRvalRequired(true));
				}
				Expression cn = new CallNamedFunctionExpression(memo, args, null, false, locator, memo.offset(), (expr.offset()+expr.length())-memo.offset());
				result.add(cn);
				if(++idx == top) {
					return result;
				}
				memo = exprs.get(idx);
			} else {
				if(memo instanceof CallNamedFunctionExpression)
					memo = ((CallNamedFunctionExpression)memo).withRvalRequired(false);
				result.add(memo);
				memo = expr;
			}
		}
		if(memo instanceof CallNamedFunctionExpression)
			memo = ((CallNamedFunctionExpression)memo).withRvalRequired(false);
		result.add(memo);
		return result;
	}

	private void assertToken(int token) {
		if(currentToken != token) {
			setPos(tokenStartPos);
			throw parseIssue(PARSE_EXPECTED_TOKEN, tokenMap.get(token), tokenMap.get(currentToken));
		}
	}

	private <T extends Expression> List<T> expressions(int endToken, Supplier<T> supplier) {
		ArrayList<T> result = new ArrayList<>();
		for(;;) {
			if(currentToken == endToken) {
				nextToken();
				return result;
			}
			result.add(supplier.get());

			if(currentToken != TOKEN_COMMA) {
				if(currentToken != endToken) {
					setPos(tokenStartPos);
					throw parseIssue(PARSE_EXPECTED_ONE_OF_TOKENS, format("'%s' or '%s'", tokenMap.get(TOKEN_COMMA), tokenMap.get(endToken)), tokenMap.get(currentToken));
				}
				nextToken();
				return result;
			}
			nextToken();
		}
	}

	private Expression syntacticStatement() {
		List<Expression> args = null;
		Expression expr = assignment();
		while(currentToken == TOKEN_COMMA) {
			nextToken();
			if(args == null) {
				args = new ArrayList<>();
				args.add(expr);
			}
			args.add(assignment());
		}
		if(args != null)
			expr = new CommaSeparatedList(args, locator, expr.offset(), pos() - expr.offset());
		return expr;
	}

	private Expression collectionEntry() {
		Expression expr;
		switch(currentToken) {
		case TOKEN_TYPE: case TOKEN_FUNCTION: case TOKEN_APPLICATION: case TOKEN_CONSUMES: case TOKEN_PRODUCES: case TOKEN_SITE:
			expr = new QualifiedName(tokenString(), locator, tokenStartPos, pos() - tokenStartPos);
			nextToken();
			break;
		default:
			expr = assignment();
		}
		return expr;
	}

	private Expression assignment() {
		Expression expr = relationship();
		for(;;) {
			switch(currentToken) {
			case TOKEN_ASSIGN: case TOKEN_ADD_ASSIGN: case TOKEN_SUBTRACT_ASSIGN:
				String op = tokenString();
				nextToken();
				expr = new AssignmentExpression(op, expr, assignment(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression relationship() {
		Expression expr = resource();
		for(;;) {
			switch(currentToken) {
			case TOKEN_IN_EDGE: case TOKEN_IN_EDGE_SUB: case TOKEN_OUT_EDGE: case TOKEN_OUT_EDGE_SUB:
				String op = tokenString();
				nextToken();
				expr = new RelationshipExpression(op, expr, resource(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression resource() {
		Expression expr = expression();
		if(currentToken == TOKEN_LC)
			expr = resourceExpression(expr.offset(), expr, "regular");
		return expr;
	}

	private Expression expression() {
		Expression expr = orExpression();
		switch(currentToken) {
		case TOKEN_PRODUCES: case TOKEN_CONSUMES:
			// Must be preceded by name of class
			if(expr instanceof  QualifiedName || expr instanceof QualifiedReference || expr instanceof ReservedWord || expr instanceof AccessExpression)
				expr = capabilityMapping(expr, tokenString());
			break;

		default:
			if(expr instanceof NamedAccessExpression)
				// Transform into method call
				expr = new CallMethodExpression(expr, emptyList(), null, false, locator, expr.offset(), pos() - expr.offset());
		}
		return expr;
	}

	private Expression orExpression() {
		Expression expr = andExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_OR:
				nextToken();
				expr = new OrExpression(expr, orExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression andExpression() {
		Expression expr = compareExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_AND:
				nextToken();
				expr = new AndExpression(expr, andExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression compareExpression() {
		Expression expr = equalExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_LESS: case TOKEN_LESS_EQUAL: case TOKEN_GREATER: case TOKEN_GREATER_EQUAL:
				String op = tokenString();
				nextToken();
				expr = new ComparisonExpression(op, expr, compareExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression equalExpression() {
		Expression expr = shiftExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_EQUAL: case TOKEN_NOT_EQUAL:
				String op = tokenString();
				nextToken();
				expr = new ComparisonExpression(op, expr, equalExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression shiftExpression() {
		Expression expr = additiveExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_LSHIFT: case TOKEN_RSHIFT:
				String op = tokenString();
				nextToken();
				expr = new ArithmeticExpression(op, expr, shiftExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression additiveExpression() {
		Expression expr = multiplicativeExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_ADD: case TOKEN_SUBTRACT:
				String op = tokenString();
				nextToken();
				expr = new ArithmeticExpression(op, expr, additiveExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression multiplicativeExpression() {
		Expression expr = matchExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_MULTIPLY: case TOKEN_DIVIDE: case TOKEN_REMAINDER:
				String op = tokenString();
				nextToken();
				expr = new ArithmeticExpression(op, expr, multiplicativeExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression matchExpression() {
		Expression expr = inExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_MATCH: case TOKEN_NOT_MATCH:
				String op = tokenString();
				nextToken();
				expr = new MatchExpression(op, expr, matchExpression(), locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression inExpression() {
		Expression expr = unaryExpression();
		for(;;) {
			switch(currentToken) {
				case TOKEN_IN:
					nextToken();
					expr = new InExpression(expr, inExpression(), locator, expr.offset(), pos() - expr.offset());
					break;

				default:
					return expr;
			}
		}
	}

	Expression unaryExpression() {
		Expression expr;
		int unaryStart = tokenStartPos;
		switch(currentToken) {
		case TOKEN_SUBTRACT:
			if(isDigit(peek())) {
				nextToken();
				if(currentToken == TOKEN_INTEGER) {
					LiteralInteger nbr = (LiteralInteger)primaryExpression();
					return new LiteralInteger(-nbr.value, nbr.radix, locator, unaryStart, pos() - unaryStart);
				}
				LiteralFloat nbr = (LiteralFloat)primaryExpression();
				return new LiteralFloat(-nbr.value, locator, unaryStart, pos() - unaryStart);
			}
			nextToken();
			expr = primaryExpression();
			return new UnaryMinusExpression(expr, locator, unaryStart, pos() - unaryStart);

		case TOKEN_ADD: {
			// Allow '+' prefix for constant numbers
			if(isDigit(peek())) {
				nextToken();
				if(currentToken == TOKEN_INTEGER) {
					LiteralInteger nbr = (LiteralInteger)primaryExpression();
					return new LiteralInteger(nbr.value, nbr.radix, locator, unaryStart, pos() - unaryStart);
				}
				LiteralFloat nbr = (LiteralFloat)primaryExpression();
				return new LiteralFloat(nbr.value, locator, unaryStart, pos() - unaryStart);
			}
			throw parseIssue(LEX_UNEXPECTED_TOKEN, "+");
		}

		case TOKEN_NOT:
			nextToken();
			expr = unaryExpression();
			return new NotExpression(expr, locator, unaryStart, pos() - unaryStart);

		case TOKEN_MULTIPLY:
			nextToken();
			expr = unaryExpression();
			return new UnfoldExpression(expr, locator, unaryStart, pos() - unaryStart);

		case TOKEN_AT: case TOKEN_ATAT:
			String kind = currentToken == TOKEN_ATAT ? "exported" : "virtual";
			nextToken();
			expr = primaryExpression();
			assertToken(TOKEN_LC);
			return resourceExpression(unaryStart, expr, kind);

		default:
			expr = primaryExpression();
			switch(currentToken) {
			case TOKEN_LP: case TOKEN_PIPE:
				expr = callFunctionExpression(expr);
				break;
			case TOKEN_LCOLLECT: case TOKEN_LLCOLLECT:
				expr = collectExpression(expr);
				break;
			case TOKEN_QMARK:
				expr = selectExpression(expr);
				break;
			}
			return expr;
		}
	}

	private Expression primaryExpression() {
		Expression expr = atomExpression();
		for(;;) {
			switch(currentToken) {
			case TOKEN_LB:
				nextToken();
				List<Expression> params = arrayExpression();
				expr = new AccessExpression(expr, params, locator, expr.offset(), pos() - expr.offset());
				break;

			case TOKEN_DOT:
				nextToken();
				Expression rhs;
				if (currentToken == TOKEN_TYPE) {
					rhs = new QualifiedName(tokenString(), locator, tokenStartPos, pos() - tokenStartPos);
					nextToken();
				} else {
					rhs = primaryExpression();
				}
				expr = new NamedAccessExpression(expr, rhs, locator, expr.offset(), pos() - expr.offset());
				break;

			default:
				return expr;
			}
		}
	}

	private Expression atomExpression() {
		Expression expr;
		String name;
		int atomStart = tokenStartPos;
		switch(currentToken) {
		case TOKEN_LP: case TOKEN_WSLP:
				nextToken();
				expr = new ParenthesizedExpression(assignment(), locator, atomStart, pos() - atomStart);
				assertToken(TOKEN_RP);
				nextToken();
				break;

			case TOKEN_LB: case TOKEN_LISTSTART:
				nextToken();
				expr = new ArrayExpression(arrayExpression(), locator, atomStart, pos() - atomStart);
				break;

			case TOKEN_LC:
				nextToken();
				expr = new HashExpression(hashExpression(), locator, atomStart, pos() - atomStart);
				break;

			case TOKEN_BOOLEAN:
				expr = new LiteralBoolean((Boolean)tokenValue, locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_INTEGER:
				expr = new LiteralInteger((Long)tokenValue, radix, locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_FLOAT:
				expr = new LiteralFloat((Double)tokenValue, locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_STRING:
				expr = new LiteralString(tokenString(), locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_ATTR: case TOKEN_PRIVATE:
				expr = new ReservedWord(tokenString(), false, locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_DEFAULT:
				expr = new LiteralDefault(locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_HEREDOC: case TOKEN_CONCATENATED_STRING: case TOKEN_VARIABLE:
				expr = (Expression)tokenValue;
				nextToken();
				break;

			case TOKEN_REGEXP:
				expr = new LiteralRegexp(tokenString(), locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_UNDEF:
				expr = new LiteralUndef(locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_TYPE_NAME:
				expr = new QualifiedReference(tokenString(), locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_IDENTIFIER:
				expr = new QualifiedName(tokenString(), locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_CASE:
				expr = caseExpression();
				break;

			case TOKEN_IF:
				expr = ifExpression(false);
				break;

			case TOKEN_UNLESS:
				expr = ifExpression(true);
				break;

			case TOKEN_CLASS:
				name = tokenString();
				nextToken();
				if(currentToken == TOKEN_LC) {
					// Class resource
					expr = new QualifiedName(name, locator, atomStart, pos() - atomStart);
				} else {
					expr = classExpression(atomStart);
				}
				break;

			case TOKEN_TYPE:
				// look ahead for '(' in which case this is a named function call
				name = tokenString();
				nextToken();
				if(currentToken == TOKEN_TYPE_NAME)
					expr = typeAliasOrDefinition();
				else
					// Not a type definition. Just treat the 'type' keyword as a qualfied name
					expr = new QualifiedName(name, locator, atomStart, pos() - atomStart);
				break;

			case TOKEN_FUNCTION:
				expr = functionDefinition();
				break;

			case TOKEN_NODE:
				expr = nodeDefinition();
				break;

			case TOKEN_DEFINE: case TOKEN_APPLICATION:
				expr = resourceDefinition(currentToken);
				break;

			case TOKEN_SITE:
				expr = siteDefinition();
				break;

			case TOKEN_RENDER_STRING:
				expr = new RenderString(tokenString(), locator, atomStart, pos() - atomStart);
				nextToken();
				break;

			case TOKEN_RENDER_EXPR:
				nextToken();
				expr = new RenderExpression(expression(), locator, atomStart, pos() - atomStart);
				break;

			default:
				setPos(tokenStartPos);
				throw parseIssue(LEX_UNEXPECTED_TOKEN, tokenMap.get(currentToken));
		}
		return expr;
	}

	private List<Expression> arrayExpression() {
		return expressions(TOKEN_RB, this::collectionEntry);
	}

	private Expression capabilityMapping(Expression component, String kind) {
		int start = tokenStartPos;
		nextToken();
		String capName = className();
		assertToken(TOKEN_LC);
		nextToken();
		List<Expression> mappings = attributeOperations();
		assertToken(TOKEN_RC);
		nextToken();
		if(component instanceof QualifiedName || component instanceof QualifiedReference) {
			// No action
		} else if(component instanceof ReservedWord) {
			// All reserved words are lowercase only
			component = new QualifiedName(qualifiedName(((ReservedWord)component).name()), locator, component.offset(), component.length());
		}
		return new CapabilityMapping(kind, qualifiedName(capName), component, mappings, locator, start, pos() - start);
	}

	private Expression siteDefinition() {
		int start = tokenStartPos;
		nextToken();
		assertToken(TOKEN_LC);
		nextToken();
		Expression block = parse(TOKEN_RC, false);
		nextToken();
		return new SiteDefinition(block, locator, start, pos() - start);
	}

	private Expression resourceDefinition(int resourceToken) {
		int start = tokenStartPos;
		nextToken();
		String name = className();
		List<Parameter> params = parameterList();
		assertToken(TOKEN_LC);
		nextToken();
		Expression body = parse(TOKEN_RC, false);
		nextToken();
		if(resourceToken == TOKEN_APPLICATION)
			return new Application(name, params, body, locator, start, pos() - start);
		return new ResourceTypeDefinition(name, params, body, locator, start, pos() - start);
	}

	private Expression nodeDefinition() {
		int start = tokenStartPos;
		nextToken();
		List<Expression> hostnames = hostnames();
		Expression nodeParent = null;
		if(currentToken == TOKEN_INHERITS) {
			nextToken();
			nodeParent = hostname();
		}
		assertToken(TOKEN_LC);
		nextToken();
		Expression block = parse(TOKEN_RC, false);
		nextToken();
		return new NodeDefinition(hostnames, nodeParent, block, locator, start, pos() - start);
	}

	private List<Expression> hostnames() {
		List<Expression> hostnames = new ArrayList<>();
		for(;;) {
			hostnames.add(hostname());
			if(currentToken != TOKEN_COMMA)
				return hostnames;

			nextToken();
			switch(currentToken) {
			case TOKEN_INHERITS: case TOKEN_LC:
				return hostnames;
			}
		}
	}

	private Expression hostname() {
		int start = tokenStartPos;
		Expression hostname;
		switch(currentToken) {
		case TOKEN_IDENTIFIER: case TOKEN_TYPE_NAME: case TOKEN_INTEGER: case TOKEN_FLOAT:
			hostname = dottedName();
			break;
		case TOKEN_REGEXP:
			hostname = new LiteralRegexp(tokenString(), locator, start, pos() - start);
			nextToken();
			break;
		case TOKEN_STRING:
			hostname = new LiteralString(tokenString(), locator, start, pos() - start);
			nextToken();
			break;
		case TOKEN_DEFAULT:
			hostname = new LiteralDefault(locator, start, pos() - start);
			nextToken();
			break;
		case TOKEN_CONCATENATED_STRING: case TOKEN_HEREDOC:
			hostname = (Expression)tokenValue;
			nextToken();
			break;
		default:
			throw parseIssue(PARSE_EXPECTED_HOSTNAME);
		}
		return hostname;
	}

	private Expression dottedName() {
		int start = tokenStartPos;
		List<String> names = new ArrayList<>();
		for(;;) {
			switch(currentToken) {
			case TOKEN_IDENTIFIER: case TOKEN_TYPE_NAME:
				names.add(tokenString());
				break;
			case TOKEN_INTEGER:
				names.add(tokenValue.toString());
				break;
			case TOKEN_FLOAT:
				names.add(format("%g", (Double)tokenValue));
				break;
			default:
				throw parseIssue(PARSE_EXPECTED_NAME_OR_NUMBER_AFTER_DOT);
			}

			nextToken();
			if(currentToken != TOKEN_DOT)
				return new LiteralString(Helpers.join(".", names), locator, start, pos() - start);
			nextToken();
		}
	}

	private Expression functionDefinition() {
		int start = tokenStartPos;
		nextToken();
		String name;
		switch(currentToken) {
		case TOKEN_IDENTIFIER: case TOKEN_TYPE_NAME:
			name = tokenString();
			break;
		default:
			setPos(tokenStartPos);
			throw parseIssue(PARSE_EXPECTED_NAME_AFTER_FUNCTION);
		}
		nextToken();
		List<Parameter> parameterList = this.parameterList();

		Expression returnType = null;
		if(currentToken == TOKEN_RSHIFT) {
			nextToken();
			returnType = parameterType();
		}

		assertToken(TOKEN_LC);
		nextToken();
		Expression block = parse(TOKEN_RC, false);
		nextToken(); // consume TOKEN_RC
		return new FunctionDefinition(name, parameterList, block, returnType, locator, start, pos() - start);
	}

	private List<Parameter> parameterList() {
		switch(currentToken) {
		case TOKEN_LP: case TOKEN_WSLP:
			nextToken();
			return expressions(TOKEN_RP, this::parameter);
		default:
			return emptyList();
		}
	}

	private Parameter parameter() {
		int start = tokenStartPos;
		Expression typeExpr = null;
		if(currentToken == TOKEN_TYPE_NAME)
			typeExpr = parameterType();

		boolean capturesRest = currentToken == TOKEN_MULTIPLY;
		if(capturesRest)
			nextToken();

		if(currentToken != TOKEN_VARIABLE)
			throw parseIssue(PARSE_EXPECTED_VARIABLE);

		VariableExpression variable = (VariableExpression)tokenValue;
		nextToken();

		Expression defaultExpression = null;
		if(currentToken == TOKEN_ASSIGN) {
			nextToken();
			defaultExpression = expression();
		}

		return new Parameter(
				((NameExpression)variable.expr).name(),
				defaultExpression, typeExpr, capturesRest, locator, start, pos() - start);
	}

	private Expression classExpression(int start) {
		String name = className();
		if(name.startsWith("::"))
			name = name.substring(2);

		// Push to namestack
		nameStack.push(name);

		List<Parameter> params = parameterList();
		String parent = null;
		if(currentToken == TOKEN_INHERITS) {
			nextToken();
			if(currentToken == TOKEN_DEFAULT) {
				parent = tokenMap.get(TOKEN_DEFAULT);
				nextToken();
			} else {
				parent = className();
			}
		}
		assertToken(TOKEN_LC);
		nextToken();
		Expression body = parse(TOKEN_RC, false);
		nextToken();

		nameStack.pop();
		return new HostClassDefinition(qualifiedName(name), parent, params, body, locator, start, pos() - start);
	}

	private Expression ifExpression(boolean unless) {
		int start = tokenStartPos;
		nextToken();
		Expression condition = orExpression();
		assertToken(TOKEN_LC);
		nextToken();
		Expression thenPart = parse(TOKEN_RC, false);
		nextToken();

		Expression elsePart;
		switch(currentToken) {
		case TOKEN_ELSE:
			nextToken();
			assertToken(TOKEN_LC);
			nextToken();
			elsePart = parse(TOKEN_RC, false);
			nextToken();
			break;
		case TOKEN_ELSIF:
			if(unless)
			  throw parseIssue(PARSE_ELSIF_IN_UNLESS);
      elsePart = ifExpression(false);
      break;
		default:
			elsePart = new NopExpression(locator, tokenStartPos, 0);
		}

		if(unless)
			return new UnlessExpression(condition, thenPart, elsePart, locator, start, pos() - start);
		return new IfExpression(condition, thenPart, elsePart, locator, start, pos() - start);
	}

	private Expression caseExpression() {
		int start = tokenStartPos;
		nextToken();
		Expression test = expression();
		assertToken(TOKEN_LC);
		nextToken();
		List<CaseOption> caseOptions = caseOptions();
		return new CaseExpression(test, caseOptions, locator, start, pos() - start);
	}

	List<CaseOption> caseOptions() {
		List<CaseOption> exprs = new ArrayList<>();
		for(;;) {
			exprs.add(caseOption());
			if(currentToken == TOKEN_RC) {
				nextToken();
				return exprs;
			}
		}
	}

	CaseOption caseOption() {
		int start = tokenStartPos;
		List<Expression> expressions = expressions(TOKEN_COLON, this::expression);
		assertToken(TOKEN_LC);
		nextToken();
		Expression block = parse(TOKEN_RC, false);
		nextToken();
		return new CaseOption(expressions, block, locator, start, pos() - start);
	}

	private Expression selectExpression(Expression test) {
		List<SelectorEntry> selectors;
		nextToken();
		if(currentToken == TOKEN_SELC) {
			nextToken();
			selectors = expressions(TOKEN_RC, this::selectorEntry);
		} else
			selectors = singletonList(selectorEntry());
		return new SelectorExpression(test, selectors, locator, test.offset(), pos() - test.offset());
	}

	private SelectorEntry selectorEntry() {
		int start = tokenStartPos;
		Expression lhs = expression();
		assertToken(TOKEN_FARROW);
		nextToken();
		return new SelectorEntry(lhs, expression(), locator, start, pos() - start);
	}

	private Expression collectExpression(Expression lhs) {
		Expression collectQuery;
		int queryStart = tokenStartPos;
		if(currentToken == TOKEN_LCOLLECT) {
			nextToken();
			Expression queryExpr;
			if(currentToken == TOKEN_RCOLLECT) {
				queryExpr = new NopExpression(locator, tokenStartPos, 0);
			} else {
				queryExpr = expression();
				assertToken(TOKEN_RCOLLECT);
			}
			nextToken();
			collectQuery = new VirtualQuery(queryExpr, locator, queryStart, pos() - queryStart);
		} else {
			nextToken();
			Expression queryExpr;
			if(currentToken == TOKEN_RRCOLLECT) {
				queryExpr = new NopExpression(locator, queryStart, tokenStartPos - queryStart);
			} else {
				queryExpr = expression();
				assertToken(TOKEN_RRCOLLECT);
			}
			nextToken();
			collectQuery = new ExportedQuery(queryExpr, locator, queryStart, pos() - queryStart);
		}

		List<Expression> attributeOps;
		if(currentToken != TOKEN_LC) {
			attributeOps = emptyList();
		} else {
			nextToken();
			attributeOps = attributeOperations();
			assertToken(TOKEN_RC);
			nextToken();
		}
		return new CollectExpression(lhs, collectQuery, attributeOps, locator, lhs.offset(), pos() - lhs.offset());
	}

	private Expression callFunctionExpression(Expression functorExpr) {
		List<Expression> args;
		if(currentToken != TOKEN_PIPE) {
			nextToken();
			args = expressions(TOKEN_RP, this::assignment);
		} else
			args = emptyList();

		Expression block = null;
		if(currentToken == TOKEN_PIPE)
			block = lambda();

		int start = functorExpr.offset();
		if(functorExpr instanceof NamedAccessExpression)
			return new CallMethodExpression(functorExpr, args, block, true, locator, start, pos() - start);

		return new CallNamedFunctionExpression(functorExpr, args, block, true, locator, start, pos() - start);
	}

	private Expression lambda() {
		int start = tokenStartPos;
		List<Parameter> parameterList = lambdaParameterList();
		Expression returnType = null;
		if(currentToken == TOKEN_RSHIFT) {
			nextToken();
			returnType = parameterType();
		}

		assertToken(TOKEN_LC);
		nextToken();
		Expression body = parse(TOKEN_RC, false);
		nextToken(); // consume TOKEN_RC
		return new LambdaExpression(parameterList, returnType, body, locator, start, pos() - start);
	}

	private List<Parameter> lambdaParameterList() {
		nextToken();
		return expressions(TOKEN_PIPE, this::parameter);
	}

	private Expression resourceExpression(int start, Expression first, String form) {
		List<Expression> ops;
		Expression expr;
		int bodiesStart = pos();
		nextToken();
		int titleStart = pos();
		Expression firstTitle = expression();
		if(currentToken != TOKEN_COLON) {
			// Resource body without title
			setPos(titleStart);
			switch(resourceShape(first)) {
	    case "resource":
				// This is just LHS followed by a hash. It only makes sense when LHS is an identifier equal
				// to one of the known "statement calls". For all other cases, this is an error
				String name = "";
				if(first instanceof  QualifiedName) {
					name = ((QualifiedName)first).name;
					if(statementCalls.contains(name)) {
						setPos(bodiesStart);
						nextToken();
						List<Expression> args = singletonList(new HashExpression(hashExpression(), locator, bodiesStart, pos()-bodiesStart));
						return new CallNamedFunctionExpression(first, args, null, true, locator, start, pos() - start);
					}
				}
	      setPos(start);
				throw parseIssue(PARSE_RESOURCE_WITHOUT_TITLE, name);

	    case "defaults":
		    setPos(bodiesStart);
				nextToken();
				ops = attributeOperations();
		    expr = new ResourceDefaults(form, first, ops, locator, start, pos() - start);
		    break;

	    case "override":
		    setPos(bodiesStart);
				nextToken();
				ops = attributeOperations();
		    expr = new ResourceOverride(form, first, ops, locator, start, pos() - start);
		    break;

			default:
				setPos(first.offset());
				throw parseIssue(PARSE_INVALID_RESOURCE);
			}
		} else {
			List<ResourceBody> bodies = resourceBodies(firstTitle);
			expr = new Resource(form, first, bodies, locator, start, pos()-start);
		}

		assertToken(TOKEN_RC);
		nextToken();
		return expr;
	}

	private List<ResourceBody> resourceBodies(Expression title) {
		List<ResourceBody> result = new ArrayList<>();
		while(currentToken != TOKEN_RC) {
			result.add(resourceBody(title));
			if(currentToken == TOKEN_SEMICOLON)
				break;
			nextToken();
			if(currentToken != TOKEN_RC)
				title = expression();
		}
		return result;
	}

	private ResourceBody resourceBody(Expression title) {
		if(currentToken != TOKEN_COLON) {
			setPos(title.offset());
			throw parseIssue(PARSE_EXPECTED_TITLE);
		}
		nextToken();
		List<Expression> ops = attributeOperations();
		return new ResourceBody(title, ops, locator, title.offset(), pos() - title.offset());
	}

	private String resourceShape(Expression expr) {
		if(expr instanceof QualifiedName)
			return "resource";
		if(expr instanceof QualifiedReference)
			return "defaults";
		if(expr instanceof AccessExpression) {
			AccessExpression ae = (AccessExpression)expr;
			if(ae.operand instanceof QualifiedReference && ae.keys.size() == 1 && ((QualifiedReference)ae.operand).name.equals("Resource"))
				return "defaults";
			return "override";
		}
		return "error";
	}

	private Expression typeAliasOrDefinition() {
		int start = tokenStartPos;
		Expression typeExpr = parameterType();
		String parent = null;

		switch(currentToken) {
		case TOKEN_ASSIGN:
			if(typeExpr instanceof QualifiedReference) {
				nextToken();
				Expression body = expression();
				return new TypeAlias(((QualifiedReference)typeExpr).name, body, locator, start, pos() - start);
			} else if(typeExpr instanceof AccessExpression) {
				nextToken();
				Expression mapping = expression();
				return new TypeMapping(typeExpr, mapping, locator, start, pos() - start);
			}
			throw parseIssue(PARSE_EXPECTED_TYPE_NAME_AFTER_TYPE);

		case TOKEN_INHERITS:
			nextToken();
			QualifiedReference nameExpr = typeName();
			if(nameExpr == null)
				throw parseIssue(PARSE_INHERITS_MUST_BE_TYPE_NAME);

			parent = nameExpr.name;
			assertToken(TOKEN_LC);

			// fallthrough
		case TOKEN_LC:
			if(typeExpr instanceof QualifiedReference) {
				nextToken();
				Expression body = parse(TOKEN_RC, false);
				nextToken(); // consume TOKEN_RC
				return new TypeDefinition(((QualifiedReference)typeExpr).name, parent, body, locator, start, pos() - start);
			}
			throw parseIssue(PARSE_EXPECTED_TYPE_NAME_AFTER_TYPE);

		default:
			throw parseIssue(LEX_UNEXPECTED_TOKEN, tokenMap.get(currentToken));
		}
	}

	private List<Expression> attributeOperations() {
		List<Expression> result = new ArrayList<>();
		for(;;) {
			switch(currentToken) {
			case TOKEN_SEMICOLON: case TOKEN_RC:
				return result;
			default:
				result.add(attributeOperation());
				if(currentToken != TOKEN_COMMA)
					return result;
			}
			nextToken();
		}
	}

	private Expression attributeOperation() {
		int start = tokenStartPos;
		if(currentToken == TOKEN_MULTIPLY) {
			nextToken();
			assertToken(TOKEN_FARROW);
			nextToken();
			return new AttributesOperation(expression(), locator, start, pos() - start);
		}

		String name = attributeName();

		switch(currentToken) {
		case TOKEN_FARROW: case TOKEN_PARROW:
			String op = tokenString();
			nextToken();
			return new AttributeOperation(op, name, expression(), locator, start,  pos() - start);
		default:
			throw parseIssue(PARSE_INVALID_ATTRIBUTE);
		}
	}

	private String attributeName() {
		switch(currentToken) {
		case TOKEN_IDENTIFIER:
			String name = tokenString();
			nextToken();
			return name;
		default:
			String word = keyword();
			if(word != null) {
				nextToken();
				return word;
			}
			setPos(tokenStartPos);
			throw parseIssue(PARSE_EXPECTED_ATTRIBUTE_NAME);
		}
	}

	private Expression parameterType() {
		int start = tokenStartPos;
		Expression typeName = typeName();
		if(typeName == null)
			throw parseIssue(PARSE_EXPECTED_TYPE_NAME);

		if(currentToken == TOKEN_LB) {
			nextToken();
			List<Expression> typeArgs = arrayExpression();
			return new AccessExpression(typeName, typeArgs, locator, start, pos() - start);
		}
		return typeName;
	}

	private String className() {
		switch(currentToken) {
		case TOKEN_TYPE_NAME: case TOKEN_IDENTIFIER:
				String name = tokenString();
				nextToken();
				return name;
		case TOKEN_STRING: case TOKEN_CONCATENATED_STRING:
			setPos(tokenStartPos);
			throw parseIssue(PARSE_QUOTED_NOT_VALID_NAME);
		case TOKEN_CLASS:
			setPos(tokenStartPos);
			throw parseIssue(PARSE_CLASS_NOT_VALID_HERE);
		default:
			setPos(tokenStartPos);
			throw parseIssue(PARSE_EXPECTED_CLASS_NAME);
		}
	}

	private String keyword() {
		if(currentToken != TOKEN_BOOLEAN) {
			String str = tokenMap.get(currentToken);
			if(keywords.containsKey(str))
				return str;
		}
		return null;
	}

	private QualifiedReference typeName() {
		if(currentToken == TOKEN_TYPE_NAME) {
			QualifiedReference name = new QualifiedReference(tokenString(), locator, tokenStartPos, pos() - tokenStartPos);
			nextToken();
			return name;
		}
		return null;
	}

	private String qualifiedName(String name) {
		if(nameStack.isEmpty())
			return name;
		StringBuilder bld = new StringBuilder();
		for(String n : nameStack) {
			bld.append(n);
			bld.append("::");
		}
		bld.append(name);
		return bld.toString();
	}

	private KeyedEntry hashEntry() {
		Expression key = collectionEntry();
		if(currentToken != TOKEN_FARROW)
			throw parseIssue(PARSE_EXPECTED_FARROW_AFTER_KEY);

		nextToken();
		Expression value = collectionEntry();
		return new KeyedEntry(key, value, locator, key.offset(), pos() - key.offset());
	}

	private List<KeyedEntry> hashExpression() {
		return expressions(TOKEN_RC, this::hashEntry);
	}
}
