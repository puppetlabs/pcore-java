package com.puppet.pcore.impl.pn;

import com.puppet.pcore.PN;
import com.puppet.pcore.impl.Helpers.MapEntry;

import java.util.List;
import java.util.Map.Entry;


import static com.puppet.pcore.impl.Helpers.asList;
import static com.puppet.pcore.impl.Helpers.map;
import static com.puppet.pcore.impl.Helpers.asMap;

public class MapPN extends AbstractPN {
	public final List<Entry<String,? extends PN>> entries;

	@SafeVarargs
	public MapPN(Entry<String,? extends PN> ...entries) {
		this.entries = asList(entries);
	}

	public MapPN(List<Entry<String,? extends PN>> entries) {
		this.entries = entries;
	}

	@Override
	public void format(StringBuilder bld) {
		bld.append('{');
		int top = entries.size();
		if(top > 0) {
			formatEntry(entries.get(0), bld);
			for(int idx = 1; idx < top; ++idx) {
				bld.append(' ');
				formatEntry(entries.get(idx), bld);
			}
		}
		bld.append('}');
	}

	@Override
	public Object toData() {
		return asMap(map(entries, (entry) -> new MapEntry<>(entry.getKey(), entry.getValue().toData())));
	}

	private static void formatEntry(Entry<String,? extends PN> entry, StringBuilder bld) {
		bld.append(':');
		bld.append(entry.getKey());
		bld.append(' ');
		entry.getValue().format(bld);
	}
}
