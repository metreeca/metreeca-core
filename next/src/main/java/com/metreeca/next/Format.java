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

package com.metreeca.next;

import com.metreeca.form.things.Transputs;

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;


/**
 * Message body format.
 *
 * <p>Manages the conversion between structured and raw message body representations.</p>
 *
 * @param <V> the type of the structured message body representation managed by the format
 */
public interface Format<V> {

	public static final Format<Source> Source=new Format<Source>() {};

	public static final Format<Consumer<Target>> Target=new Format<Consumer<Target>>() {};


	/**
	 * The format for textual message body representations.
	 *
	 * <p>On decoding, </p>
	 *
	 * <p>Gets Retrieves the textual content of the reader provided by the {@linkplain #body() body supplier} of the target
	 * inbound message.</p>
	 */
	public static final Format<String> Text=new Format<String>() {

		@Override public Optional<String> get(final Message<?> message) {
			return message.body(Source).map(source -> {
				try (final Reader reader=source.reader()) {

					return Transputs.text(reader);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override public void set(final Message<?> message, final String value) {
			message.body(Target, target -> {
						try (final Writer writer=target.writer()) {

							writer.write(value);

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}
			);
		}

	};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public default Optional<V> get(final Message<?> message) { return Optional.empty(); }

	public default void set(final Message<?> message, final V value) {}

}
