package com.puppet.pcore.regex;

import org.joni.NameEntry;
import org.joni.Option;
import org.joni.Regex;
import org.joni.Region;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;

public class Matcher {
	private final Regex regex;
	private final org.joni.Matcher matcher;
	private final byte[] source;
	private int begin;

	Matcher(Regex regex, org.joni.Matcher matcher, byte[] source) {
		this.regex = regex;
		this.matcher = matcher;
		this.source = source;
		begin = 0;
	}

	private static boolean nameEquals(byte[] name, byte[] buf, int start, int end) {
		if(end - start == name.length) {
			for(int i = 0; i < name.length; ++i)
				if(name[i] != buf[start + i])
					return false;
			return true;
		}
		return false;
	}

	public boolean matches() {
		return matcher.match(0, source.length, Option.DEFAULT) == source.length;
	}

	public boolean find() {
		if(begin >= 0 && matcher.search(begin, source.length, Option.DEFAULT) >= 0)
			begin = matcher.getEnd();
		else
			begin = -1;
		return begin >= 0;
	}

	public String group(int groupNumber) {
		Region reg = matcher.getEagerRegion();
		return group(reg.beg, reg.end, groupNumber);
	}

	public String[] group(String name) {
		byte[] nb = name.getBytes(StandardCharsets.UTF_8);
		for(Iterator<NameEntry> iter = regex.namedBackrefIterator(); iter.hasNext();) {
			NameEntry e = iter.next();
			if(nameEquals(nb, e.name, e.nameP, e.nameEnd))
				return groups(e.getBackRefs());
		}
		return null;
	}

	public String[] groups(int[] groupNumbers) {
		Region reg = matcher.getEagerRegion();
		int cnt = groupNumbers.length;
		String[] result = new String[cnt];
		while(--cnt >= 0)
			result[cnt] = group(reg.beg, reg.end, groupNumbers[cnt]);
		return result;
	}

	private String group(int[] beg, int[] end, int groupNumber) {
		if(groupNumber < 0)
			throw new IllegalArgumentException("Regexp group must be a positive integer");

		int start = beg[groupNumber];
		if(start < 0)
			return null;
		return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(source, start, end[groupNumber] - start)).toString();
	}
}
