/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.operators;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;

import static com.metreeca.json.shapes.Guard.Detail;
import static com.metreeca.json.shapes.Guard.Update;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.NoContent;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Wrapper.keeper;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;
import static com.metreeca.rest.services.Engine.engine;


/**
 * Model-driven resource updater.
 *
 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
 * item}.</p>
 *
 * <ul>
 *
 * <li>redacts the {@linkplain JSONLDFormat#shape() shape} associated with the request according to the request
 * user {@linkplain Request#roles() roles};</li>
 *
 * <li>performs shape-based {@linkplain Wrapper#keeper(Object, Object) authorization}, considering the subset of
 * the request shape enabled by the {@linkplain Guard#Update} task and the {@linkplain Guard#Detail} view.</li>
 *
 * <li>validates the {@link JSONLDFormat JSON-LD} request body against the request shape; malformed or invalid
 * payloads are reported respectively with a {@value Response#BadRequest} or a {@value Response#UnprocessableEntity}
 * status code;</li>
 *
 * <li>updates the existing description of the resource matching the redacted request shape with the assistance of the
 * shared linked data {@linkplain Engine#update(Frame, Shape) engine}.</li>
 *
 * </ul>
 *
 * <p>If the shared linked data engine was able to locate a resource matching the request item, generates a response
 * including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NoContent} status code.</li>
 *
 * </ul>
 *
 * <p>Otherwise, generates a response including:</p>
 *
 * <ul>
 *
 * <li>a {@value Response#NotFound} status code.</li>
 *
 * </ul>
 */
public final class Updater extends Delegator {

	/**
	 * Creates a resource updater.
	 *
	 * @return a new resource updater
	 */
	public static Updater updater() {
		return new Updater();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Engine engine=service(engine());


	private Updater() {
		delegate(update().with(
				keeper(Update, Detail)
		));
	}


	private Handler update() {
		return request -> {

			final Shape shape=request.get(shape());

			return request.body(jsonld()).fold(request::reply, frame -> engine.update(frame, shape)

					.map(iri -> request.reply(status(NoContent)))

					.orElseGet(() -> request.reply(status(NotFound))) // !!! 410 Gone if previously known

			);

		};
	}

}
