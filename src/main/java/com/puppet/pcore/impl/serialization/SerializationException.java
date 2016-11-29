package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.PCoreException;

public class SerializationException extends PCoreException {
	public SerializationException(String message) {
		super(message);
	}

	public SerializationException(Exception e) {
		super(e);
	}
}
