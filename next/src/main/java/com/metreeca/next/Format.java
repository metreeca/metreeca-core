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

import java.io.*;
import java.util.Optional;
import java.util.function.Consumer;

import static com.metreeca.form.things.Transputs.data;
import static com.metreeca.form.things.Transputs.text;


/**
 * Message body format.
 *
 * <p>Manages the conversion between structured and raw message body representations.</p>
 *
 * @param <V> the type of the structured message body representation managed by the format
 */
public interface Format<V> {

	/**
	 * Inbound raw message body format.
	 */
	public static final Format<Source> Inbound=new Format<Source>() {};

	/**
	 * Outbound raw message body format.
	 */
	public static final Format<Consumer<Target>> Outbound=new Format<Consumer<Target>>() {};

	/**
	 * Textual message body format.
	 *
	 * <p>{@linkplain #get(Message) Retrieves} the textual content of the message body from the reader supplied by its
	 * {@link #Inbound} representation.</p>
	 *
	 * <p>Configures the {@link #Outbound} representation of the message body to write its textual content to the
	 * writer supplied by the accepted {@link Target}.</p>
	 */
	public static final Format<String> Text=new Format<String>() {

		@Override public Optional<String> get(final Message<?> message) {
			return message.body(Inbound).map(source -> {
				try (final Reader reader=source.reader()) {

					return text(reader);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override public void set(final Message<?> message, final String value) {
			message.body(Outbound, target -> {
						try (final Writer writer=target.writer()) {

							writer.write(value);

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}
			);
		}

	};

	/**
	 * Binary message body format.
	 *
	 * <p>{@linkplain #get(Message) Retrieves} the binary content of the message body from the input stream supplied by
	 * its {@link #Inbound} representation.</p>
	 *
	 * <p>Configures the {@link #Outbound} representation of the message body to write its binary content to the
	 * output stream supplied by the accepted {@link Target}.</p>
	 */
	public static final Format<byte[]> Data=new Format<byte[]>() {

		@Override public Optional<byte[]> get(final Message<?> message) {
			return message.body(Inbound).map(source -> {
				try (final InputStream input=source.input()) {

					return data(input);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override public void set(final Message<?> message, final byte[] value) {
			message.body(Outbound, target -> {
						try (final OutputStream output=target.output()) {

							output.write(value);

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}
			);
		}

	};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the representation of a message body.
	 *
	 * @param message the message whose body representation associated with this format is to be retrieved
	 *
	 * @return the optional body representation associated with this format, if it was possible to derive one from
	 * {@code message}; an empty optional, otherwise
	 */
	public default Optional<V> get(final Message<?> message) { return Optional.empty(); }

	/**
	 * Configures derived message body representations.
	 *
	 * @param message the message whose body representations derived from {@code value} on the basis of this format are
	 *                to be configured
	 * @param value   the body representation to be used as basis for derived body representations for {@code message}
	 */
	public default void set(final Message<?> message, final V value) {}

}
