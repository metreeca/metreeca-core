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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;

import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Engine.engine;
import static com.metreeca.tree.probes.Evaluator.empty;


/**
 * Model-driven abstract resource action handler.
 *
 * <p>Provides shared building blocks for assembling model-driven resource action handlers.</p>
 */
public abstract class Actor extends Delegator { // !!! tbd

	private final Engine engine=service(engine());


	protected Wrapper connector() { // inside a single txn
		return handler -> request -> consumer -> engine.exec(() ->
				handler.handle(request).accept(consumer)
		);
	}

	protected Wrapper splitter(final boolean content) {
		return handler -> request -> handler.handle(request.shape(
				content? engine.resource(request.shape()) : engine.container(request.shape())
		));
	}

	protected Wrapper throttler(final String task, final String view) { // !!! optimize/caching
		return handler -> request -> {

			final Shape shape=request.shape();

			final Shape baseline=shape // visible to anyone taking into account task/view

					.map(new Redactor(Shape.Role))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.View, view))
					.map(new Redactor(Shape.Mode, Shape.Convey));

			final Shape authorized=shape // visible to user taking into account task/view

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.View, view))
					.map(new Redactor(Shape.Mode, Shape.Convey));


			final Function<Request, Request> pre=message -> message.shape(message.shape() // request shape redactor

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.View, view))

					// mode (convey/filter) redaction performed by action handlers

					.map(new Optimizer())
			);

			final Function<Response, Response> post=message -> message.shape(message.shape() // response shape redactor

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.View, view))
					.map(new Redactor(Shape.Mode, Shape.Convey))

					.map(new Optimizer())
			);

			return empty(baseline) ? request.reply(response -> response.status(Response.Forbidden))
					: empty(authorized) ? request.reply(response -> response.status(Response.Unauthorized))
					: handler.handle(request.map(pre)).map(post);

		};
	}

	protected Wrapper validator() {
		return handler -> request -> engine.validate(request).fold(handler::handle, request::reply);
	}

	private Wrapper trimmer() {
		return handler -> request -> handler.handle(engine.trim(request));
	}

	protected Handler handler() {
		return engine;
	}

}
