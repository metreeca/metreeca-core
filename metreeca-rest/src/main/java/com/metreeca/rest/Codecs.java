/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Codec utilities.
 */
public final class Codecs {

	private static final byte[] EmptyData={};
	private static final String EmptyText="";

	private static final Pattern SpacePattern=Pattern.compile("[\\s\\p{Space}\\p{Z}]+");


	//// Text /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String normalize(final String string) {
		if ( string == null || string.isEmpty() ) { return string; } else {

			final int length=string.length();

			final Matcher matcher=SpacePattern.matcher(string);
			final StringBuffer buffer=new StringBuffer(length);

			while ( matcher.find() ) {
				matcher.appendReplacement(buffer, matcher.start() == 0 || matcher.end() == length ? "" : " ");
			}

			return matcher.appendTail(buffer).toString();
		}
	}


	//// URLs /////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts a string into a URL.
	 *
	 * @param url the string to be converted into a URL
	 *
	 * @return the URL converted from {@code url}
	 *
	 * @throws NullPointerException     if {@code url} is null
	 * @throws IllegalArgumentException if {@code url} is not a well-formed URL
	 */
	public static URL url(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		try {

			return new URL(url);

		} catch ( final MalformedURLException e ) {
			throw new IllegalArgumentException(e.getMessage());
		}
	}


	public static String encode(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try {
			return URLEncoder.encode(text, UTF_8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}

	public static String decode(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try {
			return URLDecoder.decode(text, UTF_8.name());
		} catch ( final UnsupportedEncodingException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}


	public static Map<String, List<String>> parameters(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Map<String, List<String>> parameters=new LinkedHashMap<>();

		final int length=query.length();

		for (int head=0, tail; head < length; head=tail+1) {
			try {

				final int equal=query.indexOf('=', head);
				final int ampersand=query.indexOf('&', head);

				tail=(ampersand >= 0) ? ampersand : length;

				final boolean split=equal >= 0 && equal < tail;

				final String label=URLDecoder.decode(query.substring(head, split ? equal : tail), "UTF-8");
				final String value=URLDecoder.decode(query.substring(split ? equal+1 : tail, tail), "UTF-8");

				parameters.compute(label, (name, values) -> {

					final List<String> strings=(values != null) ? values : new ArrayList<>();

					strings.add(value);

					return strings;

				});

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}
		}

		return parameters;
	}


	//// Resources ////////////////////////////////////////////////////////////////////////////////////////////////////

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
					format("unknown resource [%s:%s]", clazz.getName(), resource),
					clazz.getName(),
					resource
			);
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

			final InputStream input=resource(master, resource).openStream();

			return resource.endsWith(".gz") ? new GZIPInputStream(input) : input;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static Reader reader(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return reader(input(master, resource));
	}

	/**
	 * Retrieves the binary content of a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be read
	 * @param resource the path of the resource to be read, relative to the target class
	 *
	 * @return the binary content of the given {@code resource}
	 *
	 * @throws MissingResourceException if {@code resource} is not available
	 */
	public static byte[] data(final Object master, final String resource) {

		if ( master == null ) {
			throw new NullPointerException("null master");
		}

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		try ( final InputStream input=input(master, resource) ) {

			return data(input);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Retrieves the textual content of a class resource.
	 *
	 * @param master   the target class or an instance of the target class for the resource to be read
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

		try ( final Reader reader=reader(master, resource) ) {

			return text(reader);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	//// Input Utilities
	// /////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an empty input stream.
	 *
	 * @return an empty input stream
	 */
	public static InputStream input() {
		return new ByteArrayInputStream(EmptyData);
	}

	public static InputStream input(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		return input(reader, UTF_8);
	}

	public static InputStream input(final Reader reader, final String charset) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return input(reader, Charset.forName(charset));
	}

	public static InputStream input(final Reader reader, final Charset charset) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return new ReaderInputStream(reader, charset);
	}


	/**
	 * Creates an empty reader.
	 *
	 * @return an emtpy reader
	 */
	public static Reader reader() {
		return new StringReader(EmptyText);
	}

	public static Reader reader(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		return reader(input, UTF_8);
	}

	public static Reader reader(final InputStream input, final String charset) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return reader(input, Charset.forName(charset));
	}

	public static Reader reader(final InputStream input, final Charset charset) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return new InputStreamReader(input, charset);
	}


	public static byte[] data(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {
			return data(output, input).toByteArray();
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static String text(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try ( final StringWriter writer=new StringWriter() ) {
			return text(writer, reader).toString();
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	//// Output Utilities /////////////////////////////////////////////////////////////////////////////////////////////

	public static OutputStream output(final Writer writer) {
		return output(writer, UTF_8);
	}

	public static OutputStream output(final Writer writer, final String charset) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return output(writer, Charset.forName(charset));
	}

	public static OutputStream output(final Writer writer, final Charset charset) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return new WriterOutputStream(writer, charset);
	}


	public static Writer writer(final OutputStream output) {
		return writer(output, UTF_8);
	}

	public static Writer writer(final OutputStream output, final String charset) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return writer(output, Charset.forName(charset));
	}

	public static Writer writer(final OutputStream output, final Charset charset) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( charset == null ) {
			throw new NullPointerException("null charset");
		}

		return new OutputStreamWriter(output, charset);
	}


	public static <T extends OutputStream> T data(final T output, final byte... data) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		try ( final InputStream input=new ByteArrayInputStream(data) ) {
			return data(output, input);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public static <T extends Writer> T text(final T writer, final String text) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		try ( final Reader reader=new StringReader(text) ) {
			return text(writer, reader);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public static <T extends OutputStream> T data(final T output, final InputStream input) {

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

	public static <T extends Writer> T text(final T writer, final Reader reader) {

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


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Codecs() {} // utility


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ReaderInputStream extends InputStream {

		private final Reader reader;
		private final CharsetEncoder encoder;

		private final ByteBuffer bytes=ByteBuffer.allocate(16);
		private final CharBuffer chars=CharBuffer.allocate(16);


		private ReaderInputStream(final Reader reader, final Charset charset) {

			if ( reader == null ) {
				throw new NullPointerException("null reader");
			}

			if ( charset == null ) {
				throw new NullPointerException("null charset");
			}

			this.reader=reader;
			this.encoder=charset.newEncoder();

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


		private WriterOutputStream(final Writer writer, final Charset charset) {

			if ( writer == null ) {
				throw new NullPointerException("null writer");
			}

			if ( charset == null ) {
				throw new NullPointerException("null charset");
			}

			this.writer=writer;
			this.decoder=charset.newDecoder();
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
					writer.write(chars.get());
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
