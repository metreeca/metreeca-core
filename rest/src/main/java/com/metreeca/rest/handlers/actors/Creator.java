/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Issue.Level;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.rest.*;
import com.metreeca.rest.engines._SPARQLEngine;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import javax.json.JsonValue;

import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;


/**
 * LDP resource creator.
 *
 * <p>Handles creation requests on the stored linked data basic resource container identified by the request
 * {@linkplain Request#item() focus item}.</p>
 *
 * <p>If the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#create}
 * task, {@link Form#verify} mode and {@link Form#detail} view;</li>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain an RDF description of the resource to be created
 * matched by the redacted shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain a symmetric concise bounded description of the
 * resource to be created; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
 *
 * </ul>
 *
 * <p>The request RDF body must describe the resource to be created using the request {@linkplain Request#item() focus
 * item} as subject.</p>
 *
 * <p>On successful body validation:</p>
 *
 * <ul>
 *
 * <li>the resource to be created is assigned a unique IRI based on the stem of the request {@linkplain Request#item()
 * focus item} and a name provided by either the default {@linkplain #uuid() UUID-based} or a {@linkplain
 * #Creator(BiFunction) custom-provided} slug generator;</li>
 *
 * <li>the request RDF body is rewritten to the assigned IRI and stored into the graph database;</li>
 *
 * <li>the target basic container identified by the request focus item is connected to the newly created resource using
 * the {@link LDP#CONTAINS ldp:contains} property.</li>
 *
 * </ul>
 *
 * <p>On successful resource creation, the IRI of the newly created resource is advertised through the {@code Location}
 * HTTP response header.</p>
 *
 * <p>Regardless of the operating mode, RDF data is inserted into the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Creator extends Actor {

	/**
	 * Generates a random UUID-based slug.
	 *
	 * @return a random UUID-based slug
	 */
	public static BiFunction<Request, Collection<Statement>, String> uuid() {
		return (request, model) -> randomUUID().toString();
	}

	/**
	 * Generates a sequential auto-incrementing slug.
	 *
	 * <p><strong>Warning</strong> / Missing native SPARQL support for auto-incrementing ids, auto-incrementing slug
	 * calls are serialized in the system {@linkplain Graph#Factory graph} database using an internal lock object: this
	 * strategy may fail for distributed containers or concurrent updates on teh SPARQL endpoint not managed by the
	 * framework.</p>
	 *
	 * @param shape a shape matching all the items in the auto-increment collection
	 *
	 * @return an auto-incrementing numeric slug uniquely identifying a new resource in the collection matched by {@code
	 * shape}
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static BiFunction<Request, Collection<Statement>, String> auto(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final Shape matcher=shape.map(new Redactor(map(
				entry(Form.task, set(Form.relate)),
				entry(Form.view, set(Form.digest)),
				entry(Form.role, set(Form.any))
		))).map(new Optimizer());

		return (request, model) -> tool(Graph.Factory).query(connection -> { // !!! tool() breaks at runtime

			// !!! don't reuse identifiers

			// !!! custom iri stem/pattern
			// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
			// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)
			// !!! 409 Conflict https://tools.ietf.org/html/rfc7231#section-6.5.8 for clashing slug?

			final String stem=request.stem();
			final Collection<Statement> edges=new _SPARQLEngine(connection)
					.browse(matcher)
					.values()
					.stream()
					.findFirst()
					.orElseGet(Collections::emptySet);

			long count=edges.size();
			IRI iri;

			do {

				iri=iri(stem, String.valueOf(++count));

			} while ( connection.hasStatement(iri, null, null, true)
					|| connection.hasStatement(null, null, iri, true) );

			return String.valueOf(count);

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Trace trace=tool(Trace.Factory);

	/*
	 * Shared lock for taming serialization issues with slug operations (concurrent graph txns may produce conflicts).
	 */
	private final Object lock=new Object();


	private final BiFunction<Request, Collection<Statement>, String> slug;


	/**
	 * Creates a resource creator with a {@linkplain #uuid() UUID} slug generator.
	 */
	public Creator() {
		this(uuid());
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param slug a function mapping from the creation request and its RDF payload to the name to be assigned to the
	 *             newly created resource; must return a non-null and non-empty value
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public Creator(final BiFunction<Request, Collection<Statement>, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		delegate(creator().with(throttler()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return new Throttler(Form.create, Form.detail, resource());
	}

	private Handler creator() {
		return request -> request.body(rdf()).fold(

				model -> {
					synchronized ( lock ) { // attempt to serialize slug operations from multiple txns

						final String name=slug.apply(request, model);

						if ( name == null ) {
							throw new NullPointerException("null resource name");
						}

						if ( name.isEmpty() ) {
							throw new IllegalArgumentException("empty resource name");
						}

						final IRI source=request.item();
						final IRI target=iri(request.stem(), name);

						return request.reply(response -> engine(request.shape())

								// !!! recognize txns failures due to conflicting slugs and report as 409 Conflict

								.create(source, target, trace.debug(this, rewrite(source, target, model)))

								.map(focus -> focus.assess(Level.Error) // shape violations

										? response.map(new Failure()
										.status(Response.UnprocessableEntity)
										.error(Failure.DataInvalid)
										.trace(focus))

										: response
										.status(Response.Created)
										.header("Location", target.stringValue())

								)

								.orElseGet(() -> {

									trace.error(this, format("conflicting slug {%s}", target));

									return response.map(new Failure()
											.status(Response.InternalServerError)
											.cause("see server logs for details")
									);

								})

						);

					}
				},

				request::reply

		);
	}

}
