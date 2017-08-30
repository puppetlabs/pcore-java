package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.CallPN;
import com.puppet.pcore.parser.Expression;

public class ExportedQuery extends QueryExpression {
	public ExportedQuery(Expression expr, Locator locator, int offset, int length) {
		super(expr, locator, offset, length);
	}

	@Override
	public PN toPN() {
		return expr instanceof NopExpression ? new CallPN("exported-query") : new CallPN("exported-query", expr.toPN());
	}
}
