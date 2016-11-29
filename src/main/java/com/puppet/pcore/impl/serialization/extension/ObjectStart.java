package com.puppet.pcore.impl.serialization.extension;

public class ObjectStart implements SequenceStart {
	public final int attributeCount;
	public final String typeName;

	public ObjectStart(String typeName, int attributeCount) {
		this.typeName = typeName;
		this.attributeCount = attributeCount;
	}

	public boolean equals(Object o) {
		if(o instanceof ObjectStart) {
			ObjectStart oo = (ObjectStart)o;
			return attributeCount == oo.attributeCount && typeName.equals(oo.typeName);
		}
		return false;
	}

	public int hashCode() {
		return typeName.hashCode() * 29 + attributeCount;
	}

	/**
	 * The same as the attributeCount since each attribute is one value
	 *
	 * @return the attributeCount
	 */
	@Override
	public int sequenceSize() {
		return attributeCount;
	}
}
