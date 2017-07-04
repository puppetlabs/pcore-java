package com.puppet.pcore;

import java.util.Map;

public interface PuppetObjectWithHash extends PuppetObject {
	Map<String, Object> _pcoreInitHash();
}
