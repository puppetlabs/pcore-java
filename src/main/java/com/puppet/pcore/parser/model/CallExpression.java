package com.puppet.pcore.parser.model;

import com.puppet.pcore.parser.Expression;

import java.util.List;
import java.util.Objects;

import static com.puppet.pcore.impl.Helpers.unmodifiableCopy;

public abstract class CallExpression extends Positioned {
	public final boolean rvalRequired;

	public final Expression functor;

	public final List<Expression> arguments;

	public final Expression lambda;

	CallExpression(Expression functor, List<Expression> arguments, Expression lambda, boolean rvalRequired,
			Locator locator, int offset, int length) {
		super(locator, offset, length);
		this.functor = functor;
		this.arguments = unmodifiableCopy(arguments);
		this.lambda = lambda;
		this.rvalRequired = rvalRequired;
	}

	public boolean equals(Object o) {
		if(!super.equals(o))
			return false;
		CallExpression co = (CallExpression)o;
		return functor.equals(co.functor) && rvalRequired == co.rvalRequired && arguments.equals(co.arguments) && Objects.equals(lambda, co.lambda);
	}
}
