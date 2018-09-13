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

import com.google.common.collect.ImmutableMap;

import java.io.*;
import java.util.Optional;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;


/**
 * JSON message body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class JSON implements Format<JsonObject> {

	/**
	 * The singleton JSON message body format.
	 */
	public static final Format<JsonObject> Format=new JSON();


	private static final JsonGeneratorFactory generators=Json.createGeneratorFactory(ImmutableMap.of(
			JsonGenerator.PRETTY_PRINTING, true
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSON() {} // singleton


	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link Inbound#Format} representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<JsonObject> get(final Message<?> message) {
		return message.body(Inbound.Format).map(source -> {
			try (final Reader reader=source.reader()) {

				return Json.createReader(reader).readObject();

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures the {@link Outbound#Format} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted {@link Target}.
	 */
	@Override public void set(final Message<?> message, final JsonObject value) {
		message.body(Outbound.Format, target -> {
			try (final Writer writer=target.writer()) {

				generators.createGenerator(writer).write(value).close();

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
