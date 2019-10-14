/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.Format;
import com.metreeca.rest.Message;
import com.metreeca.rest.Codecs;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.metreeca.rest.formats.OutputFormat.output;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Textual output body format.
 */
public final class WriterFormat extends Format<Consumer<Supplier<Writer>>> {

	/**
	 * The default MIME type for outbound textual message bodies.
	 */
	private static final String MIME="text/plain";


	/**
	 * Creates a textual output body format.
	 *
	 * @return the new textual output body format
	 */
	public static WriterFormat writer() {
		return new WriterFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final OutputFormat output=output();

	private WriterFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures a message to hold a textual output body.
	 *
	 * <p>Configures the {@linkplain OutputFormat raw binary output body} of {@code message} to write the textual message
	 * body to the accepted output stream using the character encoding specified in its {@code Content-Type} header or
	 * the {@linkplain StandardCharsets#UTF_8 default charset} if none is specified.</p>
	 *
	 * <p>Sets the {@code Content-Type} header of {@code message} to {@link #MIME}, if not already set.</p>
	 */
	@Override public <M extends Message<M>> M set(final M message, final Consumer<Supplier<Writer>> value) {
		return message

				.header("~Content-Type", MIME)

				.body(output, source -> {

					try (final OutputStream output=source.get()) {

						value.accept(() -> Codecs.writer(output, message.charset().orElse(UTF_8.name())));

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});
	}

}
