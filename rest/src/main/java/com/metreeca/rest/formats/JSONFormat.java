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

package com.metreeca.rest.formats;

import com.metreeca.form.Result;
import com.metreeca.rest.*;

import java.io.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;

import static com.metreeca.form.Result.error;
import static com.metreeca.form.Result.value;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;


/**
 * JSON body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class JSONFormat implements Format<JsonObject> {

	private static final JSONFormat Instance=new JSONFormat();


	/**
	 * The default MIME type for JSON message bodies.
	 */
	public static final String MIME="application/json";


	public static JSONFormat json() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderFormat} representation, if one is present and the value of the {@code Content-Type} header is equal
	 * to {@value #MIME}; an empty optional, otherwise
	 */
	@Override public Result<JsonObject, Failure> get(final Message<?> message) {
		return message.headers("content-type").contains(MIME) ? message.body(reader()).get().value(source -> {
			try (final Reader reader=source.get()) {

				return value(Json.createReader(reader).readObject());

			} catch ( final JsonParsingException e ) {

				return error(new Failure()
						.status(Response.BadRequest)
						.error(Failure.BodyMalformed)
						.cause(e));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}) : error(new Failure().status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures the {@link WriterFormat} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}.
	 */
	@Override public <T extends Message<T>> T set(final T message) {
		return message
				.header("content-type", MIME)
				.body(writer()).chain(consumer -> message.body(json()).get().value(json -> value(target -> {
					try (final Writer writer=target.get()) {

						Json.createWriter(writer).write(json);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				})));
	}

}
