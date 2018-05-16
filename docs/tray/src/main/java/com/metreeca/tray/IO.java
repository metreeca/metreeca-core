/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.MissingResourceException;

import static java.lang.String.format;


/**
 * I/O utilities.
 */
public final class IO {

	/**
	 * The default charset for I/O operations.
	 */
	public static final Charset UTF8=Charset.forName("UTF-8");


	private static final byte[] EmptyData={};
	private static final String EmptyText="";


	//// URL Codecs ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String encode(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try {
			return URLEncoder.encode(text, UTF8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}

	public static String decode(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try {
			return URLDecoder.decode(text, UTF8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}


	//// Resources /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static URL resource(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		final Class<?> clazz=master instanceof Class ? (Class<?>)master : master.getClass();

		final URL url=clazz.getResource(resource.startsWith(".") ? clazz.getSimpleName()+resource : resource);

		if ( url == null ) {
			throw new MissingResourceException(
					format("unknown resource [%s:%s]", clazz.getName(), resource), clazz.getName(), resource);
		}

		return url;
	}

	public static InputStream input(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		try {
			return resource(master, resource).openStream();
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Retrieves the text of a class resource.
	 *
	 * @param master   the target class or an instance of the target class  for the resource to be read
	 * @param resource the path of the resource to be read, relative to the target class
	 *
	 * @return the textual content of the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static String text(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return text(reader(input(master, resource)));
	}


	//// Input Utilities ///////////////////////////////////////////////////////////////////////////////////////////////

	public static InputStream input() {
		return new ByteArrayInputStream(EmptyData);
	}

	public static InputStream input(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		return input(reader, UTF8.name());
	}

	public static InputStream input(final Reader reader, final String encoding) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( encoding == null ) {
			throw new NullPointerException("null encoding");
		}

		return new ReaderInputStream(reader, encoding);
	}


	public static Reader reader() {
		return new StringReader(EmptyText);
	}

	public static Reader reader(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		return reader(input, UTF8.name());
	}

	public static Reader reader(final InputStream input, final String encoding) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		if ( encoding == null ) {
			throw new NullPointerException("null encoding");
		}

		try {
			return new InputStreamReader(input, encoding);
		} catch ( final UnsupportedEncodingException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static byte[] data(final InputStream input) throws UncheckedIOException {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try (final ByteArrayOutputStream output=new ByteArrayOutputStream()) {
			return data(output, input).toByteArray();
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static String text(final Reader reader) throws UncheckedIOException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try (final StringWriter writer=new StringWriter()) {
			return text(writer, reader).toString();
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	//// Output Utilities //////////////////////////////////////////////////////////////////////////////////////////////

	public static OutputStream output(final Writer writer) {
		return output(writer, UTF8.name());
	}

	public static OutputStream output(final Writer writer, final String encoding) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( encoding == null ) {
			throw new NullPointerException("null encoding");
		}

		return new WriterOutputStream(writer, encoding);
	}


	public static Writer writer(final OutputStream output) {
		return writer(output, UTF8.name());
	}

	public static Writer writer(final OutputStream output, final String encoding) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( encoding == null ) {
			throw new NullPointerException("null encoding");
		}

		try {
			return new OutputStreamWriter(output, encoding);
		} catch ( final UnsupportedEncodingException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static <T extends OutputStream> T data(final T output, final byte... data) throws UncheckedIOException {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		try (final InputStream input=new ByteArrayInputStream(data)) {
			return data(output, input);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T extends Writer> T text(final T writer, final String text) throws UncheckedIOException {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try (final Reader reader=new StringReader(text)) {
			return text(writer, reader);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static <T extends OutputStream> T data(final T output, final InputStream input) throws UncheckedIOException {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try {

			final byte[] buffer=new byte[1024];

			for (int n; (n=input.read(buffer)) >= 0; output.write(buffer, 0, n)) {}

			output.flush();

			return output;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T extends Writer> T text(final T writer, final Reader reader) throws UncheckedIOException {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try {

			final char[] buffer=new char[1024];

			for (int n; (n=reader.read(buffer)) >= 0; writer.write(buffer, 0, n)) {}

			writer.flush();

			return writer;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private IO() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ReaderInputStream extends InputStream {

		private final Reader reader;
		private final CharsetEncoder encoder;

		private final ByteBuffer bytes=ByteBuffer.allocate(16);
		private final CharBuffer chars=CharBuffer.allocate(16);


		public ReaderInputStream(final Reader reader, final String encoding) {

			if ( reader == null ) {
				throw new NullPointerException("null reader");
			}

			if ( encoding == null ) {
				throw new NullPointerException("null encoding");
			}

			this.reader=reader;
			this.encoder=Charset.forName(encoding).newEncoder();

			// put buffers in reading mode

			this.bytes.flip();
			this.chars.flip();
		}


		@Override public int read() throws IOException {

			if ( !bytes.hasRemaining() ) {

				chars.compact();

				final boolean eof=reader.read(chars) < 0;

				chars.flip();

				bytes.compact();

				if ( !encoder.encode(chars, bytes, eof).isUnderflow() ) {
					throw new CharacterCodingException();
				}

				if ( eof ) {
					encoder.flush(bytes);
					encoder.reset();
				}

				bytes.flip();
			}

			return bytes.hasRemaining() ? bytes.get()&0XFF : -1;
		}

		@Override public void close() throws IOException {
			try {
				super.close();
			} finally {
				reader.close();
			}
		}
	}

	private static final class WriterOutputStream extends OutputStream {

		private final Writer writer;
		private final CharsetDecoder decoder;

		private final ByteBuffer bytes=ByteBuffer.allocate(16);
		private final CharBuffer chars=CharBuffer.allocate(16);


		public WriterOutputStream(final Writer writer, final String encoding) {

			if ( writer == null ) {
				throw new NullPointerException("null writer");
			}

			if ( encoding == null ) {
				throw new NullPointerException("null encoding");
			}

			this.writer=writer;
			this.decoder=Charset.forName(encoding).newDecoder();
		}


		@Override public void write(final int b) throws IOException {

			bytes.put((byte)(b&0xFF));

			bytes.flip();

			if ( !decoder.decode(bytes, chars, false).isUnderflow() ) {
				throw new CharacterCodingException();
			}

			bytes.compact();

			flush();
		}

		@Override public void flush() throws IOException {
			try {

				chars.flip();

				while ( chars.hasRemaining() ) {
					writer.write((int)chars.get());
				}

				chars.compact();

				super.flush();

			} finally {
				writer.flush();
			}
		}

		@Override public void close() throws IOException {
			try {

				bytes.flip();

				if ( !decoder.decode(bytes, chars, true).isUnderflow() ) {
					throw new CharacterCodingException();
				}

				decoder.flush(chars);
				decoder.reset();

				bytes.compact();

				flush();

				super.close();

			} finally {
				writer.close();
			}
		}

	}

}
