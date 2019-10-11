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

import com.metreeca.rest.*;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.json.*;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonParsingException;

import static com.metreeca.rest.Failure.malformed;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;

import static java.util.Collections.singletonMap;


/**
 * JSON body format.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class JSONFormat implements Format<JsonObject> {

	/**
	 * The default MIME type for JSON message bodies.
	 */
	public static final String MIME="application/json";

	/**
	 * A pattern matching JSON-based MIME types, for instance {@code application/ld+json}.
	 */
	public static final Pattern MIMEPattern=Pattern.compile("^application/(.*\\+)?json$");


	private static final JSONFormat Instance=new JSONFormat();

	private static final JsonWriterFactory JsonWriters=Json
			.createWriterFactory(singletonMap(JsonGenerator.PRETTY_PRINTING, true));


	/**
	 * Retrieves the JSON body format.
	 *
	 * @return the singleton JSON body format instance
	 */
	public static JSONFormat json() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * The JSON-LD {@value} keyword.
	 */
	public static final String id="@id";

	/**
	 * The JSON-LD {@value} keyword.
	 */
	public static final String value="@value";

	/**
	 * The JSON-LD {@value} keyword.
	 */
	public static final String type="@type";

	/**
	 * The JSON-LD {@value} keyword.
	 */
	public static final String language="@language";


	/**
	 * Retrieves the default JSON-LD context factory.
	 *
	 * @return the default JSON-LD context factory, which returns an amepty context
	 */
	public static Supplier<JsonObject> context() {
		return () -> JsonValue.EMPTY_JSON_OBJECT;
	}


	/**
	 * Aliases JSON-LD property names.
	 *
	 * @param context the JSON-LD context property names are to be aliased against
	 *
	 * @return a function mapping from a property name to its alias as defined in {@code context}, defaulting to the
	 * property name if no alias is found
	 *
	 * @throws NullPointerException if {@code context} is null
	 */
	public static Function<String, String> aliaser(final JsonObject context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		final Map<String, String> aliases=new HashMap<>();

		context.forEach((alias, name) -> {
			if ( !alias.startsWith("@") && name instanceof JsonString ) {
				aliases.put(((JsonString)name).getString(), alias);
			}
		});

		return name -> aliases.getOrDefault(name, name);
	}

	/**
	 * Resolves JSON-LD property names.
	 *
	 * @param context the JSON-LD context property names are to be resolved against
	 *
	 * @return a function mapping from an alias to the aliased property name as defined in {@code context}m defaulting
	 * to the alias if no property name is found
	 *
	 * @throws NullPointerException if {@code context} is null
	 */
	public static Function<String, String> resolver(final JsonObject context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		return alias -> context.getString(alias, alias);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional JSON body representation of {@code message}, as retrieved from the reader supplied by its
	 * {@link ReaderFormat} representation, if one is present and the value of the {@code Content-Type} header is equal
	 * to {@value #MIME}; a failure reporting the {@link Response#UnsupportedMediaType} status, otherwise
	 */
	@Override public Result<JsonObject, Failure> get(final Message<?> message) {
		return message.headers("Content-Type").stream().anyMatch(type -> MIMEPattern.matcher(type).matches())

				? message.body(reader()).process(source -> {

			try (
					final Reader reader=source.get();
					final JsonReader jsonReader=Json.createReader(reader)
			) {

				return Value(jsonReader.readObject());

			} catch ( final JsonParsingException e ) {

				return Error(malformed(e));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		})

				: Error(new Failure()
				.status(Response.UnsupportedMediaType)
				.notes("missing JSON body")
		);
	}

	/**
	 * Configures the {@link WriterFormat} representation of {@code message} to write the JSON {@code value} to the
	 * writer supplied by the accepted writer and sets the {@code Content-Type} header to {@value #MIME}.
	 */
	@Override public <M extends Message<M>> M set(final M message, final JsonObject value) {
		return message
				.header("Content-Type", MIME)
				.body(writer(), target -> {
					try (
							final Writer writer=target.get();
							final JsonWriter jsonWriter=JsonWriters.createWriter(writer)
					) {

						jsonWriter.write(value);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				});
	}

}
