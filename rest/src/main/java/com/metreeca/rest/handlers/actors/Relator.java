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

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.Shape.wild;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Structures.cell;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


/**
 * Resource relator.
 *
 * <p>Handles retrieval requests on linked data resources.</p>
 *
 * <dl>
 *
 * <dt>Response shape-driven {@link RDFFormat} body</dt>
 *
 * <dd>If the request includes  a {@linkplain Message#shape() shape}, the response includes the {@linkplain
 * RDFFormat RDF description} of the request {@linkplain Request#item() focus item}, as defined by the redacted linked
 * data {@linkplain Shape shape}.</dd>
 *
 * <dt>Response shapeless {@link RDFFormat} body</dt>
 *
 * <dd>If the request does not include a {@linkplain Message#shape() shape}, the response includes the symmetric concise
 * bounded description of the request focus item, extended with {@code rdfs:label/comment} annotations for all
 * referenced IRIs.</dd>
 *
 * </dl>
 *
 * <p>If the request includes a shape, the response includes the derived shape actually used in the resource retrieval
 * process, redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link
 * Form#verify} mode and {@link Form#detail} view.</p>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Relator extends Actor<Relator> {

	private final Graph graph=tool(Graph.Factory);


	public Relator() {
		delegate(action(Form.relate, Form.detail).wrap((Request request) -> (

				wild(request.shape()) ? direct(request) : driven(request))

				.map(response -> response.success() ?

						response.headers("Vary", "Accept") : response

				)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Relator post(final BiFunction<Response, Model, Model> filter) { return super.post(filter); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request) {
		return consumer -> graph.query(connection -> {

			final IRI focus=request.item();
			final Collection<Statement> model=cell(focus, true, connection);

			request.reply(response -> model.isEmpty()

					? response.status(Response.NotFound)
					: response.status(Response.OK).body(rdf(), model)

			).accept(consumer);

		});
	}

	private Responder driven(final Request request) {

		final Shape shape=request.shape();

		final IRI focus=request.item();

		return request.query(and(shape, all(focus))).fold(query -> request.reply(response -> graph.query(connection -> {

			final Collection<Statement> model=new SPARQLEngine(connection)
					.browse(query)
					.values()
					.stream()
					.findFirst()
					.orElseGet(Collections::emptySet);

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
						return r.shape(shape.accept(mode(Form.verify))) // hide filtering constraints
								.body(rdf(), model);
					}

					@Override public Response visit(final Stats stats) {
						return r.shape(StatsShape)
								.body(rdf(), rewrite(model, Form.meta, focus));
					}

					@Override public Response visit(final Items items) {
						return r.shape(ItemsShape)
								.body(rdf(), rewrite(model, Form.meta, focus));
					}

				}));

			}

		})), request::reply);
	}

}
