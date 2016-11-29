package com.puppet.pcore;

public class PCoreException extends RuntimeException {
	public PCoreException(String message) {
		super(message);
	}

	public PCoreException(Throwable e) {
		super(e);
	}
}
