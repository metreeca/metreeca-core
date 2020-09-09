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

package com.metreeca.rdf.formats;

import com.metreeca.core.*;
import com.metreeca.core.formats.*;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Meta;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.ParserConfig;

import javax.json.JsonException;
import java.io.*;
import java.util.*;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;
import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf.Values.iri;

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
	 * Creates a wrapper for global JSON-LD keyword mappings.
	 *
	 * @param mappings an array of annotations with a JSON-LD keyword as label and an alias as value
	 *
	 * @return a wrapper extending the {@linkplain Shape#shape() shape} attribute of incoming requests and outgoing
	 * responses with the provided keyword {@code mappings}
	 *
	 * @throws NullPointerException if {@code mapping} is null or contains null values
	 */
	public static Wrapper keywords(final Meta... mappings) {

		if ( mappings == null || Arrays.stream(mappings).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null mappings");
		}

		if ( Arrays.stream(mappings).anyMatch(meta -> !meta.getLabel().toString().startsWith("@")) ) {
			throw new IllegalArgumentException("illegal mapping keywords");
		}

		final Shape keywords=and(mappings);

		return handler -> request -> handler

				.handle(request.attribute(shape(), and(request.attribute(shape()), keywords)))

				.map(response -> response.attribute(shape(), and(response.attribute(shape()), keywords)));
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

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset())
					) {

						return Right(new JSONLDDecoder(

								message.request().base(),
								iri(message.item()),
								message.attribute(shape()),
								new ParserConfig()

						).decode(reader));

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
	 * {@link OutputFormat} body
	 *
	 * <p><strong>Warning</strong> / {@code @context} objects generated from the {@code message}
	 * {@linkplain Shape#shape() shape attribute} are currently not embedded.</p>
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {
					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						new JSONLDEncoder(

								iri(message.item()),
								message.attribute(shape())

						).encode(writer, value);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
