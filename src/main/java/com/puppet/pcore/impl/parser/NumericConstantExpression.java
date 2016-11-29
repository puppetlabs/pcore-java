package com.puppet.pcore.impl.parser;

public class NumericConstantExpression extends ConstantExpression {
	public NumericConstantExpression(Number number, String expression, int offset, int length) {
		super(number, expression, offset, length);
	}

	public NumericConstantExpression negate() {
		return (value instanceof Double)
				? new NumericConstantExpression(-(Double)value, expression, offset - 1, length + 1)
				: new NumericConstantExpression(-(Long)value, expression, offset - 1, length + 1);
	}
}
