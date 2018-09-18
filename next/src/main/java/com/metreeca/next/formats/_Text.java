/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.formats;

import com.metreeca.next.*;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import static com.metreeca.form.things.Transputs.text;
import static com.metreeca.next.Result.value;


/**
 * Textual body format.
 */
public final class _Text implements Format<String> {

	/**
	 * The singleton textual body format.
	 */
	public static final Format<String> Format=new _Text();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Text() {} // singleton


	/**
	 * @return the optional textual body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link _Reader#Format} representation, if present; an empty optional, otherwise
	 */
	@Override public Result<String, Failure> get(final Message<?> message) {
		return message.body(_Reader.Format).value(source -> {
			try (final Reader reader=source.get()) {

				return value(text(reader));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures the {@link _Writer#Format} representation of {@code message} to write the textual {@code value} to the
	 * writer supplied by the accepted writer.
	 */
	@Override public void set(final Message<?> message, final String value) {
		message.body(_Writer.Format, writer -> {
			try {

				writer.write(value);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
