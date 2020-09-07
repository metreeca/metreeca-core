/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.core.formats;

import com.metreeca.core.*;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;
import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;
import static java.util.Collections.singletonMap;


/**
 * JSON message format.
 *
 * @see <a href="https://javaee.github.io/jsonp/">JSR 374 - Java API for JSON Processing</a>
 */
public final class JSONFormat extends Format<JsonObject> {

	/**
	 * The default MIME type for JSON messages ({@value}).
	 */
	public static final String MIME="application/json";

	/**
	 * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
	 */
	public static final Pattern MIMEPattern=Pattern.compile("(?i)^(text/json|application/(.*\\+)?json)(?:\\s*;.*)?$");


	private static final JsonWriterFactory JsonWriters=Json
			.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));


	/**
	 * Creates a JSON message format.
	 *
	 * @return a new JSON message format
	 */
	public static JSONFormat json() {
		return new JSONFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a JSON object.
	 *
	 * @param reader the reader the JSON object is to be parsed from
	 *
	 * @return either a parsing exception or the JSON object parsed from {@code reader}
	 *
	 * @throws NullPointerException if {@code reader} is null
	 */
	public static Either<JsonParsingException, JsonObject> json(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try ( final JsonReader jsonReader=Json.createReader(reader) ) {

			return Right(jsonReader.readObject());

		} catch ( final JsonParsingException e ) {

			return Left(e);

		}
	}

	/**
	 * Writes a JSON object.
	 *
	 * @param <W>    the type of the {@code writer} the JSON object is to be written to
	 * @param writer the writer the JSON object is to be written to
	 * @param object the JSON object to be written
	 *
	 * @return the target {@code writer}
	 *
	 * @throws NullPointerException if either {@code writer} or {@code object} is null
	 */
	public static <W extends Writer> W json(final W writer, final JsonObject object) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		try ( final JsonWriter jsonWriter=JsonWriters.createWriter(writer) ) {

			jsonWriter.writeObject(object);

			return writer;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the JSON {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available and the {@code message} {@code Content-Type} header is either missing or  matched by
	 * {@link #MIMEPattern}
	 */
	@Override public Either<MessageException, JsonObject> decode(final Message<?> message) {
		return message.header("Content-Type").filter(MIMEPattern.asPredicate().or(String::isEmpty))

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset())
					) {

						return json(reader).fold(e -> Left(status(BadRequest, e)), Either::Right);

					} catch ( final UnsupportedEncodingException e ) {

						return Left(status(BadRequest, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no JSON body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes
	 * the JSON {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body
	 */
	@Override public <M extends Message<M>> M encode(final M message, final JsonObject value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {
					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						json(writer, value);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
