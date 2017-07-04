package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public interface ExpressionFactory {
	Expression access(Expression expr, List<Expression> parameters, String expression, int offset, int length);

	Expression array(List<Expression> expressions, String expression, int offset, int length);

	Expression assignment(Expression lhs, Expression rhs, String expression, int offset, int length);

	Expression constant(Object value, String expression, int offset, int length);

	Expression heredoc(Object value, String syntax, String expression, int offset, int length);

	Expression hash(List<Expression> expressionPairs, String expression, int offset, int length);

	Expression identifier(String s, String string, int offset, int length);

	Expression named_access(Expression lhs, Expression rhs, String expression, int offset, int length);

	Expression negate(Expression expr, String expression, int offset, int length);

	Expression regexp(String value, String tokenValue, int offset, int length);

	Expression typeName(String name, String expression, int offset, int length);

	Expression typeDeclaration(String typeName, String expression, int offset, int length);

	Expression variable(String typeName, String expression, int offset, int length);
}
