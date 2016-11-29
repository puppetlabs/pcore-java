package com.puppet.pcore.impl.types;

import java.util.IdentityHashMap;
import java.util.Map;

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
		private final Map<Object,Boolean> thatMap = new IdentityHashMap<>();
		private final Map<Object,Boolean> thisMap = new IdentityHashMap<>();
		private int state;

		RecursionGuard() {
			state = NO_SELF_RECURSION;
		}

		/**
		 * Add the given argument as 'that' and return the resulting state
		 *
		 * @param instance the object to add
		 * @return the resulting state
		 */
		int addThat(Object instance) {
			if((state & SELF_RECURSION_IN_THAT) == 0)
				if(thatMap.put(instance, Boolean.TRUE) != null)
					state |= SELF_RECURSION_IN_THAT;
			return state;
		}

		/**
		 * Add the given argument as 'this' and return the resulting state
		 *
		 * @param instance the object to add
		 * @return the resulting state
		 */
		int addThis(Object instance) {
			if((state & SELF_RECURSION_IN_THIS) == 0)
				if(thisMap.put(instance, Boolean.TRUE) != null)
					state |= SELF_RECURSION_IN_THIS;
			return state;
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'that' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThat(Object instance) {
			return thatMap.containsKey(instance);
		}

		/**
		 * Checks if recursion was detected for the given argument in the 'this' context
		 *
		 * @param instance the object to check
		 * @return true if recursion was detected, false otherwise.
		 */
		boolean recursiveThis(Object instance) {
			return thisMap.containsKey(instance);
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
