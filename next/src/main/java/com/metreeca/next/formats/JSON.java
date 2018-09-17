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

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;


/**
 * JSON body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class JSON implements Format<JsonObject> {

	/**
	 * The singleton JSON body format.
	 */
	public static final Format<JsonObject> Format=new JSON();

	/**
	 * The default MIME type for JSON message bodies.
	 */
	public static final String MIME="application/json";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSON() {} // singleton


	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link _Reader#Format} representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<JsonObject> get(final Message<?> message) {
		return message.body(_Reader.Format)

				.filter(source -> message.header("content-type").orElse("").equals(MIME))

				.map(source -> {
					try (final Reader reader=source.get()) {

						return Json.createReader(reader).readObject();

					} catch ( final JsonParsingException e ) {

						throw new UnsupportedOperationException("to be implemented"); // !!! tbi

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				});
	}

	/**
	 * Configures the {@link _Writer#Format} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted writer.
	 */
	@Override public void set(final Message<?> message, final JsonObject value) {
		message.header("content-type", MIME)

				.body(_Writer.Format, writer -> {
					Json.createWriter(writer).write(value);
				});
	}

}
