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

package com.metreeca.rest.handlers;


import com.metreeca.form.Form;
import com.metreeca.form.Issue.Level;
import com.metreeca.form.Shape;
import com.metreeca.form.things.Shapes;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.Collection;
import java.util.function.BiFunction;

import javax.json.JsonValue;

import static com.metreeca.form.things.Shapes.container;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.rest.Response.NotImplemented;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.trace;

import static org.eclipse.rdf4j.repository.util.Connections.getStatement;

import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;


/**
 * LDP resource creator.
 *
 * <p>Handles creation requests on the linked data container identified by the request {@linkplain Request#item() focus
 * item}, according to the following operating modes.</p>
 *
 * <p>If the request target is a {@linkplain Request#container() container} and the request includes a resource
 * {@linkplain Message#shape() shape}:</p>
 *
 * <ul>
 *
 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#create}
 * task, {@link Form#convey} mode and {@link Form#detail} view;</li>
 *
 * <li>the request {@link RDFBody RDF body} is expected to contain an RDF description of the resource to be created
 * matched by the redacted shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
 *
 * </ul>
 *
 * <p>Otherwise, If the request target is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>the request {@link RDFBody RDF body} is expected to contain a symmetric concise bounded description of the
 * resource to be created; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, the request RDF body is expected to describe the resource to be created using
 * the request {@linkplain Request#item() focus item} as subject.</p>
 *
 * <p>On successful body validation:</p>
 *
 * <ul>
 *
 * <li>the resource to be created is assigned a unique IRI based on the stem of the {@linkplain Request#stem()
 * stem} of the request IRI and a name generated by either the default {@linkplain #uuid() UUID-based} or a {@linkplain
 * #Creator(BiFunction) custom-provided} slug generator;</li>
 *
 * <li>the request RDF body is rewritten to the assigned IRI and stored into the system {@linkplain Graph#graph()
 * graph} database;</li>
 *
 * <li>the target container identified by the request focus item is connected to the newly created resource as required
 * by the LDP container {@linkplain com.metreeca.form.things.Shapes profile} identified by {@code rdf:type} and LDP
 * properties in the request shape.</li>
 *
 * </ul>
 *
 * <p>On successful resource creation, the IRI of the newly created resource is advertised through the {@code Location}
 * HTTP response header.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Creator extends Delegator {

	/**
	 * Creates a random UUID-based slug generator.
	 *
	 * @return a slug generator returning a new random UUID for each call
	 */
	public static BiFunction<Request, Model, String> uuid() {
		return (request, model) -> randomUUID().toString();
	}

	/**
	 * Creates a sequential auto-incrementing slug generator.
	 *
	 * <p><strong>Warning</strong> / SPARQL doesn't natively support auto-incrementing ids: auto-incrementing slug
	 * calls are partly serialized in the system {@linkplain Graph#graph() graph} database using an internal lock
	 * object; this strategy may fail for distributed containers or external concurrent updates on the SPARQL endpoint,
	 * causing requests to fail with an {@link Response#InternalServerError} or {@link Response#Conflict} status
	 * code.</p>
	 *
	 * @return a slug generator returning an auto-incrementing numeric id unique to the focus item of the request
	 */
	public static BiFunction<Request, Collection<Statement>, String> auto() {
		return new AutoGenerator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/*
	 * Shared lock for taming serialization issues with slug operations (concurrent graph txns may produce conflicts).
	 */
	private final Object lock=new Object();

	private final BiFunction<Request, Model, String> slug;


	private final Engine engine=tool(engine());
	private final Trace trace=tool(trace());


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
	 *             newly created resource; must return a non-null and non-empty value; names clashing with existing
	 *             resources are reported with a {@linkplain Response#Conflict} status code and a structured {@linkplain
	 *             Failure#trace(JsonValue) trace} element
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public Creator(final BiFunction<Request, Model, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		delegate(creator()
				.with(throttler())
				.with(connector())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return new Throttler(Form.create, Form.detail, Shapes::resource);
	}

	private Wrapper connector() {
		return handler -> request -> engine.writing(() -> handler.handle(request));
	}

	private Handler creator() {
		return request -> request.container() ? request.body(rdf()).fold(

				rdf -> {
					synchronized ( lock ) { // attempt to serialize slug operations from multiple txns

						final String name=slug.apply(request, rdf instanceof Model ? (Model)rdf : new LinkedHashModel(rdf));

						if ( name == null ) {
							throw new NullPointerException("null resource name");
						}

						if ( name.isEmpty() ) {
							throw new IllegalArgumentException("empty resource name");
						}

						final IRI container=request.item();
						final IRI resource=iri(request.stem(), name);

						final Shape shape=container(container, request.shape());
						final Collection<Statement> model=rewrite(resource, container, trace.trace(this, rdf));

						// !!! recognize txns failures due to conflicting slugs and report as 409 Conflict

						return request.reply(response -> engine.create(resource, shape, model)

								.map(focus -> focus.assess(Level.Error) // shape violations

										? response.map(new Failure()
										.status(Response.UnprocessableEntity)
										.error(Failure.DataInvalid)
										.trace(focus))

										: response
										.status(Response.Created)
										.header("Location", resource.stringValue())

								)

								.orElseGet(() -> {

									trace.error(this, format("conflicting slug {%s}", resource));

									return response.map(new Failure()
											.status(Response.InternalServerError)
											.cause("see server logs for details")
									);

								})

						);

					}
				},

				request::reply

		) : request.reply(

				new Failure().status(NotImplemented).cause("resource creation not supported")

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> rewrite(final IRI target, final IRI source, final Collection<Statement> model) {
		return model.stream().map(statement -> rewrite(target, source, statement)).collect(toList());
	}

	private Statement rewrite(final IRI target, final IRI source, final Statement statement) {
		return statement(
				rewrite(target, source, statement.getSubject()),
				rewrite(target, source, statement.getPredicate()),
				rewrite(target, source, statement.getObject()),
				rewrite(target, source, statement.getContext())
		);
	}

	private <T extends Value> T rewrite(final T target, final T source, final T value) {
		return source.equals(value) ? target : value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AutoGenerator implements BiFunction<Request, Collection<Statement>, String> {

		private static final IRI Auto=iri("app://rest.metreeca.com/terms#", "auto");


		private final Graph graph=tool(Graph.graph());


		@Override public String apply(final Request request, final Collection<Statement> model) {
			return graph.update(connection -> {

				// !!! custom name pattern
				// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
				// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)

				final IRI stem=iri(request.stem());

				long id=getStatement(connection, stem, Auto, null)
						.map(Statement::getObject)
						.filter(value -> value instanceof Literal)
						.map(value -> {
							try {
								return ((Literal)value).longValue();
							} catch ( final NumberFormatException e ) {
								return 0L;
							}
						})
						.orElse(0L);

				IRI iri;

				do {

					iri=iri(stem.stringValue(), String.valueOf(++id));

				} while ( connection.hasStatement(iri, null, null, true)
						|| connection.hasStatement(null, null, iri, true) );

				connection.remove(stem, Auto, null);
				connection.add(stem, Auto, literal(id));

				return String.valueOf(id);

			});
		}
	}

}
