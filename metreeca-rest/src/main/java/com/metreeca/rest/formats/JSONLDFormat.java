/*
 * Copyright © 2013-2020 Metreeca srl
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

import com.metreeca.json.*;
import com.metreeca.json.shapes.Or;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import javax.json.JsonException;
import javax.json.JsonObject;
import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.json.Values.iri;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.JSONFormat.json;

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

			return Right(new JSONLDParser(focus, shape, asset(keywords())).parse(query));

		} catch ( final JsonException e ) {

			return Left(status(BadRequest, e));

		} catch ( final NoSuchElementException e ) {

			return Left(status(UnprocessableEntity, e));

		}
	}


	/**
	 * Verifies the well-formedness of a leniently compacted/framed JSON-LD description.
	 *
	 * @param focus the IRI of the resource described by {@code json}
	 * @param shape the expected description shape
	 * @param json  the JSON-LD description to be scanned
	 *
	 * @return either a shape validation trace reporting well-formedness issues or the well-formed {@code json}
	 * description
	 *
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	public static Either<Trace, JsonObject> scan(final IRI focus, final Shape shape, final JsonObject json) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		return new JSONLDScanner(focus, shape, asset(keywords())).validate(json);
	}

	/**
	 * Removes out of envelope fields from a strictly compacted/framed JSON-LD description.
	 *
	 * @param focus the IRI of the resource described by {@code json}
	 * @param shape the expected description shape
	 * @param json  the JSON-LD description to be trimmed
	 *
	 * @return a subset of the {@code json} description trimmed according to {@code shape}
	 *
	 * @throws NullPointerException if any parameter is {@code null}
	 */
	public static JsonObject trim(final IRI focus, final Shape shape, final JsonObject json) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		return new JSONLDTrimmer(focus, shape, asset(keywords())).trim(json).asJsonObject();
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
	 * {@linkplain JSONLDFormat#shape()
	 * shape attribute}: embedded {@code @context} objects are ignored.</p>
	 */
	@Override public Either<MessageException, Collection<Statement>> decode(final Message<?> message) {
		return message.header("Content-Type").filter(JSONFormat.MIMEPattern.asPredicate().or(String::isEmpty))

				.map(type -> message.body(json()).flatMap(json -> { // parse possibly validated json

					try {

						return Right(new JSONLDDecoder(

								iri(message.item()),
								message.attribute(shape()),
								asset(keywords())

						).decode(json));

					} catch ( final JsonException e ) {

						return Left(status(BadRequest, e));

					}

				}))

				.orElseGet(() -> Left(status(UnsupportedMediaType, "no JSON-LD body")));
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value JSONFormat#MIME}, unless already defined, and
	 * encodes the JSON-LD model {@code value} into the output stream accepted by the {@code message}
	 * {@link OutputFormat} body
	 *
	 * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
	 * {@linkplain JSONLDFormat#shape() shape attribute} are currently not embedded.</p>
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

		final String mime=message

				// content-type explicitly defined by the handler

				.header("Content-Type")

				.orElseGet(() -> types(message.request().header("Accept").orElse("")).stream()

						// application/ld+json or application/json accepted?

						.filter(type -> type.equals(MIME) || type.equals(JSONFormat.MIME))
						.findFirst()

						// default to application/json

						.orElse(JSONFormat.MIME)

				);

		return message

				.header("~Content-Type", mime)

				.body(json(), new JSONLDEncoder( // make json available for trimming

						iri(message.item()),
						message.attribute(shape()),
						asset(keywords()),
						mime.equals(MIME) // include context objects for application/ld+json

				).encode(value));
	}

}
