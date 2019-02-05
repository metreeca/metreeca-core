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
import com.metreeca.rest.*;
import com.metreeca.rest.engines.GraphEngine;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;

import java.util.IdentityHashMap;
import java.util.Map;

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

	private final Map<Shape, Engine> engines=new IdentityHashMap<>();


	public Deleter() {
		delegate(deleter()
				// !!! .with(splitter())
				.with(throttler())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Wrapper splitter() {
	//	return wrapper(Request::container, wrapper(), new Splitter(resource()));
	//}

	private Throttler throttler() {
		return new Throttler(Form.delete, Form.detail);
	}

	private Handler deleter() {
		return request -> request.reply(response -> graph.update(connection -> {

			final IRI item=request.item();
			final Shape shape=request.shape();

			// !!! container
			//  final Engine engine=request.container()
			//		? shaped ? new ShapedContainer(graph, shape) : new SimpleContainer(graph)
			//		: shaped ? new ShapedResource(graph, shape) : new SimpleResource(graph);

			// !!! 410 Gone if the resource is known to have existed (how to test?)

			try {

				return engine(shape).delete(item).isPresent()
						? response.status(Response.NoContent)
						: response.status(Response.NotFound);

			} catch ( final UnsupportedOperationException e ) {

				return response.map(new Failure()
						.status(Response.MethodNotAllowed)
						.cause(e.getMessage())
				);

			}

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Engine engine(final Shape shape) {
		return engines.computeIfAbsent(shape, _shape -> new GraphEngine(graph, _shape));
	}

}
