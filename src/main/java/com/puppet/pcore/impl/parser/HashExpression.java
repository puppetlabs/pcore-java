package com.puppet.pcore.impl.parser;

import com.puppet.pcore.parser.Expression;

import java.util.List;

public class HashExpression extends ExpressionList {
	public HashExpression(List<Expression> parameters, String expression, int offset, int length) {
		super(parameters, expression, offset, length);
	}


	/**
	 * Finds an expression with that represents the string given by {@code key} and returns it.
	 *
	 * @param key
	 * @return the found expression or {@code null} if no expression matches the given key.
	 */
	public Expression getValue(String key) {
		int top = elements.size();
		for(int idx = 0; idx < top; idx += 2) {
			Expression keyExpr = elements.get(idx);
			if(keyExpr instanceof StringExpression && key.equals(((StringExpression)keyExpr).getString()))
				return elements.get(idx + 1);
		}
		return null;
	}
}
