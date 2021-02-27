/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.formats;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Or;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.json.*;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.lang;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;

/**
 * Model-driven JSON-LD message format.
 */
public final class JSONLDFormat extends Format<Collection<Statement>> {

	/**
	 * The default MIME type for JSON-LD messages ({@value}).
	 */
	public static final String MIME="application/ld+json";


	/**
	 * Creates a JSON-LD message format.
	 *
	 * @return a new JON-LD message format
	 */
	public static JSONLDFormat jsonld() {
		return new JSONLDFormat();
	}


	/**
	 * Retrieves the default JSON-LD shape asset factory.
	 *
	 * @return the default shape factory, which returns an {@linkplain Or#or() empty disjunction}, that is a shape
	 * the always fails to validate
	 */
	public static Supplier<Shape> shape() {
		return Or::or;
	}

	/**
	 * Retrieves the default JSON-LD keywords asset factory.
	 *
	 * <p>The keywords asset maps JSON-LD {@code @keywords} to user-defined aliases.</p>
	 *
	 * @return the default keywords factory, which returns an empty map
	 */
	public static Supplier<Map<String, String>> keywords() {
		return Collections::emptyMap;
	}


	private static final JsonWriterFactory JsonWriters=Json.createWriterFactory(singletonMap(PRETTY_PRINTING, true));


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes a shape-based query.
	 *
	 * @param focus the target IRI fr the decoding process; relative IRIs will be resolved against it
	 * @param shape the base shape for the decoded query
	 * @param query the query to be decoded
	 *
	 * @return either a message exception reporting a decoding issue or the decoded query
	 *
	 * @throws NullPointerException if any parameter is null
	 */
	public static Either<MessageException, Query> query(final IRI focus, final Shape shape, final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		try {

			return Right(new JSONLDFilter(focus, shape, asset(keywords())).parse(query));

		} catch ( final JsonException e ) {

			return Left(status(BadRequest, e));

		} catch ( final NoSuchElementException e ) {

			return Left(status(UnprocessableEntity, e));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONLDFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the JSON-LD {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat} body, if one is available and the {@code message} {@code Content-Type} header is either
	 * missing or  matched by {@link JSONFormat#MIMEPattern}
	 *
	 * <p><strong>Warning</strong> / Decoding is completely driven by the {@code message}
	 * {@linkplain JSONLDFormat#shape() shape attribute}: embedded {@code @context} objects are ignored.</p>
	 */
	@Override public Either<MessageException, Collection<Statement>> decode(final Message<?> message) {
		return message

				.header("Content-Type")

				.filter(JSONFormat.MIMEPattern.asPredicate().or(String::isEmpty))

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset());
							final JsonReader jsonReader=Json.createReader(reader)
					) {

						return new JSONLDDecoder(

								iri(message.item()),
								message.attribute(shape()),
								asset(keywords())

						)

								.decode(jsonReader.readObject())

								.fold(trace -> Left(status(UnprocessableEntity, trace.toJSON())), Either::Right);

					} catch ( final UnsupportedEncodingException|JsonException e ) {

						return Left(status(BadRequest, e));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no JSON-LD body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value JSONFormat#MIME}, unless already defined, and
	 * encodes the JSON-LD model {@code value} into the output stream accepted by the {@code message}
	 * {@link OutputFormat} body.
	 *
	 * <p>If the originating {@code message} {@linkplain Message#request() request} includes an {@code Accept-Language}
	 * header, only matching tagged literals from {@code value} are included in the response body.</p>
	 *
	 * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
	 * {@linkplain JSONLDFormat#shape() shape attribute} are embedded only if {@code Content-Type} is {@value MIME}.</p>
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

		final String mime=message

				.header("Content-Type") // content-type explicitly defined by handler

				.orElseGet(() -> mimes(message.request().header("Accept").orElse("")).stream()

						// application/ld+json or application/json accepted?

						.filter(type -> type.equals(MIME) || type.equals(JSONFormat.MIME)).findFirst()

						// default to application/json

						.orElse(JSONFormat.MIME)

				);

		final List<String> langs=message.request()

				.header("Accept-Language")

				.map((Function<String, List<String>>)Format::langs)
				.orElse(emptyList());

		return message

				.header("~Content-Type", mime)

				.body(output(), output -> {
					try (
							final Writer writer=new OutputStreamWriter(output, message.charset());
							final JsonWriter jsonWriter=JsonWriters.createWriter(writer)
					) {

						jsonWriter.writeObject(new JSONLDEncoder(

								iri(message.item()),
								message.attribute(shape()),
								asset(keywords()),
								mime.equals(MIME) // include context objects for application/ld+json?

						).encode(langs.isEmpty() || langs.contains("*") ? value : value.stream().filter(statement -> {

							// retain only tagged literals with an accepted language

							final String lang=lang(statement.getObject());

							return lang == null || langs.contains(lang);

						}).collect(toList())));

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
