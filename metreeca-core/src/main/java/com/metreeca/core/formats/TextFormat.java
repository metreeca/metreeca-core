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

package com.metreeca.core.formats;

import com.metreeca.core.*;

import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;


/**
 * Textual message format.
 */
public final class TextFormat extends Format<String> {

	/**
	 * The default MIME type for textual messages ({@value}).
	 */
	public static final String MIME="text/plain";

	/**
	 * A pattern matching textual MIME types, for instance {@code text/csv}.
	 */
	public static final Pattern MIMEPattern=Pattern.compile("(?i)^text/.+$");


	/**
	 * Creates a textual message format.
	 *
	 * @return a new textual message format
	 */
	public static TextFormat text() {
		return new TextFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String text(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try ( final StringWriter writer=new StringWriter() ) {

			Xtream.copy(writer, reader);

			return writer.toString();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	public static <W extends Writer> W text(final W writer, final String value) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		try {

			writer.write(value);
			writer.flush();

			return writer;

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TextFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the textual {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat} body, if one is available, taking into account the {@code message}
	 * {@linkplain Message#charset() charset}
	 */
	@Override public Either<MessageException, String> decode(final Message<?> message) {
		return message.body(input()).flatMap(source -> {
			try (
					final InputStream input=source.get();
					final Reader reader=new InputStreamReader(input, message.charset())
			) {

				return Right(text(reader));

			} catch ( final UnsupportedEncodingException e ) {

				return Left(status(BadRequest, e));

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		});
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes
	 * the textual {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the {@code message} {@linkplain Message#charset() charset}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final String value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {
					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						text(writer, value);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
