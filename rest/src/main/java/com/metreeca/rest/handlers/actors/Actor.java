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

import com.metreeca.form.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.engines.GraphEngine;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.tray.rdf.Graph;

import java.util.IdentityHashMap;
import java.util.Map;

import static com.metreeca.tray.Tray.tool;


public abstract class Actor extends Delegator {

	private final Graph graph=tool(Graph.Factory);

	private final Map<Shape, Engine> engines=new IdentityHashMap<>();


	protected Engine engine(final Shape shape) {
		return engines.computeIfAbsent(shape, _shape -> new GraphEngine(graph, _shape));
	}


	@Override public Responder handle(final Request request) {
		return consumer -> {
			try {

				super.handle(request).accept(consumer);

			} catch ( final UnsupportedOperationException e ) {

				request.reply(response -> response.map(new Failure()
						.status(Response.MethodNotAllowed)
						.cause(e.getMessage())
				)).accept(consumer);

			}
		};
	}

}
