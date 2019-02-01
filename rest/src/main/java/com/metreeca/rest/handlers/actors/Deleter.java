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
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Splitter;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.rest.Handler.handler;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.tray.Tray.tool;


/**
 * Stored resource deleter.
 *
 * <p>Handles deletion requests on the stored linked data resource identified by the request {@linkplain Request#item()
 * focus item}.</p>
 *
 * <p>If the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the shape is redacted taking into account request user {@linkplain Request#roles() roles}, {@link Form#delete}
 * task, {@link Form#verify} mode and {@link Form#detail} view.</li>
 *
 * <li>the existing RDF description of the target resource matched by the redacted shape is deleted.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the existing symmetric concise bounded description of the target resource is deleted.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is removed from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Deleter extends Delegator {

	private final Graph graph=tool(Graph.Factory);


	public Deleter() {
		delegate(handler(Request::container, container(), resource())
				.with(wrapper(Request::container, wrapper(), new Splitter(Splitter.resource())))
				.with(new Throttler(Form.delete, Form.detail))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler resource() {
		return request -> request.reply(response -> graph.update(connection -> {

			final IRI focus=request.item();
			final Shape shape=request.shape();

			if ( !connection.hasStatement(focus, null, null, true)
					&& !connection.hasStatement(null, null, focus, true) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				return response.status(Response.NotFound);

			} else {

				if ( !request.driven() ) {
					new CellEngine(connection).delete(focus);
				} else {
					new SPARQLEngine(connection).delete(focus, shape);
				}

				return response.status(Response.NoContent);

			}

		}));
	}

	private Handler container() {
		return request -> request.reply(new Failure()
				.status(Response.MethodNotAllowed)
				.cause("LDP container deletion not supported")
		);
	}

}
