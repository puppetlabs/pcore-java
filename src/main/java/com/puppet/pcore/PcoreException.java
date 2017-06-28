package com.puppet.pcore;

public class PcoreException extends RuntimeException {
	private static final long serialVersionUID = -1;

	public PcoreException(String message) {
		super(message);
	}

	public PcoreException(Throwable e) {
		super(e);
	}
}
