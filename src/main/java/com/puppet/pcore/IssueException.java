package com.puppet.pcore;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.asList;

public class IssueException extends PcoreException {
	public final Issue issue;

	public final List<Object> args;

	public final Location location;

	public IssueException(Issue issue, Object[] args, Location location) {
		super(issue.name());
		this.issue = issue;
		this.args = asList(args);
		this.location = location;
	}

	public ReportedIssue reportedIssue() {
		return new ReportedIssue(issue, Severity.ERROR, args, location);
	}

	@Override
	public String getMessage() {
		return reportedIssue().toString();
	}
}
