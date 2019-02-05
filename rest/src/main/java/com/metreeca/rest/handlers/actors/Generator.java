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
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import static com.metreeca.form.things.Values.time;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.emptySet;


/**
 * Virtual resource generator.
 *
 * <p>Handles retrieval requests on virtual linked data resources.</p>
 *
 * <dl>
 *
 * <dt>Response {@link RDFFormat} body</dt>
 *
 * <dd>The response includes the {@linkplain RDFFormat RDF description} of the virtual request {@linkplain
 * Request#item() focus item}, as generated by the {@linkplain #Generator(Function) virtual model generator}.</dd>
 *
 * <dd>Empty generated models are reported with a {@link Response#NotFound} status code.</dd>
 *
 * </dl>
 *
 * <p>If the request includes a shape, the response includes the derived shape actually used in the resource retrieval
 * process, redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link
 * Form#verify} mode and {@link Form#detail} view.</p>
 */
public final class Generator extends Delegator {

	/**
	 * Configures the SPARQL virtual model generator.
	 *
	 * <p>The model for the virtual resource is generated by a SPARQL construct query executed on the shared
	 * {@linkplain
	 * Graph#Factory construct} tool, with the following pre-defined bindings:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>this</td>
	 * <td>the value of the request {@linkplain Request#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>user</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>time</td>
	 * <td>an {@code xsd:dateTime} literal representing the current system time with millisecond precision</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param construct the SPARQL construct query to be used as virtual model generator; empty queries are ignored
	 *
	 * @return this builder
	 *
	 * @throws NullPointerException if {@code construct} is null
	 */
	public static Function<Request, Collection<Statement>> construct(final String construct) {

		if ( construct == null ) {
			throw new NullPointerException("null construct query");
		}

		return new Function<Request, Collection<Statement>>() {

			private final Graph graph=tool(Graph.Factory);


			@Override public Collection<Statement> apply(final Request request) {
				return construct.isEmpty() ? emptySet() : graph.query(connection -> {

					final GraphQuery query=connection.prepareGraphQuery(QueryLanguage.SPARQL, construct, request.base());

					query.setBinding("this", request.item());
					query.setBinding("user", request.user());
					query.setBinding("time", time(true));

					final Collection<Statement> model=new ArrayList<>();

					query.evaluate(new AbstractRDFHandler() {
						@Override public void handleStatement(final Statement statement) { model.add(statement); }
					});

					return model;

				});
			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Request, Collection<Statement>> generator;


	public Generator(final String construct) {
		this(construct(construct));
	}

	/**
	 * Creates a virtual model generator.
	 *
	 * @param generator a function mapping from a request to a possibly empty RDF model; must return a non null value
	 *
	 * @throws NullPointerException if {@code model} is null
	 */
	public Generator(final Function<Request, Collection<Statement>> generator) {

		if ( generator == null ) {
			throw new NullPointerException("null generator");
		}

		this.generator=generator;

		delegate(generator()

				//.with(wrapper(Request::container,
				//		new Splitter(shape -> shape).wrap(new Throttler(Form.relate, Form.digest)),
				//		new Splitter(resource()).wrap(new Throttler(Form.relate, Form.detail))
				//))

				.with(handler -> request -> handler.handle(request).map(response ->
						response.header("+Vary", "Accept")
				))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler generator() { // !!! handle queries
		return request -> {

			final Collection<Statement> model=Objects.requireNonNull(
					generator.apply(request), "null generator return value"
			);

			return request.reply(response -> model.isEmpty() ? response.status(Response.NotFound) : response

					.status(Response.OK)
					.map(r -> request.shaped() ? r.shape(request.shape()) : r)
					.body(rdf(), model)

			);
		};
	}

}
