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

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import static com.metreeca.form.Result.error;
import static com.metreeca.form.Result.value;


/**
 * JSON body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class _JSON implements Format<JsonObject> {

	/**
	 * The default MIME type for JSON message bodies.
	 */
	public static final String MIME="application/json";

	/**
	 * The singleton JSON body format.
	 */
	public static final Format<JsonObject> Format=new _JSON();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _JSON() {} // singleton


	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderFormat#asReader} representation, if present; an empty optional, otherwise
	 */
	@Override public Result<JsonObject, Failure> get(final Message<?> message) {
		return message.headers("content-type").contains(MIME) ? message.body(ReaderFormat.asReader).value(source -> {
			try (final Reader reader=source.get()) {

				return value(Json.createReader(reader).readObject());

			} catch ( final JsonParsingException e ) {

				return error(new Failure()
						.status(Response.BadRequest)
						.error(Failure.BodyMalformed)
						.trace((JsonValue)e));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}) : error(new Failure().status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures the {@link _Writer#Format} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted writer.
	 */
	@Override public <T extends Message<T>> T set(final T message, final JsonObject value) {
		return message.header("content-type", MIME)

				.body(_Writer.Format, writer -> Json.createWriter(writer).write(value));
	}

}
