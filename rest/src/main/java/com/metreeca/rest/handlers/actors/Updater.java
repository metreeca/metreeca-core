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
import com.metreeca.form.Issue;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.rest.engines.GraphEngine;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import javax.json.JsonValue;

import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.rest.handlers.actors._Shapes.resource;
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
 * <p>Regardless of the operating mode, RDF data is updated in the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Updater extends Delegator {

	private final Trace trace=tool(Trace.Factory);

	private final _Engine engine=new GraphEngine();


	public Updater() {
		delegate(updater().with(throttler()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper throttler() {
		return wrapper(Request::container,
				new Throttler(Form.update, Form.detail, _Shapes::container),
				new Throttler(Form.update, Form.detail, _Shapes::resource)
		);
	}

	private Handler updater() {
		return request -> request.container()? request.reply(

				new Failure().status(Response.NotImplemented).cause("container updating not supported")

		) : request.body(rdf()).fold(

				model -> request.reply(response -> engine

						.update(
								request.item(),
								resource(request.item(), request.shape()),
								trace.trace(this, model)
						)

						.map(focus -> focus.assess(Issue.Level.Error) // shape violations

								? response.map(new Failure()
								.status(Response.UnprocessableEntity)
								.error(Failure.DataInvalid)
								.trace(focus))

								: response.status(Response.NoContent)

						)

						.orElseGet(() -> response.status(Response.NotFound)) // !!! 410 Gone if previously known

				),

				request::reply

		);
	}

}

