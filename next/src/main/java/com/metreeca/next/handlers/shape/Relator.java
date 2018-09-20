/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.next.*;
import com.metreeca.next.formats._Failure;
import com.metreeca.next.formats._RDF;
import com.metreeca.next.formats._Shape;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.sparql.SPARQLEngine.contains;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.next.Handler.forbidden;
import static com.metreeca.next.Handler.unauthorized;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;


/**
 * Resource relator.
 *
 * <p>Handles retrieval requests on linked data resources</p>
 *
 * <dl>
 *
 * <dt>Request Payload</dt>
 * <dd>
 * <dl>
 *
 * <dt>{@link _Shape} {optional}</dt>
 * <dd>An optional linked data shape driving the retrieval process.</dd>
 *
 * </dl>
 * </dd>
 *
 * <dt>Response Payload</dt>
 * <dd>
 * <dl>
 *
 * <dt>{@link _Shape} {optional}</dt>
 * <dd>If the request includes a shape payload, the response includes the derived shape actually used in the resource
 * retrieval process, redacted according to request user roles, retrieval task, filtering mode and detail view.</dd>
 *
 * <dt>{@link _RDF}</dt>
 *
 * <dd>If the request includes a {@link _Shape} body representation, the response includes the {@linkplain _RDF RDF
 * description} of the request {@linkplain Request#item() focus item}, as defined by the associated linked data
 * {@linkplain Shape shape} redacted taking into account the user {@linkplain Request#roles() roles} of the
 * request.</dd>
 *
 * <dd>Otherwise, the  response includes the symmetric concise bounded description of the request focus item, extended
 * with {@code rdfs:label/comment} annotations for all referenced IRIs.</dd>
 *
 * </dl>
 * </dd>
 *
 * </dl>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Relator implements Handler {

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//* @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>

	//	.headers("Link",
	//		"<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"",
	//		"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""
	//)

	//if ( status == Response.OK ) {
	//	response.header("Vary", "Accept", "Prefer", "");
	//}



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	@Override public Responder handle(final Request request) {
		return request.body(_Shape.Format).map(
				shape -> {

					final Shape redacted=shape
							.accept(task(Form.relate))
							.accept(view(Form.detail));

					final Shape authorized=redacted
							.accept(role(request.roles()));

					return empty(redacted) ? forbidden(request)
							: empty(authorized) ? unauthorized(request)
							: shaped(request, authorized);

				},
				error -> direct(request)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private Responder shaped(final Request request, final Shape shape) {
		return request.query(and(all(request.item()), shape)).map( // focused shape
				query -> shaped(request, query),
				error -> request.reply(response -> response.body(_Failure.Format, error))
		);
	}

	private Responder shaped(final Request request, final Query query) {
		return graph.browse(connection -> {

			final IRI focus=request.item();

			final Collection<Statement> model=SPARQLEngine
					.browse(connection, query)
					.values()
					.stream()
					.findFirst()
					.orElseGet(Collections::emptySet);

			return request.reply(response -> {
				if ( !contains(connection, focus) ) {

					return response.status(Response.NotFound);

				} else if ( model.isEmpty() ) { // resource known but empty envelope for the current user

					return response.status(Response.Forbidden); // !!! 404 under strict security

				} else {

					return response.status(Response.OK).map(r -> query.accept(new Query.Probe<Response>() { // !!! factor

						@Override public Response visit(final Edges edges) {
							return r.body(_Shape.Format, edges.getShape().accept(mode(Form.verify))) // hide filtering constraints
									.body(_RDF.Format, model);

						}

						@Override public Response visit(final Stats stats) {
							return r.body(_Shape.Format, StatsShape)
									.body(_RDF.Format, rewrite(model, Form.meta, focus));
						}

						@Override public Response visit(final Items items) {
							return r.body(_Shape.Format, ItemsShape)
									.body(_RDF.Format, rewrite(model, Form.meta, focus));
						}

					}));

				}

			});

		});
	}

	private Responder direct(final Request request) { // !!! optimize for SPARQL
		return graph.browse(connection -> {

			final IRI focus=request.item();
			final Collection<Statement> model=new LinkedHashModel();

			final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
			final Collection<Value> visited=new HashSet<>();

			while ( !pending.isEmpty() ) {

				final Value value=pending.remove();

				if ( visited.add(value) ) {
					if ( value.equals(focus) || value instanceof BNode ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, null, null, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								model.add(statement);
								pending.add(statement.getObject());
							}
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								null, null, value, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								model.add(statement);
								pending.add(statement.getSubject());
							}
						}

					} else if ( value instanceof IRI ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.LABEL, null, true
						)) {
							while ( statements.hasNext() ) { model.add(statements.next()); }
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.COMMENT, null, true
						)) {
							while ( statements.hasNext() ) { model.add(statements.next()); }
						}

					}
				}

			}

			return request.reply(response -> model.isEmpty()
					? response.status(Response.NotFound)
					: response.status(Response.OK).body(_RDF.Format, model)
			);

		});

	}

}
