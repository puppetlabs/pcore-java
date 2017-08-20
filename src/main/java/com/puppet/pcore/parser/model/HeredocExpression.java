package com.puppet.pcore.parser.model;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.pn.LiteralPN;
import com.puppet.pcore.impl.pn.MapPN;
import com.puppet.pcore.parser.Expression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class HeredocExpression extends Positioned {
	public final Expression text;

	public final String syntax;

	public HeredocExpression(Expression text, String syntax, Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.text = text;
		this.syntax = syntax;
	}

	public boolean equals(Object o) {
		return super.equals(o) && text.equals(((HeredocExpression)o).text) && Objects.equals(syntax, ((HeredocExpression)o).syntax);
	}

	@Override
	public PN toPN() {
		List<Map.Entry<String,? extends PN>> entries = new ArrayList<>();
		entries.add(text.toPN().withName("text"));
		if(syntax != null)
			entries.add(new LiteralPN(syntax).withName("syntax"));
		return new MapPN(entries).asCall("heredoc");
	}
}
