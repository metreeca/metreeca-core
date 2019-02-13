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

package com.metreeca.rest.handlers;

import com.metreeca.form.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.engines.GraphEngine;
import com.metreeca.tray.rdf.Graph;

import static com.metreeca.tray.Tray.tool;


/**
 * Linked data action handler.
 *
 * <p>Handles actions on linked data resources; the base class:</p>
 *
 * <ul>
 * <li>handles {@link UnsupportedOperationException} thrown by engines.</li>
 * </ul>
 */
public abstract class Actor extends Delegator { // !!! remove

	private final Graph graph=tool(Graph.Factory);


	/**
	 * Creates a shape-driven resource engine.
	 *
	 * @param shape the shape driving the engine to be created
	 *
	 * @return a resource engine driven by {@code shape}; the returned value is cached for future use
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	protected Engine engine(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new GraphEngine(graph, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return consumer -> {
			try {

				super.handle(request).accept(consumer);

			} catch ( final NotImplementedException e ) {

				request.reply(response -> response.map(new Failure()
						.status(Response.NotImplemented)
						.cause(e.getMessage())
				)).accept(consumer);

			}
		};
	}


	public static final class NotImplementedException extends RuntimeException {

		private static final long serialVersionUID=-5919197536228324934L;

		public NotImplementedException(final String message) {

			super(message);

			if ( message == null ) {
				throw new NullPointerException("null message");
			}

		}

	}

}
