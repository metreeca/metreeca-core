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

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;

import javax.json.*;
import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.core.Context.asset;
import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.JSONFormat.json;
import static com.metreeca.core.formats.OutputFormat.output;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rest.assets.Engine.shape;

/**
 * Model-driven JSON-LD message format.
 */
public final class JSONLDFormat extends Format<Collection<Statement>> {

	/**
	 * The default MIME type for JSON-LD messages ({@value}).
	 */
	public static final String MIME="application/ld+json";


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
	 * Creates a JSON-LD message format.
	 *
	 * @return a new JON-LD message format
	 */
	public static JSONLDFormat jsonld() {
		return new JSONLDFormat();
	}


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
	 * @return a function mapping from an alias to the aliased property name as defined in {@code context}m
	 * defaulting to
	 * the alias if no property name is found
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

	/**
	 * Parses a JSON-LD model.
	 *
	 * <p><strong>Warning</strong> / Parsing is completely driven by the provided shape: embedded {@code @context}
	 * objects are ignored.</p>
	 *
	 * @param reader  the reader the JSON-LD object is to be parsed from
	 * @param base
	 * @param focus
	 * @param shape
	 * @param context
	 *
	 * @return either a parsing exception or the RDF model parsed from {@code reader}
	 *
	 * @throws NullPointerException if {@code reader} is null
	 */
	public static Either<JsonException, Collection<Statement>> jsonld(
			final Reader reader, final String base,
			final Resource focus, final Shape shape, final JsonObject context // !!! get from shape
	) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		return json(reader).fold(Either::Left, json -> {
			try {

				return Right(new JSONLDDecoder(base, context) {}.decode(focus, shape, json));

			} catch ( final JsonException e ) {

				return Left(e);

			}
		});

	}

	/**
	 * Writes a JSON-LD model.
	 *
	 * <p><strong>Warning</strong> / To be fixed: {@code @context} objects generaed from the provided shape are
	 * currently not emitted.</p>
	 *
	 * @param <W>     the type of the {@code writer} the JSON-LD model is to be written to
	 * @param writer  the writer the JSON-LD model is to be written to
	 * @param base
	 * @param focus
	 * @param shape
	 * @param context
	 * @param model   the JSON-LD model to be written
	 *
	 * @return the target {@code writer}
	 *
	 * @throws NullPointerException if either {@code writer} or {@code model} is null or if {@code model} contains
	 *                              null statements
	 */
	public static <W extends Writer> W jsonld(
			final W writer, final String base,
			final Resource focus, final Shape shape, final JsonObject context, // !!! get from shape
			final Collection<Statement> model
	) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null model or model statement");
		}

		return json(writer, new JSONLDEncoder(base, context) {}.encode(focus, shape, model));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONLDFormat() {}

	private final JsonObject context=asset(context());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the JSON-LD {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat} body, if one is available and the {@code message} {@code Content-Type} header is either
	 * missing or  matched by {@link JSONFormat#MIMEPattern}
	 */
	@Override public Either<MessageException, Collection<Statement>> decode(final Message<?> message) {
		return message.header("Content-Type").filter(JSONFormat.MIMEPattern.asPredicate().or(String::isEmpty))

				.map(type -> message.body(input()).flatMap(source -> {

					try (
							final InputStream input=source.get();
							final Reader reader=new InputStreamReader(input, message.charset())
					) {

						return jsonld(reader, message.request().base(),
								iri(message.item()), message.attribute(shape()), context
						).fold(e -> Left(status(BadRequest, e)), Either::Right);

					} catch ( final UnsupportedEncodingException e ) {

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
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> {
					try ( final Writer writer=new OutputStreamWriter(output, message.charset()) ) {

						jsonld(writer, message.request().base(),
								iri(message.item()), message.attribute(shape()), context, value
						);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}
				});
	}

}
