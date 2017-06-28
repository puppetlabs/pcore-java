package com.puppet.pcore;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static com.puppet.pcore.impl.Helpers.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings({"unused", "ResultOfMethodCallIgnored"})
@DisplayName("Binary")
public class BinaryTest {
	@Test
	@DisplayName("can be created from byte[]")
	void createFromByteArray() {
		assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, new Binary(new byte[] {1, 2, 3}).toByteArray()));
	}

	@Test
	@DisplayName("can be created from partial byte[]")
	void createFromPartialByteArray() {
		assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, new Binary(new byte[] {0, 1, 2, 3, 4}, 1, 3).toByteArray()));
	}

	@Test
	@DisplayName("can be created from partial ByteBuffer")
	void createFromPartialByteBuffer() {
		assertTrue(Arrays.equals(new byte[] { 1, 2, 3 }, new Binary(ByteBuffer.wrap(new byte[] {1, 2, 3, 4}), 3).toByteArray()));
	}

	@Test
	@DisplayName("can be created from base64 when padding is missing")
	void createFromBase64() {
		assertEquals("YmluYXI=", Binary.fromBase64("YmluYXI").toString());
	}

	@Test
	@DisplayName("can be created from base64Strict when padding is correct")
	void createFromBase64Strict() {
		assertEquals("YmluYXI=", Binary.fromBase64Strict("YmluYXI=").toString());
	}

	@Test
	@DisplayName("can be created from an Base64 using URL safe encoding")
	void failCreateFromBase64Strict() {
		Throwable ex = assertThrows(IllegalArgumentException.class, () -> Binary.fromBase64Strict("YmluYXI").toString());
		assertEquals("Input string length must be dividable with 4", ex.getMessage());
	}

	@Test
	@DisplayName("can be created from an Base64 using URL safe encoding")
	void createFromBase64URLSafe() {
		assertEquals("++//", Binary.fromBase64URLSafe("--__").toString());
	}

	@Test
	@DisplayName("can be created from an UTF-8 string")
	void createFromUTF8() {
		assertEquals("YmluYXJ5", Binary.fromUTF8("binary").toString());
	}

	@Test
	@DisplayName("can produce a mime string")
	void produceMimeString() {
		assertEquals("YmluYXJ5\n", Binary.fromBase64Strict("YmluYXJ5").toMimeString());
	}

	@Test
	@DisplayName("can produce an URL-safe string")
	void produceURLSafeString() {
		assertEquals("--__", Binary.fromBase64URLSafe("--__").toURLSafeString());
	}

	@Test
	@DisplayName("can produce an UTF-8 encoded string")
	void produceUTF8String() {
		assertEquals("binary", Binary.fromBase64Strict("YmluYXJ5").toUTF8String());
	}

	@Test
	@DisplayName("can not produce an invalid UTF-8 encoded string")
	void failInvalidUTF8String() {
		assertThrows(IllegalArgumentException.class, () -> Binary.fromBase64("apa=").toUTF8String());
	}

	@Test
	@DisplayName("can be compared to another instance for magnitude")
	void compareInstances() {
		assertEquals(0, Binary.fromBase64Strict("YmluYXJ5").compareTo(Binary.fromBase64Strict("YmluYXJ5")));
		assertEquals(1, Binary.fromBase64Strict("YmluYXJ5").compareTo(Binary.fromBase64Strict("YmluYXJ4")));
		assertEquals(-1, Binary.fromBase64Strict("YmluYXJ5").compareTo(Binary.fromBase64Strict("YmluYXJ6")));
		assertEquals(1, Binary.fromBase64Strict("YmluYXJ5").compareTo(Binary.fromBase64Strict("YmluYXJ=")));
		assertEquals(-1, Binary.fromBase64Strict("Ymlu").compareTo(Binary.fromBase64Strict("YmluYXJ5")));
	}

	@Test
	@DisplayName("can be compared to another instance for equality")
	void compareInstanceEquality() {
		assertTrue(Binary.fromBase64Strict("YmluYXJ5").equals(Binary.fromBase64Strict("YmluYXJ5")));
		assertFalse(Binary.fromBase64Strict("YmluYXJ5").equals(Binary.fromBase64Strict("YmluYXJ4")));
	}

	@Test
	@DisplayName("equal instances produce equal hashCode")
	void producesHashCode() {
		assertEquals(Binary.fromBase64Strict("YmluYXJ5").hashCode(), Binary.fromBase64Strict("YmluYXJ5").hashCode());
	}

	@Test
	@DisplayName("can be converted to a list")
	void convertToList() {
		assertEquals(asList((byte)1, (byte)2, (byte)3), new Binary(new byte[] {1, 2, 3}).asList());
	}

	@Test
	@DisplayName("can be converted to a read-only ByteBuffer")
	void convertToByteBuffer() {
		ByteBuffer bf = new Binary(new byte[] {1, 2, 3}).getBuffer();
		assertTrue(bf.isReadOnly());
		assertEquals(1, bf.get());
		assertEquals(2, bf.get());
		assertEquals(3, bf.get());
		assertThrows(BufferUnderflowException.class, bf::get);
	}

	@Test
	@DisplayName("can write selected contents to a ByteBuffer")
	void writeToBuffer() {
		byte[] result = new byte[3];
		ByteBuffer bf = ByteBuffer.wrap(result);
		new Binary(new byte[] {0, 1, 2, 3, 4}).writeTo(bf, 1, 3);
		assertTrue(Arrays.equals(new byte[] {1, 2, 3}, result));
	}

	@Test
	@DisplayName("can write selected contents to an OutputStream")
	void writeToStream() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new Binary(new byte[] {0, 1, 2, 3, 4}).writeTo(out, 1, 3);
		assertTrue(Arrays.equals(new byte[] {1, 2, 3}, out.toByteArray()));
	}
}
