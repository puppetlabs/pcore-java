package com.puppet.pcore;

import java.util.List;

import static java.lang.String.format;

public class ReportedIssue {
	public final Issue issue;

	public final Severity severity;

	public final List<Object> args;

	public final Location location;

	public ReportedIssue(Issue issue, Severity severity, List<Object> args, Location location) {
		this.issue = issue;
		this.severity = severity;
		this.args = args;
		this.location = location;
	}

	public String toString() {
		String str = format("%s: %s", issue.name(), format(issue.messageFormat(), args.toArray()));
		return location == null ? str : location.appendLocation(str);
	}
}
