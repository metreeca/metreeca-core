/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

import com.metreeca.core.*;
import com.metreeca.core.handlers.Delegator;
import com.metreeca.json.Shape;
import com.metreeca.json.probes.Redactor;
import com.metreeca.rest.assets.Engine;

import java.util.function.Function;

import static com.metreeca.core.Response.Forbidden;
import static com.metreeca.core.Response.Unauthorized;
import static com.metreeca.json.Shape.empty;
import static com.metreeca.rest.assets.Engine.engine;
import static com.metreeca.rest.assets.Engine.shape;
import static java.util.function.Function.identity;


/**
 * Model-driven abstract resource action handler.
 *
 * <p>Provides shared building blocks for assembling model-driven resource action handlers.</p>
 */
public abstract class Actor extends Delegator {

	private final Engine engine=Context.asset(engine());


	/**
	 * Creates a connector wrapper.
	 *
	 * @return returns a wrapper processing request inside a single {@linkplain Engine#exec(Runnable) engine
	 * transaction}
	 */
	protected Wrapper connector() {
		return handler -> request -> consumer -> engine.exec(() ->
				handler.handle(request).accept(consumer)
		);
	}

	/**
	 * Creates a throttler wrapper.
	 *
	 * @param task the accepted value for the {@linkplain Shape#Task task} parametric axis
	 * @param area the accepted values for the {@linkplain Shape#Area task} parametric axis
	 *
	 * @return returns a wrapper performing role-based shape redaction and shape-based authorization
	 */
	protected Wrapper throttler(final Object task, final Object... area) { // !!! optimize/cache
		return handler -> request -> {

			final Shape shape=request.attribute(shape());

			final Shape baseline=shape // visible to anyone taking into account task/area

					.map(new Redactor(Shape.Role, values -> true))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.Area, area))
					.map(new Redactor(Shape.Mode, Shape.Convey));

			final Shape authorized=shape // visible to user taking into account task/area

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.Area, area))
					.map(new Redactor(Shape.Mode, Shape.Convey));


			final Function<Request, Request> pre=message -> message.attribute(shape(), message.attribute(shape()) //
					// request shape redactor

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.Area, area)));

			final Function<Response, Response> post=message -> message.attribute(shape(), message.attribute(shape())
					// response shape redactor

					.map(new Redactor(Shape.Role, request.roles()))
					.map(new Redactor(Shape.Task, task))
					.map(new Redactor(Shape.Area, area))
					.map(new Redactor(Shape.Mode, Shape.Convey)));

			return empty(baseline) ? request.reply(response -> response.status(Forbidden))
					: empty(authorized) ? request.reply(response -> response.status(Unauthorized))
					: handler.handle(request.map(pre)).map(post);

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a validator wrapper.
	 *
	 * @return returns a wrapper performing engine-assisted {@linkplain Engine#validate(Message) validation} of request
	 * payloads
	 */
	protected Wrapper validator() {
		return handler -> request -> engine.validate(request).fold(request::reply, handler::handle);
	}

	/**
	 * Creates a trimmer wrapper.
	 *
	 * @return returns a wrapper performing engine-assisted {@linkplain Engine#trim(Message) validation} of {@linkplain
	 * Response#success() successful} response payloads
	 */
	protected Wrapper trimmer() {
		return handler -> request -> handler.handle(request).map(response -> response.success() ?
				engine.trim(response).fold(response::map, identity()) : response
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a creator handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#create(Request) creation}
	 */
	protected Handler creator() {
		return engine::create;
	}

	/**
	 * Creates a relator handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#create(Request) retrieval}
	 */
	protected Handler relator() {
		return engine::relate;
	}

	/**
	 * Creates an updater handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#update(Request) updating}
	 */
	protected Handler updater() {
		return engine::update;
	}

	/**
	 * Creates a deleter handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#delete(Request) deletion}
	 */
	protected Handler deleter() {
		return engine::delete;
	}

}
