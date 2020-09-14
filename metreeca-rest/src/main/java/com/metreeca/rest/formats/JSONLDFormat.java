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

package com.metreeca.rest.formats;

import com.metreeca.json.*;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import javax.json.JsonException;
import javax.json.JsonObject;
import java.util.Collection;
import java.util.NoSuchElementException;

import static com.metreeca.json.Shape.expanded;
import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Guard.*;
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


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes a shape-based query.
	 *
	 * @param query the query to be decoded
	 * @param focus the target IRI fr the decoding process; relative IRIs will be resolved against it
	 * @param shape the base shape for the decoded query
	 *
	 * @return either a message exception reporting a decoding issue or the decoded query
	 *
	 * @throws NullPointerException if any parameter is null
	 */
	public static Either<MessageException, Query> query(final String query, final IRI focus, final Shape shape) {

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


	public static Either<Trace, Collection<Statement>> validate(
			final IRI focus, final Shape shape, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return new JSONLDValidator().validate(focus, shape, model);
	}

	public static JsonObject trim(final IRI focus, final Shape shape, final JsonObject object) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		return new JSONTrimmer(asset(keywords())).trim(focus, shape, object).asJsonObject();
	}


	static Shape driver(final Shape shape) { // !!! caching
		return expanded(shape.redact( // add inferred constraints to drive json shorthands
				retain(Role),
				retain(Task),
				retain(Area),
				retain(Mode, Convey) // remove internal filtering shapes
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONLDFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the JSON-LD {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat} body, if one is available and the {@code message} {@code Content-Type} header is either
	 * missing or  matched by {@link JSONFormat#MIMEPattern}
	 *
	 * <p><strong>Warning</strong> / Decoding is completely driven by the {@code message} {@linkplain Shape#shape()
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
	 * {@linkplain Shape#shape() shape attribute} are currently not embedded.</p>
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {
		return message

				.header("~Content-Type", MIME)

				.body(json(), new JSONLDEncoder( // make json available for trimming

						iri(message.item()),
						message.attribute(shape()),
						asset(keywords())

				).encode(value));
	}

}
