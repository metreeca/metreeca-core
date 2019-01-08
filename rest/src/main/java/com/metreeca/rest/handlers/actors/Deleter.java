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
import com.metreeca.form.Shape;
import com.metreeca.form.engines.CellEngine;
import com.metreeca.form.engines.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.form.Shape.wild;
import static com.metreeca.tray.Tray.tool;


/**
 * Resource deleter.
 *
 * <p>Handles deletion requests on linked data resources.</p>
 *
 * <p>If the request includes  a {@linkplain Message#shape() shape}, it is redacted taking into account the request
 * user {@linkplain Request#roles() roles}, {@link Form#delete} task, {@link Form#verify} mode and {@link Form#detail}
 * view and used to identify the neighborhood of the request {@linkplain Request#item() focus item} to be deleted.</p>
 *
 * <dd>If the request does not include a {@linkplain Message#shape() shape}, the symmetric concise bounded description
 * of the request {@linkplain Request#item() focus item} will be deleted.</dd>
 *
 * <p>Regardless of the operating mode, resource description content is stored into the system {@linkplain
 * Graph#Factory graph} database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Deleter extends Actor<Deleter> {

	private final Graph graph=tool(Graph.Factory);


	public Deleter() {
		delegate(action(Form.delete, Form.detail).wrap(this::process));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Deleter sync(final String script) { return super.sync(script); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder process(final Request request) {
		return request.reply(response -> graph.update(connection -> {

			final IRI focus=request.item();
			final Shape shape=request.shape();

			if ( !connection.hasStatement(focus, null, null, true)
					&& !connection.hasStatement(null, null, focus, true) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				return response.status(Response.NotFound);

			} else {

				if ( wild(shape) ){
					new CellEngine(connection).delete(focus);
				} else {
					new SPARQLEngine(connection).delete(focus, shape);
				}

				return response.status(Response.NoContent);

			}

		}));
	}

}
