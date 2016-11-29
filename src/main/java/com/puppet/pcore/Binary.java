package com.puppet.pcore;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

/**
 * Holds a sequence of bytes and provides conversion methods to convert to and from base64 encoding.
 */
public class Binary {
	private final byte[] buffer;

	public Binary(byte[] buffer) {
		this.buffer = buffer.clone();
	}

	public Binary(byte[] buffer, int off, int len) {
		this.buffer = Arrays.copyOfRange(buffer, off, off + len);
	}

	public Binary(ByteBuffer buffer, int off, int len) {
		this.buffer = new byte[len];
		buffer.get(this.buffer, off, len);
	}

	public static Binary fromBase64(String string) {
		return new Binary(Base64.getMimeDecoder().decode(string));
	}

	public static Binary fromBase64Strict(String string) {
		return new Binary(Base64.getDecoder().decode(string));
	}

	public static Binary fromBase64URLSafe(String string) {
		return new Binary(Base64.getUrlDecoder().decode(string));
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
		return Base64.getMimeEncoder().encodeToString(buffer);
	}

	public String toString() {
		return Base64.getEncoder().encodeToString(buffer);
	}

	public String toURLSafeString() {
		return Base64.getUrlEncoder().encodeToString(buffer);
	}

	public void writeTo(ByteBuffer out, int off, int len) {
		out.put(buffer, off, len);
	}

	public void writeTo(OutputStream out, int off, int len) throws IOException {
		out.write(buffer, off, len);
	}
}
