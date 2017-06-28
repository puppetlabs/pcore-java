package com.puppet.pcore;

/**
 * An dynamic object represents an instance of an Object type. It holds an array
 * of attributes accessible by name.
 */
public interface DynamicObject extends PuppetObject {
	/**
	 * Return value of attribute with given name
	 *
	 * @param attrName the name of the attribute
	 * @return the attribute value
	 */
	Object get(String attrName);
}
