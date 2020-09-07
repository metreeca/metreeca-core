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

package com.metreeca.rdf4j.assets;


import com.metreeca.core.*;
import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.InternalServerError;
import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf4j.assets.Graph.graph;


final class GraphDeleter extends GraphProcessor {

	private final Graph graph=Context.asset(graph());


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {
		return request.reply(status(InternalServerError, new UnsupportedOperationException("holder DELETE method")));
	}

	private Future<Response> member(final Request request) {
		return request.reply(response -> graph.exec(connection -> {

			final IRI item=iri(request.item());
			final Shape shape=request.attribute(shape());

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
