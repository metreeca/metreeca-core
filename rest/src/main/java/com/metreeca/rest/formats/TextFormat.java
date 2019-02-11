/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.formats;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.*;

import java.io.*;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;


/**
 * Textual body format.
 */
public final class TextFormat implements Format<String> {

	private static final TextFormat Instance=new TextFormat();


	/**
	 * Retrieves the textual format.
	 *
	 * @return the singleton textual format instance
	 */
	public static TextFormat text() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TextFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to the textual representation of {@code message}, as retrieved from the reader
	 * supplied by its {@link ReaderFormat} body, if one is present; a failure describing the processing error,
	 * otherwise
	 */
	@Override public Result<String, Failure> get(final Message<?> message) {
		return message.body(reader()).fold(

				source -> {
					try (final Reader reader=source.get()) {

						return Value(Codecs.text(reader));

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				},

				error -> error.equals(Format.Missing) ? Value("") : Error(error)

		);
	}

	/**
	 * Configures the {@link WriterFormat} body of {@code message} to write the textual {@code value} to the output
	 * stream supplied by the accepted output stream supplier.
	 */
	@Override public <T extends Message<T>> T set(final T message) {
		return message.pipe(writer(), consumer -> message.body(text()).value(bytes -> target -> {
			try (final Writer output=target.get()) {

				output.write(bytes);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}));
	}

}
