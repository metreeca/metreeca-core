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

import com.metreeca.next.Format;
import com.metreeca.next.Message;
import com.metreeca.next.Target;

import java.io.*;
import java.util.Optional;

import static com.metreeca.form.things.Transputs.text;


/**
 * Textual message body format.
 */
public final class Text implements Format<String> {

	/**
	 * The singleton textual message body format.
	 */
	public static final Format<String> Format=new Text();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Text() {} // singleton


	/**
	 * @return the optional textual body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link Inbound#Format} representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<String> get(final Message<?> message) {
		return message.body(com.metreeca.next.formats.Inbound.Format).map(source -> {
			try (final Reader reader=source.reader()) {

				return text(reader);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures the {@link Outbound#Format} representation of {@code message} to write the textual {@code value} to
	 * the writer supplied by the accepted {@link Target}.
	 */
	@Override public void set(final Message<?> message, final String value) {
		message.body(com.metreeca.next.formats.Outbound.Format, target -> {
					try (final Writer writer=target.writer()) {

						writer.write(value);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}
		);
	}

}
