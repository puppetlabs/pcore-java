package com.puppet.pcore;

import com.puppet.pcore.impl.Helpers;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Holds a sequence of bytes and provides conversion methods to convert to and from base64 encoding.
 */
public class Binary implements Comparable<Binary> {
	private final byte[] buffer;

	public static int compare(byte[] left, byte[] right) {
		int ll = left.length;
		int rl = right.length;
		int max = ll > rl ? rl : ll;
		for (int i = 0; i < max; ++i) {
			int a = (left[i] & 0xff);
			int b = (right[i] & 0xff);
			if (a != b)
				return a > b ? 1 : -1;
		}
		return ll > rl ? 1 : (rl > ll ? -1 : 0);
	}

	public Binary(byte[] buffer) {
		this.buffer = buffer.clone();
	}

	public Binary(byte[] buffer, int off, int len) {
		this.buffer = Arrays.copyOfRange(buffer, off, off + len);
	}

	public Binary(ByteBuffer buffer, int len) {
		this.buffer = new byte[len];
		buffer.get(this.buffer, 0, len);
	}

	public static Binary fromUTF8(String string) {
		return new Binary(string.getBytes(StandardCharsets.UTF_8));
	}

	public static Binary fromBase64(String string) {
		return new Binary(Base64.getMimeDecoder().decode(string));
	}

	public static Binary fromBase64Strict(String string) {
		byte[] bytes = string.getBytes(StandardCharsets.ISO_8859_1);
		if(bytes.length % 4 != 0)
			throw new IllegalArgumentException("Input string length must be dividable with 4");
		return new Binary(Base64.getDecoder().decode(bytes));
	}

	public static Binary fromBase64URLSafe(String string) {
		return new Binary(Base64.getUrlDecoder().decode(string));
	}

	public List<Byte> asList() {
		int idx = buffer.length;
		Byte[] result = new Byte[idx];
		while(--idx >= 0)
			result[idx] = buffer[idx];
		return Helpers.asList(result);
	}

	@Override
	public int compareTo(Binary o) {
		return compare(buffer, o.buffer);
	}

	public boolean equals(Object o) {
		return o instanceof Binary && Arrays.equals(buffer, ((Binary)o).buffer);
	}

	public int hashCode() {
		return Arrays.hashCode(buffer);
	}

	public byte[] toByteArray() {
		return buffer.clone();
	}

	public String toMimeString() {
		return Base64.getMimeEncoder().encodeToString(buffer) + '\n';
	}

	public String toString() {
		return Base64.getEncoder().encodeToString(buffer);
	}

	public String toURLSafeString() {
		return Base64.getUrlEncoder().encodeToString(buffer);
	}

	public String toUTF8String() {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();
		try {
			return decoder.decode(getBuffer()).toString();
		} catch(CharacterCodingException e) {
			throw new IllegalArgumentException("binary data is not valid UTF-8: " + e.getMessage());
		}
	}

	public void writeTo(ByteBuffer out, int off, int len) {
		out.put(buffer, off, len);
	}

	public void writeTo(OutputStream out, int off, int len) throws IOException {
		out.write(buffer, off, len);
	}

	public ByteBuffer getBuffer() {
		return ByteBuffer.wrap(buffer).asReadOnlyBuffer();
	}
}
