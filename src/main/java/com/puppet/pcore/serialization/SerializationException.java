package com.puppet.pcore.serialization;

import com.puppet.pcore.PcoreException;

public class SerializationException extends PcoreException {
	private static final long serialVersionUID = -1;

	public SerializationException(String message) {
		super(message);
	}

	public SerializationException(Exception e) {
		super(e);
	}
}
