package com.puppet.pcore.pspec;

import com.puppet.pcore.ReportedIssue;

import java.util.List;

public class ValidatesWith implements Result<List<ReportedIssue>> {
	public final Assertions assertions;

	public final List<ValidationResult> expectedIssues;

	public ValidatesWith(Assertions assertions, List<ValidationResult> expectedIssues) {
		this.assertions = assertions;
		this.expectedIssues = expectedIssues;
	}

	@Override
	public Executable createTest(List<ReportedIssue> actual) {
		return () -> {
			StringBuilder bld = new StringBuilder();
			nextExpected: for(ValidationResult result : expectedIssues) {
				for(ReportedIssue issue : actual) {
					if(result.issue == issue.issue && result.severity == issue.severity)
						continue nextExpected;
				}
				bld.append("Expected ").append(result.severity).append(" issue ").append(result.issue).append(" but it was not produced");
			}

			nextIssue: for(ReportedIssue issue : actual) {
				for(ValidationResult result : expectedIssues) {
					if(result.issue == issue.issue && result.severity == issue.severity)
						continue nextIssue;
				}
				bld.append("Unexpected ").append(issue.severity).append(" issue ").append(issue.toString());
			}
			if(bld.length() > 0)
				assertions.fail(bld.toString());
		};
	}
}
