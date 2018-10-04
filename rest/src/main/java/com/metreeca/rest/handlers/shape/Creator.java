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
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.UUID.randomUUID;


public final class Creator extends Actor<Creator> {

	/*
	 * Shared lock for serializing slug operations (concurrent graph txns may produce conflicting results).
	 */
	private static final Supplier<Object> LockFactory=Object::new; // !!! ;( breaks in distributed containers


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);
	private final Object lock=tool(LockFactory);


	private BiFunction<Request, Collection<Statement>, String> slug=uuid();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Creator slug(final BiFunction<Request, Collection<Statement>, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		this.slug=slug;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return request.query().isEmpty() ? request.body(rdf()).map(

				model -> handler(Form.create, Form.detail, shape ->

						empty(shape) ? direct(request, model) : driven(request, model, shape)

				).handle(request),

				request::reply

		) : request.reply(new Failure<>().status(Response.BadRequest).cause("unexpected query parameters"));
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

				final IRI source=request.item();
				final IRI target=iri(request.stem(), slug.apply(request, model));

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
