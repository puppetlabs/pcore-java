package com.puppet.pcore;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PatternSubstitution {
	public final Pattern pattern;
	public final String replacement;

	public PatternSubstitution(Pattern pattern, String replacement) {
		this.pattern = pattern;
		this.replacement = replacement;
	}

	/**
	 * Perform substitution if the string matches the pattern and return the substituted
	 * String. Method will return <tt>null</tt> if the string doesn't match.
	 *
	 * @param str The string to perform substitutions on.
	 * @return The substituted string or <code>null</code> if it didn't match.
	 */
	public String replaceIn(String str) {
		Matcher m = pattern.matcher(str);
		if(!m.find())
			return null;

		// Nuisance to use a StringBuffer but it's required by the appendReplacement method
		StringBuffer sb = new StringBuffer();
		do {
			m.appendReplacement(sb, replacement);
		} while(m.find());
		m.appendTail(sb);
		return sb.toString();
	}
}
