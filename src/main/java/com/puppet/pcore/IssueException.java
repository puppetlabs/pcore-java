package com.puppet.pcore;

public class IssueException extends PcoreException {
	private final Object[] args;

	private final Location location;

	public IssueException(String issueCode, Object[] args, Location location) {
		super(issueCode);
		this.args = args;
		this.location = location;
	}
}
