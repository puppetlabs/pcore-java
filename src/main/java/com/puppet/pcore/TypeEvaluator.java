package com.puppet.pcore;

import com.puppet.pcore.loader.Loader;
import com.puppet.pcore.parser.Expression;

import java.net.URI;

/**
 * Provides the ability to parse and evaluate type expressions in String form.
 */
public interface TypeEvaluator {
	/**
	 * Parses and evaluates a given {@code typeExpression} and makes it available under a given {@code name}.
	 * <p>
	 * If the expression_ evaluates to an {@code ObjectType} or a {@code TypeSet}, then that
	 * the object is bound and returned. All other types will be bound using
	 * a {@code TypeAlias} and the alias will be returned.
	 *
	 * @param name           the name to use for the type
	 * @param typeExpression the expression that evaluates to the type
	 * @return the bound type or type alias.
	 */
	Type declareType(String name, String typeExpression);

	/**
	 * Parses and evaluates a given {@code typeExpression} and makes it available under a given {@code name}.
	 * <p>
	 * If the expression_ evaluates to an {@code ObjectType} or a {@code TypeSet}, then that
	 * the object is bound and returned. All other types will be bound using
	 * a {@code TypeAlias} and the alias will be returned.
	 *
	 * @param name           the name to use for the type
	 * @param typeExpression the expression that evaluates to the type
	 * @param nameAuthority  the name authority for the given name
	 * @return the bound type or type alias.
	 */
	Type declareType(String name, String typeExpression, URI nameAuthority);

	/**
	 * Evaluates a given {@code expression} and makes it available under a given {@code name}.
	 * <p>
	 * If the expression_ evaluates to an {@code ObjectType} or a {@code TypeSet}, then that
	 * the object is bound and returned. All other types will be bound using
	 * a {@code TypeAlias} and the alias will be returned.
	 *
	 * @param name           the name to use for the type
	 * @param typeExpression the expression that evaluates to the type
	 * @param nameAuthority  the name authority to use for the declaration
	 * @return the bound type or type alias.
	 */
	Type declareType(String name, Expression typeExpression, URI nameAuthority);

	/**
	 * Returns the loader associated with this evaluator
	 *
	 * @return the loader
	 */
	Loader getLoader();

	/**
	 * Resolves a literal expression. The expression may contain numbers, strings, boolean,
	 * undef, array, hash, or type expressions.
	 *
	 * @param string the string to evaluate
	 * @return the resulting object (hash, array, string, type, etc.)
	 */
	Object resolve(String string);

	/**
	 * Resolves a parsed expression.
	 *
	 * @param expression the expression to evaluate
	 * @return the resulting object (hash, array, string, type, etc.)
	 */
	Object resolve(Expression expression);

	/**
	 * Resolves a type expression and returns the type. It's an error if the expression does not
	 * resolve to a type.
	 *
	 * @param typeString the type expression to evaluate
	 * @return the resulting type
	 */
	Type resolveType(String typeString);

	/**
	 * Resolves a type expression and returns the type. It's an error if the expression does not
	 * resolve to a type.
	 *
	 * @param typeExpression the type expression to evaluate
	 * @return the resulting type
	 */
	Type resolveType(Expression typeExpression);
}
