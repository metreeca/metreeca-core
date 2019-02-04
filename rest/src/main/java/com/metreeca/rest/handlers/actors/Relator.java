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
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.rest.*;
import com.metreeca.rest.engines.ShapedResource;
import com.metreeca.rest.engines.SimpleResource;
import com.metreeca.rest.engines._SPARQLEngine;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Splitter;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Collections;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Splitter.resource;
import static com.metreeca.tray.Tray.tool;


/**
 * LDP resource relator.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}.</p>
 *
 * <p>If the focus item is a {@linkplain Request#container() container} and the request includes an expected
 * {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the focus item is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
 * user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#detail} view and {@link Form#verify}
 * mode.</li>
 *
 * <li>the response {@link RDFFormat RDF body} contains the RDF description of the request focus, as matched by the
 * redacted request shape.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the response {@link RDFFormat RDF body} contains the symmetric concise bounded description of the request focus
 * item, extended with {@code rdfs:label/comment} annotations for all referenced IRIs.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Relator extends Delegator {

	// !!! activate response trimming only if a custom wrapper/handler is inserted in the pipeline


	private final Graph graph=tool(Graph.Factory);


	public Relator() {
		delegate(

				relator()

						.with(wrapper(Request::container,
								new Splitter(shape -> shape).wrap(new Throttler(Form.relate, Form.digest)),
								new Splitter(resource()).wrap(new Throttler(Form.relate, Form.detail))
						))

						.with(handler -> request -> handler.handle(request).map(response ->
								response.header("+Vary", "Accept")
						))

		);
	}

	private Handler relator() {
		return request -> request.reply(response -> graph.query(connection -> {

			final IRI item=request.item();
			final Shape shape=request.shape();

			final boolean shaped=!pass(shape);

			if ( request.container() ) {

				return request.query(shape).fold(

						query -> {

							final Collection<Statement> model=new _SPARQLEngine(connection)
									.browse(query)
									.values()
									.stream()
									.findFirst()
									.orElseGet(Collections::emptySet);

							if ( model.isEmpty() ) {

								// !!! identify and ignore housekeeping historical references (e.g. versioning/auditing)
								// !!! support returning 410 Gone if the resource is known to have existed (as identified by housekeeping)
								// !!! optimize using a single query if working on a remote repository

								final boolean contains=connection.hasStatement(item, null, null, true)
										|| connection.hasStatement(null, null, item, true);

								final Response response1=contains // !!! always 404 under strict security
										? response.status(Response.Forbidden) // resource known but empty envelope for user
										: response.status(Response.NotFound);
								return response1;

							} else {

								return response.status(Response.OK).map(r -> query.map(new Query.Probe<Response>() { // !!! factor

									@Override public Response probe(final Edges edges) {
										return r
												.shape(shape // !!! cache returned shape
														.map(new Redactor(Form.mode, Form.verify)) // hide filtering constraints
														.map(new Optimizer())
												)
												.body(rdf(), model);
									}

									@Override public Response probe(final Stats stats) {
										return r.shape(StatsShape)
												.body(rdf(), rewrite(Form.meta, item, model));
									}

									@Override public Response probe(final Items items) {
										return r.shape(ItemsShape)
												.body(rdf(), rewrite(Form.meta, item, model));
									}

								}));

							}

						},

						response::map
				);

			} else {

				final Engine engine=shaped
						? new ShapedResource(connection, shape)
						: new SimpleResource(connection);


				// !!!

				return response;

			}

		}));
	}

}
