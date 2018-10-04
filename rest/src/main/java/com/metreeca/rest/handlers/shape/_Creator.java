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

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.*;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Sets.intersection;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.handlers.shape.Actor.link;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.disjoint;
import static java.util.UUID.randomUUID;


public final class _Creator implements Handler {

	/*
	 * Shared lock for serializing slug operations (concurrent graph txns may produce conflicting results).
	 */
	private static final Supplier<Object> LockFactory=Object::new; // !!! ;( breaks in distributed containers


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);
	private final Object lock=tool(LockFactory);


	private BiFunction<Request, Collection<Statement>, String> slug=uuid();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public _Creator slug(final BiFunction<Request, Collection<Statement>, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Value> roles=new HashSet<>(); // !!!

	private Handler handler(final IRI task, final IRI view, final Function<Shape, Responder> action) {

		return request -> request.body(ShapeFormat.shape()).map(

				shape -> {  // !!! look for ldp:contains sub-shapes?

					final Shape redacted=shape
							.accept(task(task))
							.accept(view(view));

					final Shape authorized=redacted
							.accept(role(roles.isEmpty() ? request.roles() : intersection(roles, request.roles())));

					return empty(redacted) ? forbidden(request)
							: empty(authorized) ? refused(request)
							: action.apply(authorized);

				},

				error -> {

					final boolean refused=!roles.isEmpty() && disjoint(roles, request.roles());

					return refused ? refused(request)
							: action.apply(and());

				}

		).map(response -> {

			if ( response.request().safe() && response.success() ) {
				response.headers("+Vary", "Accept", "Prefer"); // !!! move to implementations
			}

			return response.headers("Link",
					link(LDP.RDF_SOURCE, "type"),
					link(LDP.RESOURCE, "type")
			);

		});
	}

	@Override public Responder handle(final Request request) {
		return request.query().isEmpty() ? request.body(rdf()).map(

				model -> handler(Form.create, Form.detail, shape -> graph.update(connection -> {

					synchronized ( lock ) { // attempt to serialize slug generation from multiple txns

						final String name=slug.apply(request, model);

						final IRI iri=iri(request.stem(), name); // assign an IRI to the resource to be created
						final Collection<Statement> rewritten=rewrite(model, request.item(), iri); // rewrite to IRI

						final Report report=new SPARQLEngine(connection).create(iri, shape, /* !!! trace */(rewritten));

						// shape violations
						// !!! rewrite report value references to original target iri
						// !!! rewrite references to external base IRI
						// valid data
						return request.reply(response -> report.assess(Issue.Level.Error)
										? response.map(new Failure<>()
										.status(Response.UnprocessableEntity)
										.error("data-invalid")
								// !!! .cause(report(report))
								) : response
										.status(Response.Created)
										.header("Location", iri.stringValue())
						);
					}

				})).handle(request),

				request::reply

		) : request.reply(new Failure<>().status(Response.BadRequest).cause("unexpected query parameters"));
	}


	//// Slug Functions ////////////////////////////////////////////////////////////////////////////////////////////////

	public static BiFunction<Request, Collection<Statement>, String> uuid() {
		return (request, model) -> randomUUID().toString();
	}

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
			// !!! support UUID hint
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
