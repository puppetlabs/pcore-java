package com.puppet.pcore.pspec;

import com.puppet.pcore.ReportedIssue;
import com.puppet.pcore.parser.Expression;

import java.util.List;

public interface Input {
	List<Executable> createOkTests(Result<Expression> expected);

	List<Executable> createIssuesTests(Result<List<ReportedIssue>> expected);
}
