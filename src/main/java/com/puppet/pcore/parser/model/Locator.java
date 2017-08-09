package com.puppet.pcore.parser.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Locator {
	public final String file;

	public final String source;

	private int[] lineIndex;

	public Locator(String file, String source) {
		this.file = file;
		this.source = source;
	}

	private synchronized int[] lineIndex() {
		if(lineIndex == null) {
			List<Integer> li = new ArrayList<>(32);

			for(int nlPos = 0; nlPos >= 0;) {
				int lastNlPos = nlPos;
				li.add(nlPos);
				nlPos = source.indexOf('\n', lastNlPos + 1);
			}
			int idx = li.size();
			int[] liArr = new int[idx];
			while(--idx >= 0)
				liArr[idx] = li.get(idx);
			lineIndex = liArr;
		}
		return lineIndex;
	}

	public int lineforOffset(int offset) {
		int idx = Arrays.binarySearch(lineIndex(), offset);
		return idx < 0 ? -idx : idx + 1;
	}

	public int posforOffset(int offset) {
		int line = lineforOffset(offset) - 1;
		return line == 0 ? offset + 1 : offset - lineIndex[line - 1];
	}
}
