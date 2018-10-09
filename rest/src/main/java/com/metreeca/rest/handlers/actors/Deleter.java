/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.form.Shape.wild;
import static com.metreeca.tray.Tray.tool;


public final class Deleter extends Actor<Deleter> {

	private final Graph graph=tool(Graph.Factory);

	public Deleter() {
		delegate(handler(Form.delete, Form.detail, (request, shape) ->
				wild(shape) ? direct(request) : driven(request, shape)
		));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request) {
		return request.reply(response -> response.map(new Failure<>()
				.status(Response.NotImplemented)
				.cause("shapeless resource creation not supported"))
		);
	}

	private Responder driven(final Request request, final Shape shape) {
		return request.reply(response -> graph.update(connection -> {


			final IRI focus=request.item();

			if ( !connection.hasStatement(focus, null, null, true)
					&& !connection.hasStatement(null, null, focus, true) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				return response.status(Response.NotFound);

			} else {

				new SPARQLEngine(connection).delete(focus, shape);

				return response.status(Response.NoContent);

			}

		}));
	}

}
