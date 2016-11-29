package com.puppet.pcore.impl.serialization.extension;

public class Numbers {
	// 0x00 - 0x0F are reserved for low-level serialization / tabulation extensions

	// Tabulation internal to the low level protocol reader/writer
	public static final byte INNER_TABULATION = 0x00;

	// Tabulation managed by the serializer / deserializer
	public static final byte TABULATION = 0x01;

	// 0x10 - 0x1F are reserved for structural extensions
	public static final byte ARRAY_START = 0x10;
	public static final byte MAP_START = 0x11;
	public static final byte OBJECT_START = 0x12;
	public static final byte SENSITIVE_START = 0x13;

	// 0x20 - 0x2f reserved for special extension objects
	public static final byte DEFAULT = 0x20;
	public static final byte COMMENT = 0x21;

	// 0x30 - 0x7f reserved for mapping of specific runtime classes
	public static final byte REGEXP = 0x30;
	public static final byte TYPE_REFERENCE = 0x31;
	public static final byte SYMBOL = 0x32;
	public static final byte TIME = 0x33;
	public static final byte TIMESPAN = 0x34;
	public static final byte VERSION = 0x35;
	public static final byte VERSION_RANGE = 0x36;
	public static final byte BINARY = 0x37;
	public static final byte BASE64 = 0x38;
}
