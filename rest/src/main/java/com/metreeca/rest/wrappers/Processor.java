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

package com.metreeca.rest.wrappers;


import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.Update;

import java.util.function.BiFunction;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Objects.requireNonNull;


/**
 * RDF processor.
 *
 * <p>Process {@linkplain RDFFormat RDF} payloads for incoming request and outgoing responses and executes SPARQL
 * Update post-processing scripts.</p>
 */
public final class Processor implements Wrapper {

	private BiFunction<Request, Model, Model> pre;
	private BiFunction<Response, Model, Model> post;

	private String update="";

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Inserts a pre-processing RDF filter.
	 *
	 * <p>The filter is chained after previously inserted pre-processing filters and executed on incoming requests and
	 * their {@linkplain RDFFormat RDF} payload, if one is present, or ignored, otherwise.</p>
	 *
	 * @param filter the RDF pre-processing filter to be inserted; takes as argument an incoming request and its
	 *               {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor pre(final BiFunction<Request, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.pre=chain(pre, filter);

		return this;
	}

	/**
	 * Inserts a post-processing RDF filter.
	 *
	 * <p>The filter is chained after previously inserted post-processing filters and executed on {@linkplain
	 * Response#success() successful} outgoing responses and their {@linkplain RDFFormat RDF} payload, if one is
	 * present, or ignored, otherwise.</p>
	 *
	 * @param filter the RDF post-processing filter to be inserted; takes as argument a successful outgoing response and
	 *               its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor post(final BiFunction<Response, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.post=chain(post, filter);

		return this;
	}

	/**
	 * Configures the SPARQL Update post-processing script.
	 *
	 * <p>The script is executed on the shared {@linkplain Graph#Factory graph} tool on {@linkplain Response#success()
	 * successful} request processing by wrapped handlers and {@linkplain #post(BiFunction) post-processing filters},
	 * with the following pre-defined bindings:</p>
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
	 * <td>the value of the response {@linkplain Response#item() focus item}</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>stem</td>
	 * <td>the {@linkplain IRI#getNamespace() namespace} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>name</td>
	 * <td>the local {@linkplain IRI#getLocalName() name} of the IRI bound to the {@code this} variable</td>
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
	 * @param update the SPARQL Update update to be executed by this processor on successful request processing; empty
	 *               scripts are ignored
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code update} is null
	 */
	public Processor update(final String update) {

		if ( update == null ) {
			throw new NullPointerException("null update script");
		}

		this.update=update;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return pre().wrap(post()).wrap(update()).wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T extends Message<T>> BiFunction<T, Model, Model> chain(
			final BiFunction<T, Model, Model> pipeline, final BiFunction<T, Model, Model> filter
	) {

		final BiFunction<T, Model, Model> checked=(request, model) ->
				requireNonNull(filter.apply(request, model), "null filter return value");

		return (pipeline == null) ? checked
				: (request, model) -> checked.apply(request, pipeline.apply(request, model));
	}

	private <T extends Message<T>> T process(final T message, final BiFunction<T, Model, Model> filter) {
		return message.body(rdf()).pipe(statements ->
				(filter == null) ? statements : filter.apply(message, new LinkedHashModel(statements)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> handler.handle(process(request, pre));
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request)
				.map(response -> response.success() ? process(response, post) : response);
	}

	private Wrapper update() {
		return handler -> request -> handler.handle(request).map(response -> {

			if ( response.success() && !update.isEmpty() ) {
				graph.update(connection -> {

					final IRI user=response.request().user();
					final IRI item=response.item();

					final Update update=connection.prepareUpdate(QueryLanguage.SPARQL, this.update, request.base());

					update.setBinding("this", item);
					update.setBinding("stem", iri(item.getNamespace()));
					update.setBinding("name", literal(item.getLocalName()));
					update.setBinding("user", user);
					update.setBinding("time", time(true));

					update.execute();
				});
			}

			return response;

		});
	}

}
