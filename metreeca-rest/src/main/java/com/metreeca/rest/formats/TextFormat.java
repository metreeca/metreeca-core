/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.UnaryOperator;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;


/**
 * Textual body format.
 */
public final class TextFormat extends Format<String> {

	/**
	 * The default MIME type for textual messages ({@value}).
	 */
	public static final String MIME="text/plain";


	/**
	 * Creates a textual format.
	 *
	 * @return the new textual format
	 */
	public static TextFormat text() {
		return new TextFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TextFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to the textual representation of {@code message}, as retrieved from the reader
	 * supplied by its {@link ReaderFormat} body, if one is present; a failure describing the processing error,
	 * otherwise
	 */
	@Override public Result<String, UnaryOperator<Response>> get(final Message<?> message) {
		return message.body(reader()).value(source -> {
			try ( final Reader reader=source.get() ) {

				return Codecs.text(reader);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures a message to hold a textual body representation.
	 *
	 * <ul>
	 *
	 * <li>the {@link InputFormat} body of {@code message} is configured to generate an input stream reading the
	 * textual {@code value} using the character encoding specified in the {@code Content-Type} header of
	 * {@code message} or the {@linkplain StandardCharsets#UTF_8 default charset} if none is specified;</li>
	 *
	 * <li>the {@link ReaderFormat} body of {@code message} is configured to generate a reader reading the
	 * textual {@code value};</li>
	 *
	 * <li>the {@link WriterFormat} body of {@code message} is configured to write the textual {@code value} to the
	 * writer supplied by the accepted writer supplier.</li>
	 *
	 * </ul>
	 */
	@Override public <M extends Message<M>> M set(final M message, final String value) {
		return message

				.body(input(), () -> new ByteArrayInputStream(value.getBytes(message.charset())))

				.body(reader(), () -> new StringReader(value))

				.body(writer(), writer -> {
					try {

						writer.write(value);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				});
	}

}
