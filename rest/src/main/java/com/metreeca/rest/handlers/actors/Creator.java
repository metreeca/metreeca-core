/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.*;
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import javax.json.JsonValue;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.UUID.randomUUID;


/**
 * Resource creator.
 *
 * <p>Handles creation requests on linked data resource containers.</p>
 *
 * <p>On successful resource creation, the IRI of the newly created resource is advertised through the {@code Location}
 * HTTP response header.</p>
 *
 * <dl>
 *
 * <dt>Request {@link ShapeFormat} body {optional}</dt>
 *
 * <dd>An optional linked data shape driving the creation process.</dd>
 *
 * <dt>Request shape-driven {@link RDFFormat} body</dt>
 *
 * <dd>The RDF content to be assigned to the newly created resource; must describe the new resource using
 * the request {@linkplain Request#item() focus item} as subject.</dd>
 *
 * <dd>If the request includes a {@link ShapeFormat} body, it is redacted taking into account the request user
 * {@linkplain Request#roles() roles},  {@link Form#create} task,  {@link Form#verify} mode and {@link Form#detail} view
 * and used to validate the request RDF body; validation errors are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</dd>
 *
 * <dd>On successful body validation, the newly created resource is assigned a unique IRI based on the stem of the
 * request {@linkplain Request#item() focus item} and a name provided by either the default {@linkplain #uuid()
 * UUID-based} or a {@linkplain #slug(BiFunction) custom-provided} slug generator.</dd>
 *
 * <dt>Request shapeless {@link RDFFormat} body</dt>
 *
 * <dd><strong>Warning</strong> / Shapeless resource creation is not yet supported and is reported with a {@linkplain
 * Response#NotImplemented} HTTP status code.</dd>
 *
 * </dl>
 *
 * <p>Regardless of the operating mode, resource description content is stored into the system {@linkplain
 * Graph#Factory graph} database.</p>
 */
public final class Creator extends Actor<Creator> {

	/*
	 * Shared lock for serializing slug operations (concurrent graph txns may produce conflicting results).
	 */
	private static final Supplier<Object> LockFactory=Object::new; // !!! ;( breaks in distributed containers


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);
	private final Object lock=tool(LockFactory);


	private BiFunction<Request, Collection<Statement>, String> slug=uuid();


	public Creator() {
		delegate(handler(Form.create, Form.detail, (request, shape) -> request.body(rdf()).map(
				model -> empty(shape) ? direct(request, model) : driven(request, model, shape),
				request::reply
		)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Creator pre(final BiFunction<Request, Model, Model> filter) { return super.pre(filter); }

	@Override public Creator update(final String update) { return super.update(update); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the slug generation function.
	 *
	 * @param slug a function mapping from the creation request and its RDF body to the name to be assigned to the newly
	 *             created resource; must return a non-null and non-empty value
	 *
	 * @return this creator
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public Creator slug(final BiFunction<Request, Collection<Statement>, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		synchronized ( lock ) {

			this.slug=slug;

		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request, final Collection<Statement> model) {
		return request.reply(response -> response.map(new Failure<>()
				.status(Response.NotImplemented)
				.cause("shapeless resource creation not supported"))
		);
	}

	private Responder driven(final Request request, final Collection<Statement> model, final Shape shape) {
		return request.reply(response -> graph.update(connection -> {

			synchronized ( lock ) { // attempt to serialize slug handling from multiple txns

				final String slug=this.slug.apply(request, model);

				if ( slug == null ) {
					throw new NullPointerException("null slug");
				}

				if ( slug.isEmpty() ) {
					throw new IllegalArgumentException("empty slug");
				}

				final IRI source=request.item();
				final IRI target=iri(request.stem(), slug);

				model.addAll(shape // add implied statements
						.accept(mode(Form.verify))
						.accept(new Outliner(source))
				);

				final Report report=new SPARQLEngine(connection).create(target, shape, trace(rewrite(
						model, source, target
				)));

				if ( report.assess(Issue.Level.Error) ) { // shape violations

					connection.rollback();

					// !!! rewrite report value references to original target iri
					// !!! rewrite references to external base IRI
					// !!! factor with Updater

					return response.map(new Failure<>()
							.status(Response.UnprocessableEntity)
							.error("data-invalid")
							.trace(report(report)));

				} else { // valid data

					connection.commit();

					return response
							.status(Response.Created)
							.header("Location", target.stringValue());

				}
			}

		}));
	}


	//// Slug Functions ////////////////////////////////////////////////////////////////////////////////////////////////

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

		final Shape matcher=shape
				.accept(task(Form.relate))
				.accept(view(Form.digest))
				.accept(role(Form.any));

		return (request, model) -> tool(Graph.Factory).query(connection -> {

			// !!! custom iri stem/pattern
			// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
			// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)
			// !!! 409 Conflict https://tools.ietf.org/html/rfc7231#section-6.5.8 for clashing slug?

			final String stem=request.stem();
			final Collection<Statement> edges=new SPARQLEngine(connection)
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

}
