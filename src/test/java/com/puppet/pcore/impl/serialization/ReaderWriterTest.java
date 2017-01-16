package com.puppet.pcore.impl.serialization;

import com.puppet.pcore.Binary;
import com.puppet.pcore.Comment;
import com.puppet.pcore.Symbol;
import com.puppet.pcore.impl.PcoreImpl;
import com.puppet.pcore.impl.serialization.extension.ArrayStart;
import com.puppet.pcore.impl.serialization.extension.MapStart;
import com.puppet.pcore.impl.serialization.extension.PcoreObjectStart;
import com.puppet.pcore.impl.serialization.extension.SensitiveStart;
import com.puppet.pcore.impl.serialization.msgpack.MsgPackSerializationFactory;
import com.puppet.pcore.semver.Version;
import com.puppet.pcore.semver.VersionRange;
import com.puppet.pcore.serialization.Reader;
import com.puppet.pcore.serialization.Writer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unused")
@DisplayName("The Writer/Reader can write and read an object of type")
public class ReaderWriterTest {
	@Test
	@DisplayName("ArrayStart")
	public void rwArrayStart() throws IOException {
		ArrayStart as = new ArrayStart(10);
		assertEquals(as, writeAndRead(as));
	}

	@Test
	@DisplayName("Binary")
	public void rwBinary() throws IOException {
		Binary binary = new Binary(new byte[] { (byte)139, 12, (byte)233, 5, 42 });
		assertEquals(binary, writeAndRead(binary));
	}

	@Test
	@DisplayName("Boolean")
	public void rwBoolean() throws IOException {
		assertEquals(true, writeAndRead(true));
		assertEquals(false, writeAndRead(false));
	}

	@Test
	@DisplayName("byte[]")
	public void rwByteArray() throws IOException {
		byte[] bytes = new byte[] { (byte)139, 12, (byte)233, 5, 42 };
		assertArrayEquals(bytes, (byte[])writeAndRead(bytes));
	}

	@Test
	@DisplayName("Comment")
	public void rwComment() throws IOException {
		Comment comment = new Comment("This is a comment");
		assertEquals(comment, writeAndRead(comment));
	}

	@Test
	@DisplayName("Float")
	public void rwFloat() throws IOException {
		assertEquals(123.45, writeAndRead(123.45));
		assertEquals(123456.456789, writeAndRead(123456.456789));
		assertEquals((double)123.45F, writeAndRead(123.45F));
	}

	@Test
	@DisplayName("Integer")
	public void rwInteger() throws IOException {
		assertEquals(12L, writeAndRead((byte)12));
		assertEquals(12345L, writeAndRead((short)12345));
		assertEquals(12345678L, writeAndRead((long)12345678));
		assertEquals(543123456789L, writeAndRead(543123456789L));
		assertEquals(12L, writeAndRead(12));
		assertEquals(12345L, writeAndRead(12345));
		assertEquals(12345678L, writeAndRead(12345678));
		assertEquals(543123456789L, writeAndRead(543123456789L));
	}

	@Test
	@DisplayName("MapStart")
	public void rwMapStart() throws IOException {
		MapStart ms = new MapStart(10);
		assertEquals(ms, writeAndRead(ms));
	}

	@Test
	@DisplayName("PcoreObjectStart")
	public void rwObjectStart() throws IOException {
		PcoreObjectStart os = new PcoreObjectStart("My::Test::Object::Test", 10);
		assertEquals(os, writeAndRead(os));
	}

	@Test
	@DisplayName("SensitiveStart")
	public void rwSensitiveStart() throws IOException {
		SensitiveStart ss = SensitiveStart.SINGLETON;
		assertEquals(ss, writeAndRead(ss));
	}

	@Test
	@DisplayName("String")
	public void rwString() throws IOException {
		assertEquals("Blue Öyster Cult", writeAndRead("Blue Öyster Cult"));
	}

	@Test
	@DisplayName("Symbol")
	public void rwSymbol() throws IOException {
		Symbol sym = new Symbol("someSymbol");
		assertEquals(sym, writeAndRead(sym));
	}

	@Test
	@DisplayName("TimeSpan")
	public void rwTimeStamp() throws IOException {
		Duration d = Duration.ofDays(23481).minusHours(12).negated();
		assertEquals(d, writeAndRead(d));
	}

	@Test
	@DisplayName("Timestamp")
	public void rwTimestamp() throws IOException {
		Instant now = Instant.now();
		assertEquals(now, writeAndRead(now));
	}

	@Test
	@DisplayName("Undef")
	public void rwUndef() throws IOException {
		assertEquals(null, writeAndRead(null));
	}

	@Test
	@DisplayName("Version")
	public void rwVersion() throws IOException {
		Version version = Version.create(10,20,30,"beta2");
		assertEquals(version, writeAndRead(version));
	}

	@Test
	@DisplayName("VersionRange")
	public void rwVersionRange() throws IOException {
		VersionRange versionRange = VersionRange.create("1.0.0 - 2.0.0");
		assertEquals(versionRange, writeAndRead(versionRange));
	}

	public Object writeAndRead(Object value) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		SerializationFactoryImpl factory = new MsgPackSerializationFactory(new PcoreImpl());
		Writer writer = factory.writerOn(emptyMap(), out);
		writer.write(value);
		writer.finish();
		Reader reader = factory.readerOn(new ByteArrayInputStream(out.toByteArray()));
		return reader.read();
	}
}

