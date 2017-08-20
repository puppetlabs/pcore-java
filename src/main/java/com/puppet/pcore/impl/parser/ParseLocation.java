package com.puppet.pcore.impl.parser;

import com.puppet.pcore.Location;
import com.puppet.pcore.parser.model.Locator;

public class ParseLocation implements Location {
	final Locator locator;

	final int offset;

	public ParseLocation(Locator locator, int offset) {
		this.locator = locator;
		this.offset = offset;
	}

	@Override
	public String sourceName() {
		return locator.file;
	}

	@Override
	public int line() {
		return locator.lineforOffset(offset);
	}

	@Override
	public int pos() {
		return locator.posforOffset(offset);
	}

	@Override
	public String appendLocation(String message) {
		StringBuilder bld = new StringBuilder(message);
		int line = line();
		if(locator.file != null) {
			if(line > 0) {
				bld.append(" at ");
				bld.append(locator.file);
				bld.append(':');
				bld.append(line);
				int pos = pos();
				if(pos > 0) {
					bld.append(':');
					bld.append(pos);
				}
			} else {
				bld.append(" in ");
				bld.append(locator.file);
			}
		} else if(line > 0) {
			bld.append(" at line ");
			bld.append(line);
			int pos = pos();
			if(pos > 0) {
				bld.append(':');
				bld.append(pos);
			}
		}
		return bld.toString();
	}
}
