package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class DefaultExpressionFactory implements ExpressionFactory {
	public static final ExpressionFactory SINGLETON = new DefaultExpressionFactory();

	private DefaultExpressionFactory() {
	}

	@Override
	public Expression access(Expression expr, List<Expression> parameters, String expression, int offset, int length) {
		return new AccessExpression(expr, parameters, expression, offset, length);
	}

	@Override
	public Expression array(List<Expression> expressions, String expression, int offset, int length) {
		return new ArrayExpression(expressions, expression, offset, length);
	}

	@Override
	public Expression assignment(Expression lhs, Expression rhs, String expression, int offset, int length) {
		return new AssignmentExpression(lhs, rhs, expression, offset, length);
	}

	@Override
	public Expression constant(Object value, String expression, int offset, int length) {
		if(value instanceof Double)
			return literalFloat((Double)value, expression, offset, length);
		if(value instanceof Long)
			return literalInteger((Long)value, expression, offset, length);
		if(value instanceof Integer)
			return literalInteger(((Integer)value).longValue(), expression, offset, length);
		return new ConstantExpression(value, expression, offset, length);
	}

	@Override
	public Expression hash(List<Expression> expressionPairs, String expression, int offset, int length) {
		return new HashExpression(expressionPairs, expression, offset, length);
	}

	@Override
	public Expression identifier(String name, String expression, int offset, int length) {
		return new IdentifierExpression(name, expression, offset, length);
	}

	@Override
	public Expression negate(Expression expr, String expression, int offset, int length) {
		return expr instanceof NumericConstantExpression
				? ((NumericConstantExpression)expr).negate()
				: new NegateExpression(expr, expression, offset, length);
	}

	@Override
	public Expression regexp(String tokenValue, String expression, int offset, int length) {
		return new RegexpExpression(tokenValue, expression, offset, length);
	}

	@Override
	public Expression typeName(String name, String expression, int offset, int length) {
		return new TypeNameExpression(name, expression, offset, length);
	}

	private Expression literalFloat(Double number, String expression, int offset, int length) {
		return new NumericConstantExpression(number, expression, offset, length);
	}

	private Expression literalInteger(Long number, String expression, int offset, int length) {
		return new NumericConstantExpression(number, expression, offset, length);
	}
}
