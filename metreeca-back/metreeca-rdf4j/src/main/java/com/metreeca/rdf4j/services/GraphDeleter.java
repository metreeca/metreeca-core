/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf4j.services;


import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.internal;
import static com.metreeca.tree.queries.Items.items;


final class GraphDeleter extends GraphProcessor {

	private final Graph graph=service(graph());


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {
		return request.reply(internal(new UnsupportedOperationException("holder DELETE method")));
	}

	private Future<Response> member(final Request request) {
		return request.reply(response -> graph.exec(connection -> {

			final IRI item=iri(request.item());
			final Shape shape=request.shape();

			return Optional

					.of(fetch(connection, item, items(shape)))

					.filter(current -> !current.isEmpty())

					.map(current -> {

						connection.remove(outline(item, filter(shape)));
						connection.remove(current);

						return response.status(Response.NoContent);

					})

					.orElseGet(() ->

							response.status(Response.NotFound) // !!! 410 Gone if previously known

					);

		}));
	}

}
