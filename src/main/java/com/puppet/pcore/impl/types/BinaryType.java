package com.puppet.pcore.impl.types;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Type;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.serialization.FactoryDispatcher;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.puppet.pcore.impl.ConstructorImpl.constructor;
import static com.puppet.pcore.impl.FactoryDispatcherImpl.dispatcher;
import static com.puppet.pcore.impl.types.TypeFactory.*;

public class BinaryType extends AnyType {
	public static final BinaryType DEFAULT = new BinaryType();

	private static ObjectType ptype;

	private BinaryType() {
	}

	@Override
	public Type _pcoreType() {
		return ptype;
	}

	@Override
	public FactoryDispatcher<Binary> factoryDispatcher() {
		AnyType byteInteger = integerType(0, 266);
		AnyType base64Format = enumType("%b", "%u", "%B", "%s", "%r");
		AnyType stringHash = structType(structElement("value", stringType()), structElement(optionalType("format"), base64Format));
		AnyType arrayHash = structType(structElement("value", arrayType(byteInteger)));
		AnyType binaryArgsHash = variantType(stringHash, arrayHash);

		return dispatcher(
				constructor(
						(args) -> fromString((String)args.get(0), "%B"),
						stringType()),
				constructor(
						(args) -> fromString((String)args.get(0), (String)args.get(1)),
						stringType(), base64Format),
				constructor(
						(args) -> fromArray((List<Number>)args.get(0)),
						arrayType(byteInteger)),
				constructor(
						(args) -> fromHash((Map<String,Object>)args.get(0)),
						binaryArgsHash)
		);
	}

	private Binary fromArray(List<Number> array) {
		int idx = array.size();
		byte[] bytes = new byte[idx];
		while(--idx >= 0)
			bytes[idx] = array.get(idx).byteValue();
		return new Binary(bytes);
	}

	private Binary fromString(String str, String format) {
		if(format == null)
			format = "%B";

		switch(format) {
		case "%b":
			return Binary.fromBase64(str);
		case "%B":
			return Binary.fromBase64Strict(str);
		case "%s":
			return Binary.fromUTF8(str);
		case "%r":
			return new Binary(str.getBytes(StandardCharsets.ISO_8859_1));
		default:
			throw new IllegalArgumentException(String.format("Unsupported Binary format '%s'", format));
		}
	}

	private Binary fromHash(Map<String,Object> hash) {
		Object value = hash.get("value");
		return value instanceof List<?>
			? fromArray((List<Number>)value)
			: fromString((String)value, (String)hash.get("format"));
	}

	@Override
	public AnyType generalize() {
		return DEFAULT;
	}

	public boolean roundtripWithString() {
		return true;
	}

	@SuppressWarnings("unused")
	static ObjectType registerPcoreType(PcoreImpl pcore) {
		return ptype = pcore.createObjectType("Pcore::BinaryType", "Pcore::AnyType");
	}

	@SuppressWarnings("unused")
	static void registerImpl(PcoreImpl pcore) {
		pcore.registerImpl(ptype, binaryTypeDispatcher());
	}

	@Override
	boolean isInstance(Object o, RecursionGuard guard) {
		return o instanceof Binary;
	}

	@Override
	boolean isUnsafeAssignable(AnyType t, RecursionGuard guard) {
		return t instanceof BinaryType;
	}
}
