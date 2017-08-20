package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class IfExpression extends Positioned {
	public final Expression test;
	public final Expression then;
	public final Expression elseExpr;

	public IfExpression(Expression test, Expression then, Expression elseExpr, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.test = test;
		this.then = then;
		this.elseExpr = elseExpr;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		IfExpression co = (IfExpression)o;
		return test.equals(co.test) && Objects.equals(then, co.then) && Objects.equals(elseExpr, co.elseExpr);
	}

	@Override
	public PN toPN() {
		return ifToPN("if");
	}

	PN ifToPN(String ifType) {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(test.toPN().withName("test"));
		if(!(then instanceof NotExpression))
			entries.add(then.toPN().withName("then"));
		if(!(elseExpr instanceof NotExpression))
			entries.add(elseExpr.toPN().withName("else"));
		return new MapPN(entries).asCall(ifType);
	}
}
