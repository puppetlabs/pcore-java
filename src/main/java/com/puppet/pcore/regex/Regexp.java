package com.puppet.pcore.regex;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF8Encoding;
import org.joni.Option;
import org.joni.Regex;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.joni.Option.*;

public class Regexp {
	public static final int MULTILINE = Option.MULTILINE;
	public static final int EXTENDED = Option.EXTEND;
	public static final int IGNORECASE = Option.IGNORECASE;

	public final String pattern;
	private final Regex regex;

	public static String quote(String str) {
		return quote(new ByteList(str), UTF8Encoding.INSTANCE).toString();
	}

	public static Regexp compile(String str) {
		return new Regexp(str);
	}

	public static Regexp compile(String str, int options) {
		return new Regexp(str, options);
	}

	private Regexp(String pattern) {
		this(pattern, new Regex(pattern));
	}

	private Regexp(String pattern, int options) {
		this.pattern = patternWithFlagsExpanded(pattern, options);
		this.regex = new Regex(this.pattern);
	}

	private Regexp(String pattern, Regex regex) {
		this.regex = regex;
		this.pattern = pattern;
	}

	public boolean equals(Object o) {
		return o instanceof Regexp && pattern.equals(((Regexp)o).pattern);
	}

	public int hashCode() {
		return pattern.hashCode() * 3;
	}

	public Matcher matcher(String str) {
		byte[] source = str.getBytes(StandardCharsets.UTF_8);
		return new Matcher(regex, regex.matcher(source), source);
	}

	public String toString() {
		return pattern;
	}

	private static String patternWithFlagsExpanded(String patternString, int flags) {
		if((flags & (IGNORECASE|EXTEND|MULTILINE)) != 0) {
			StringBuilder bld = new StringBuilder();
			bld.append("(?");
			if((flags & IGNORECASE) != 0)
				bld.append('i');
			if((flags & EXTEND) != 0)
				bld.append('e');
			if((flags & MULTILINE) != 0)
				bld.append('m');
			bld.append(':');
			bld.append(patternString);
			bld.append(')');
			patternString = bld.toString();
		}
		return patternString;
	}

	static class ByteList extends ByteArrayOutputStream {
		ByteList(String str) {
			buf = str.getBytes(StandardCharsets.UTF_8);
			count = buf.length;
		}

		ByteList(int capacity) {
			super(capacity);
		}

		byte[] getUnsafeBytes() {
			return buf;
		}

		int getBegin() {
			return 0;
		}

		int getRealSize() {
			return count;
		}

		void setRealSize(int sz) {
			count = sz;
		}
	}

	/**
	 * This method is borrowed from org.jruby.RubyRegexp
	 */
	private static ByteList quote(ByteList bs, Encoding enc) {
		int p = bs.getBegin();
		int end = p + bs.getRealSize();
		byte[]bytes = bs.getUnsafeBytes();

		metaFound: do {
			for(; p < end; p++) {
				int c = bytes[p] & 0xff;
				int cl = enc.length(bytes, p, end);
				if (cl != 1) {
					while (cl-- > 0 && p < end) p++;
					p--;
					continue;
				}
				switch (c) {
				case '[': case ']': case '{': case '}':
				case '(': case ')': case '|': case '-':
				case '*': case '.': case '\\':
				case '?': case '+': case '^': case '$':
				case ' ': case '#':
				case '\t': case '\f': case '\n': case '\r':
					break metaFound;
				}
			}
			return bs;
		} while (false);

		ByteList result = new ByteList(end * 2);
		byte[] obytes = result.getUnsafeBytes();
		int op = p - bs.getBegin();
		System.arraycopy(bytes, bs.getBegin(), obytes, 0, op);

		for(; p < end; p++) {
			int c = bytes[p] & 0xff;
			int cl = enc.length(bytes, p, end);
			if (cl != 1) {
				while (cl-- > 0 && p < end) obytes[op++] = bytes[p++];
				p--;
				continue;
			}

			switch (c) {
			case '[': case ']': case '{': case '}':
			case '(': case ')': case '|': case '-':
			case '*': case '.': case '\\':
			case '?': case '+': case '^': case '$':
			case '#': obytes[op++] = '\\'; break;
			case ' ': obytes[op++] = '\\'; obytes[op++] = ' '; continue;
			case '\t':obytes[op++] = '\\'; obytes[op++] = 't'; continue;
			case '\n':obytes[op++] = '\\'; obytes[op++] = 'n'; continue;
			case '\r':obytes[op++] = '\\'; obytes[op++] = 'r'; continue;
			case '\f':obytes[op++] = '\\'; obytes[op++] = 'f'; continue;
			}
			obytes[op++] = (byte)c;
		}

		result.setRealSize(op);
		return result;
	}
}
