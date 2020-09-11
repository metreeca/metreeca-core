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

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Meta;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.*;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.ParserConfig;
import org.eclipse.rdf4j.rio.RioConfig;

import javax.json.JsonException;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;

import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

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
		return new JSONLDFormat(options -> {});
	}

	/**
	 * Creates a customized JSON-LD message format.
	 *
	 * @param customizer the JSON-LD parser/writer customizer; takes as argument a customizable RIO configuration
	 *
	 * @return a new customized JSON-LD message format
	 */
	public static JSONLDFormat rdf(final Consumer<RioConfig> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		return new JSONLDFormat(customizer);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a wrapper managing global JSON-LD keyword mappings.
	 *
	 * @param mappings an array of annotations with a JSON-LD keyword as label and an alias as value
	 *
	 * @return a wrapper extending the {@linkplain Shape#shape() shape} attribute of incoming requests and outgoing
	 * responses with the provided JSON-LD keyword {@code mappings}
	 *
	 * @throws NullPointerException if {@code mapping} is null or contains null values
	 */
	public static Wrapper keywords(final Meta... mappings) {

		if ( mappings == null || Arrays.stream(mappings).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null mappings");
		}

		if ( Arrays.stream(mappings).anyMatch(meta -> !meta.label().toString().startsWith("@")) ) {
			throw new IllegalArgumentException("illegal mapping keywords");
		}

		final Shape keywords=and(mappings);

		return handler -> request -> handler

				.handle(request.attribute(shape(), and(request.attribute(shape()), keywords)))

				.map(response -> response.attribute(shape(), and(response.attribute(shape()), keywords)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Consumer<RioConfig> customizer;

	private JSONLDFormat(final Consumer<RioConfig> customizer) {
		this.customizer=customizer;
	}


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

						final ParserConfig options=new ParserConfig();

						customizer.accept(options);

						return Right(new JSONLDDecoder(

								iri(message.item()), message.attribute(shape()), options

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

						final ParserConfig options=new ParserConfig();

						customizer.accept(options);

						new JSONLDEncoder(

								iri(message.item()), message.attribute(shape()), options

						).encode(writer, value);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
