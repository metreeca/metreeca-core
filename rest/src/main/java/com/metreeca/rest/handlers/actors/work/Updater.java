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

package com.metreeca.rest.handlers.actors.work;


import com.metreeca.form.*;
import com.metreeca.form.engines.CellEngine;
import com.metreeca.form.engines.SPARQLEngine;
import com.metreeca.form.probes.Outliner;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;

import javax.json.JsonValue;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.toList;


/**
 * Stored resource updater.
 *
 * <p>Handles updating requests on the stored linked data resource identified by the request {@linkplain Request#item()
 * focus item}.</p>
 *
 * <p>If the request target is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the resource shape is extracted and redacted taking into account request user {@linkplain Request#roles()
 * roles}, {@link Form#update} task, {@link Form#verify} mode and {@link Form#detail} view</li>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain an RDF description of the resource to be updated
 * matched by the redacted resource shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
 *
 * <li>on successful body validation, the existing RDF description of the target resource matched by the redacted shape
 * is replaced with the updated one.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the request {@link RDFFormat RDF body} is expected to contain a symmetric concise bounded description of the
 * resource to be updated; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
 *
 * <li>on successful body validation, the existing symmetric concise bounded description of the target resource is
 * replaced with the updated one.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is updated in the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Updater implements Handler{

	private final Graph graph=tool(Graph.Factory);


	//public Updater() {
	//	delegate(query(false)
	//			// !!! .wrap(modulator().task(Form.update).view(Form.detail))
	//			.wrap(processor())
	//			.wrap(this::process)
	//	);
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder process(final Request request) {
		return request.container() ? container(request) : resource(request);
	}

	private Responder container(final Request request) {
		return request.reply(response -> response.status(Response.NotImplemented));
	}

	private Responder resource(final Request request) {
		return request.body(rdf()).fold(model -> request.reply(response -> graph.update(connection -> {

			final IRI focus=request.item();

			if ( !connection.hasStatement(focus, null, null, true)
					&& !connection.hasStatement(null, null, focus, true) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				return response.status(Response.NotFound);

			} else {

				final Shape shape=/* !!! resource*/(request.shape());
				final Collection<Statement> update=/* !!! trace*/(expand(focus, shape, model));

				final Focus report=pass(shape)
						? new CellEngine(connection).update(focus, update)
						: new SPARQLEngine(connection).update(focus, shape, update);

				if ( report.assess(Issue.Level.Error) ) { // shape violations

					connection.rollback();

					return response.map(new Failure()
							.status(Response.UnprocessableEntity)
							.error(Failure.DataInvalid)
							.trace(report));

				} else { // valid data

					connection.commit();

					return response.status(Response.NoContent);

				}
			}

		})), request::reply);
	}

	private Collection<Statement> expand(final IRI focus, final Shape shape, final Collection<Statement> model) {

		model.addAll(shape // add implied statements
				.map(mode(Form.verify))
				.map(new Outliner(focus))
				.collect(toList())
		);

		return model;
	}


	@Override public Responder handle(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}
}

