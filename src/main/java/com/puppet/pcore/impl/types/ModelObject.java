package com.puppet.pcore.impl.types;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;

abstract class ModelObject {
	interface Visitor {
		void visit(ModelObject visitable, RecursionGuard guard);
	}

	/**
	 * Keeps track of self recursion of conceptual 'this' and 'that' instances using two separate maps and
	 * <p>
	 * a state. The class is used when tracking self recursion in two objects ('this' and 'that') simultaneously.
	 * A typical example of when this is needed is when testing if 'that' Puppet Type is assignable to 'this'
	 * Puppet Type since both types may contain self references.
	 * <p>
	 * All comparisons are made using the identity of the instance.
	 */
	static class RecursionGuard {

		static final int NO_SELF_RECURSION = 0;
		static final int SELF_RECURSION_IN_BOTH = 3;
		static final int SELF_RECURSION_IN_THAT = 2;
		static final int SELF_RECURSION_IN_THIS = 1;
		private Map<Object,Boolean> thatMap;
		private Map<Object,Boolean> thisMap;
		private Map<Object,Boolean> recursiveThatMap;
		private Map<Object,Boolean> recursiveThisMap;
		private int state;

		RecursionGuard() {
			state = NO_SELF_RECURSION;
		}

		/**
		 * Add the given argument as 'that' and call block with the resulting state. Pop
		 * restore state after call.
		 *
		 * @param instance the object to add
		 * @return the result of calling the block
		 */
		<R> R withThat(Object instance, Function<Integer, R> block) {
			R result;
			if(getThatMap().put(instance, Boolean.TRUE) == null) {
				result = block.apply(state);
				thatMap.remove(instance);
			} else {
				getRecursiveThatMap().put(instance, Boolean.TRUE);
				if((state & SELF_RECURSION_IN_THAT) == 0) {
					state |= SELF_RECURSION_IN_THAT;
					result = block.apply(state);
					state &= ~SELF_RECURSION_IN_THAT;
				}
				else
					result = block.apply(state);
			}
			return result;
		}

		/**
		 * Add the given argument as 'this' and call block with the resulting state. Pop
		 * restore state after call.
		 *
		 * @param instance the object to add
		 * @return the result of calling the block
		 */
		<R> R withThis(Object instance, Function<Integer, R> block) {
			R result;
			if(getThisMap().put(instance, Boolean.TRUE) == null) {
				result = block.apply(state);
				thisMap.remove(instance);
			} else {
				getRecursiveThisMap().put(instance, Boolean.TRUE);
				if((state & SELF_RECURSION_IN_THIS) == 0) {
					state |= SELF_RECURSION_IN_THIS;
					result = block.apply(state);
					state &= ~SELF_RECURSION_IN_THIS;
				} else
					result = block.apply(state);
			}
			return result;
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'that' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThat(Object instance) {
			return recursiveThatMap != null && recursiveThatMap.containsKey(instance);
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'this' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThis(Object instance) {
			return recursiveThisMap != null && recursiveThisMap.containsKey(instance);
		}

		private Map<Object,Boolean> getRecursiveThatMap() {
			if(recursiveThatMap == null)
				recursiveThatMap = new IdentityHashMap<>();
			return recursiveThatMap;
		}

		private Map<Object,Boolean> getRecursiveThisMap() {
			if(recursiveThisMap == null)
				recursiveThisMap = new IdentityHashMap<>();
			return recursiveThisMap;
		}

		private Map<Object,Boolean> getThatMap() {
			if(thatMap == null)
				thatMap = new IdentityHashMap<>();
			return thatMap;
		}

		private Map<Object,Boolean> getThisMap() {
			if(thisMap == null)
				thisMap = new IdentityHashMap<>();
			return thisMap;
		}
	}

	/**
	 * Acceptor used when re-checking for self recursion
	 */
	static class NoopAcceptor implements Visitor {
		static final NoopAcceptor singleton = new NoopAcceptor();

		@Override
		public void visit(ModelObject type, RecursionGuard guard) {
		}
	}

	void accept(Visitor visitor, RecursionGuard guard) {
		visitor.visit(this, guard);
	}
}
