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
import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;


/**
 * Textual body format.
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
	 * Creates a textual format.
	 *
	 * @return the new textual format
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

			final char[] buffer=new char[1024];

			for (int n; (n=reader.read(buffer)) >= 0; writer.write(buffer, 0, n)) {}

			writer.flush();

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
	 * @return a result providing access to the textual representation of {@code message}, as retrieved from the input
	 * stream supplied by its {@link InputFormat} body, if one is present; a failure describing the decoding error,
	 * otherwise
	 */
	@Override public Result<String, MessageException> decode(final Message<?> message) {
		return message.body(input()).process(source -> {
			try (
					final InputStream input=source.get();
					final Reader reader=new InputStreamReader(input, message.charset())
			) {

				return Value(text(reader));

			} catch ( final UnsupportedEncodingException e ) {

				return Error(status(BadRequest, e));

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		});
	}

	/**
	 * Configures the {@link OutputFormat} representation of {@code message} to write the textual {@code value} to the
	 * accepted output stream and sets the {@code Content-Type} header to {@value #MIME}, unless already defined.
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
