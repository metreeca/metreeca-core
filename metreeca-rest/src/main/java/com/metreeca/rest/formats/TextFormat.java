/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.lang.String.valueOf;


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
	public static final Pattern MIMEPattern=Pattern.compile("(?i:^text/.+$)");


	/**
	 * Creates a textual message format.
	 *
	 * @return a new textual message format
	 */
	public static TextFormat text() {
		return new TextFormat();
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

				return Either.Right(Xtream.text(reader));

			} catch ( final UnsupportedEncodingException e ) {

				return Either.Left(status(BadRequest, e));

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
		try {

			final String charset=message.charset();
			final byte[] bytes=value.getBytes(charset);

			return message

					.header("~Content-Type", MIME)
					.header("~Content-Length", valueOf(bytes.length))

					.body(output(), output -> {
						try {

							output.write(bytes);
							output.flush();

						} catch ( final IOException e ) {

							throw new UncheckedIOException(e);

						}
					});

		} catch ( final UnsupportedEncodingException e ) {

			throw new UncheckedIOException(e); // !!! report to client as 4/5xx? how?

		}
	}

}
