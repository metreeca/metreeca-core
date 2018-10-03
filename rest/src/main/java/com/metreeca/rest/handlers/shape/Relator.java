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

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;


/**
 * Resource relator.
 *
 * <p>Handles retrieval requests on linked data resources.</p>
 *
 * <dl>
 *
 * <dt>Request Payload</dt>
 * <dd>
 * <dl>
 *
 * <dt>{@link ShapeFormat} {optional}</dt>
 * <dd>An optional linked data shape driving the retrieval process.</dd>
 *
 * </dl>
 * </dd>
 *
 * <dt>Response Payload</dt>
 * <dd>
 * <dl>
 *
 * <dt>{@link ShapeFormat} {optional}</dt>
 * <dd>If the request includes a shape payload, the response includes the derived shape actually used in the resource
 * retrieval process, redacted according to request user roles, retrieval task, filtering mode and detail view.</dd>
 *
 * <dt>{@link RDFFormat}</dt>
 *
 * <dd>If the request includes a {@link ShapeFormat} body representation, the response includes the {@linkplain
 * RDFFormat RDF description} of the request {@linkplain Request#item() focus item}, as defined by the associated linked
 * data {@linkplain Shape shape} redacted taking into account the user {@linkplain Request#roles() roles} of the
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
public final class Relator extends Actor<Relator> {

	private final Graph graph=tool(Graph.Factory);


	public Relator() {
		super(Form.relate, Form.detail);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Responder shaped(final Request request, final Query query) {
		return consumer -> graph.query(connection -> {

			final IRI focus=request.item();

			final Collection<Statement> model=new SPARQLEngine(connection)
					.browse(query)
					.values()
					.stream()
					.findFirst()
					.orElseGet(Collections::emptySet);

			request.reply(response -> {
				if ( model.isEmpty() ) {

					// !!! identify and ignore housekeeping historical references (e.g. versioning/auditing)
					// !!! support returning 410 Gone if the resource is known to have existed (as identified by housekeeping)
					// !!! optimize using a single query if working on a remote repository

					final boolean contains=connection.hasStatement(focus, null, null, true)
							|| connection.hasStatement(null, null, focus, true);

					return contains
							? response.status(Response.Forbidden) // resource known but empty envelope for user
							: response.status(Response.NotFound); // !!! 404 under strict security

				} else {

					return response.status(Response.OK).map(r -> query.accept(new Query.Probe<Response>() { // !!! factor

						@Override public Response visit(final Edges edges) {
							return r.body(ShapeFormat.shape()).set(edges.getShape().accept(mode(Form.verify))) // hide filtering constraints
									.body(rdf()).set(model);

						}

						@Override public Response visit(final Stats stats) {
							return r.body(ShapeFormat.shape()).set(StatsShape)
									.body(rdf()).set(rewrite(model, Form.meta, focus));
						}

						@Override public Response visit(final Items items) {
							return r.body(ShapeFormat.shape()).set(ItemsShape)
									.body(rdf()).set(rewrite(model, Form.meta, focus));
						}

					}));

				}
			}).accept(consumer);

		});
	}

	@Override protected Responder direct(final Request request, final Query query) {
		return consumer -> graph.query(connection -> {

			final IRI focus=request.item();
			final Collection<Statement> model=cell(connection, focus);

			request.reply(response -> model.isEmpty()

					? response.status(Response.NotFound)
					: response.status(Response.OK).body(rdf()).set(model)

			).accept(consumer);

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> cell(final RepositoryConnection connection, final IRI focus) { // !!! optimize for SPARQL

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

		return model;
	}

}
