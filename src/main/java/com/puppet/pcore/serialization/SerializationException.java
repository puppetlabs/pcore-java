package com.puppet.pcore.serialization;

import com.puppet.pcore.PCoreException;

public class SerializationException extends PCoreException {
	private static final long serialVersionUID = -1;

	public SerializationException(String message) {
		super(message);
	}

	public SerializationException(Exception e) {
		super(e);
	}
}
