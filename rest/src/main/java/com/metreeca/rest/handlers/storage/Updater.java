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

package com.metreeca.rest.handlers.storage;


import com.metreeca.form.*;
import com.metreeca.form.engines.CellEngine;
import com.metreeca.form.engines.SPARQLEngine;
import com.metreeca.form.probes.Outliner;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.function.BiFunction;

import javax.json.JsonValue;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.Shape.wild;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


/**
 * Stored resource updater.
 *
 * <p>Handles updating requests on the stored linked data resource identified by the request {@linkplain Request#item()
 * focus item}, taking into account the expected resource {@linkplain Message#shape() shape}, if one is provided.</p>
 *
 * <dl>
 *
 * <dt>Shape-less mode</dt>
 *
 * <dd>If no shape is provided, the request {@link RDFFormat} body is expected to contain a symmetric concise bounded
 * description of the resource to be updated; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</dd>
 *
 * <dd>On successful body validation, the existing symmetric concise bounded description of the target resource is
 * replaced with the updated one.</dd>
 *
 * <dt>Shape-driven mode</dt>
 *
 * <dd>If a shape is provided, it is redacted taking into account the request user {@linkplain Request#roles() roles},
 * {@link Form#update} task, {@link Form#verify} mode and {@link Form#detail} view.</dd>
 *
 * <dd>The request {@link RDFFormat} body is expected to contain an RDF description of the resource to be updated
 * matching the redacted shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</dd>
 *
 * <dd>On successful body validation, the existing RDF description of the target resource matched by the redacted shape
 * is replaced with the updated one.</dd>
 *
 * </dl>
 *
 * <p>Regardless of the operating mode, the updated RDF resource description is stored into the system {@linkplain
 * Graph#Factory graph} database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Updater extends Actor<Updater> {

	private final Graph graph=tool(Graph.Factory);


	public Updater() {
		delegate(action(Form.update, Form.detail).wrap((Request request) -> request.body(rdf())

				.value(model -> { // add implied statements

					model.addAll(request.shape()
							.accept(mode(Form.verify))
							.accept(new Outliner(request.item()))
					);

					return model;

				})

				.fold(
						model -> process(request, model),
						request::reply
				)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Updater pre(final BiFunction<Request, Model, Model> filter) { return super.pre(filter); }

	@Override public Updater sync(final String script) { return super.sync(script); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder process(final Request request, final Collection<Statement> model) {
		return request.reply(response -> graph.update(connection -> {

			final IRI focus=request.item();

			if ( !connection.hasStatement(focus, null, null, true)
					&& !connection.hasStatement(null, null, focus, true) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				return response.status(Response.NotFound);

			} else {


				final Shape shape=request.shape();
				final Collection<Statement> update=trace(model);

				final Report report=wild(shape)
						? new CellEngine(connection).update(focus, update)
						: new SPARQLEngine(connection).update(focus, shape, update);

				if ( report.assess(Issue.Level.Error) ) { // shape violations

					connection.rollback();

					// !!! rewrite report value references to original target iri
					// !!! rewrite references to external base IRI
					// !!! factor with Creator

					return response.map(new Failure()
							.status(Response.UnprocessableEntity)
							.error("data-invalid")
							.trace(report(report)));

				} else { // valid data

					connection.commit();

					return response.status(Response.NoContent);

				}
			}
		}));
	}

}

