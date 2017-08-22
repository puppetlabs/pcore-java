package com.puppet.pcore.pspec;

import com.puppet.pcore.Issue;
import com.puppet.pcore.Severity;

public class ValidationResult {
	public final Issue issue;

	public final Severity severity;

	public static ValidationResult warning(Issue issue) {
		return new ValidationResult(Severity.WARNING, issue);
	}

	public static ValidationResult error(Issue issue) {
		return new ValidationResult(Severity.ERROR, issue);
	}

	private ValidationResult(Severity severity, Issue issue) {
		this.severity = severity;
		this.issue = issue;
	}
}
