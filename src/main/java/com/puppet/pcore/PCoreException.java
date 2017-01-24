package com.puppet.pcore;

public class PCoreException extends RuntimeException {
	private static final long serialVersionUID = -1;

	public PCoreException(String message) {
		super(message);
	}

	public PCoreException(Throwable e) {
		super(e);
	}
}
