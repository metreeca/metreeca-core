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

package com.metreeca.rest.bodies;

import com.metreeca.rest.*;

import java.io.*;
import java.util.regex.Pattern;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.ReaderBody.reader;
import static com.metreeca.rest.bodies.WriterBody.writer;

import static java.util.Collections.singletonMap;


/**
 * JSON body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class JSONBody implements Body<JsonObject> {

	private static final JSONBody Instance=new JSONBody();


	/**
	 * The default MIME type for JSON message bodies.
	 */
	public static final String MIME="application/json";

	/**
	 * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
	 */
	public static final Pattern MIMEPattern=Pattern.compile("^application/(.*\\+)?json$");


	private static final JsonWriterFactory JsonWriters=Json
			.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));


	/**
	 * Retrieves the JSON body format.
	 *
	 * @return the singleton JSON body format instance
	 */
	public static JSONBody json() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONBody() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderBody} representation, if one is present and the value of the {@code Content-Type} header is equal
	 * to {@value #MIME}; a failure reporting the {@link Response#UnsupportedMediaType} status, otherwise
	 */
	@Override public Result<JsonObject, Failure> get(final Message<?> message) {
		return message.headers("Content-Type").stream().anyMatch(type -> MIMEPattern.matcher(type).matches())
				? message.body(reader())
				.fold(

						source -> {
							try (
									final Reader reader=source.get();
									final JsonReader jsonReader=Json.createReader(reader)
							) {

								return Value(jsonReader.readObject());

							} catch ( final JsonParsingException e ) {

								return Error(new Failure()
										.status(Response.BadRequest)
										.error(Failure.BodyMalformed)
										.cause(e));

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						},

						error -> error.equals(Body.Missing) ? Value(JsonValue.EMPTY_JSON_OBJECT) : Error(error)

				)

				: Error(new Failure().status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures the {@link WriterBody} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}.
	 */
	@Override public <T extends Message<T>> T set(final T message) {
		return message
				.header("content-type", MIME)
				.body(writer(), json().map(json -> target -> {
					try (
							final Writer writer=target.get();
							final JsonWriter jsonWriter=JsonWriters.createWriter(writer)
					) {

						jsonWriter.write(json);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}));
	}

}
