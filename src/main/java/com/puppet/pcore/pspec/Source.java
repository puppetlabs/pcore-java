package com.puppet.pcore.pspec;

import com.puppet.pcore.IssueException;
import com.puppet.pcore.ReportedIssue;
import com.puppet.pcore.impl.parser.Parser;
import com.puppet.pcore.parser.Expression;

import java.util.List;

import static com.puppet.pcore.impl.Helpers.map;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class Source implements Input {
	public final Parser parser = new Parser();

	public final List<String> sources;

	public Source(List<String> sources) {
		this.sources = sources;
	}

	@Override
	public List<Executable> createOkTests(Result<Expression> expected) {
		return map(sources, source -> createOkTest(source, expected));
	}

	@Override
	public List<Executable> createIssuesTests(Result<List<ReportedIssue>> expected) {
		return map(sources, source -> createIssueTest(source, expected));
	}

	protected Executable createOkTest(String source, Result<Expression> expected) {
		Expression parsed = parser.parse(null, source, false, true);
		return expected.createTest(parsed);
	}

	protected Executable createIssueTest(String source, Result<List<ReportedIssue>> expected) {
		try {
			parser.parse(null, source);
			return expected.createTest(emptyList());
		} catch(IssueException e) {
			return expected.createTest(singletonList(e.reportedIssue()));
		}
	}
}
