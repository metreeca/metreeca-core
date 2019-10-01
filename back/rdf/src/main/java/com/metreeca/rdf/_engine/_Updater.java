/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf._engine;


import com.metreeca.tree.Form;
import com.metreeca.tree.Issue;
import com.metreeca.tree.Shape;
import com.metreeca.tree.things.Shapes;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;

import javax.json.JsonValue;

import static com.metreeca.tree.things.Shapes.resource;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.tray.Trace.trace;
import static com.metreeca.tray.Tray.tool;


/**
 * LDP resource updater.
 *
 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}, according to the following operating modes.</p>
 *
 * <p>If the request target is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>the request is reported with a {@linkplain Response#NotImplemented} status code.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request includes an expected {@linkplain Request#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the resource shape is extracted and redacted taking into account request user {@linkplain Request#roles()
 * roles}, {@link Form#update} task, {@link Form#convey} mode and {@link Form#detail} view</li>
 *
 * <li>the request {@link RDFBody RDF body} is expected to contain an RDF description of the resource to be updated
 * matched by the redacted resource shape; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element.</li>
 *
 * <li>on successful body validation, the existing RDF description of the target resource matched by the redacted shape
 * is replaced with the request RDF body.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the request {@link RDFBody RDF body} is expected to contain a symmetric concise bounded description of the
 * resource to be updated; statements outside this envelope are reported with a {@linkplain
 * Response#UnprocessableEntity} status code and a structured {@linkplain Failure#trace(JsonValue) trace} element;</li>
 *
 * <li>on successful body validation, the existing symmetric concise bounded description of the target resource is
 * replaced with the request RDF body.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is updated in the system storage {@linkplain Engine#engine() engine}.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class _Updater extends Delegator {

	private final Engine engine=tool(engine());
	private final Trace trace=tool(trace());


	public _Updater() {
		delegate(updater()
				.with(throttler())
				.with(connector())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return wrapper(Request::container,
				new Throttler(Form.update, Form.detail, Shapes::container),
				new Throttler(Form.update, Form.detail, Shapes::resource)
		);
	}

	private Wrapper connector() {
		return handler -> request -> engine.exec(() -> handler.handle(request));
	}

	private Handler updater() {
		return request -> request.container() ? request.reply(

				new Failure()
						.status(Response.NotImplemented)
						.cause("container updating not supported")

		) : request.body(rdf()).fold(

				rdf -> {

					final IRI item=request.item();
					final Shape shape=resource(item, request.shape());
					final Collection<Statement> model=trace.trace(this, rdf);

					return request.reply(response -> engine.update(item, shape, model)

							.map(focus -> focus.assess(Issue.Level.Error) // shape violations

									? response.map(new Failure()
									.status(Response.UnprocessableEntity)
									.error(Failure.DataInvalid)
									.trace(focus))

									: response.status(Response.NoContent)

							)

							.orElseGet(() -> response.status(Response.NotFound)) // !!! 410 Gone if previously known

					);
				},

				request::reply

		);
	}

}

