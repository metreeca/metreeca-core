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

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;

import javax.json.JsonObject;
import java.util.Collection;
import java.util.function.UnaryOperator;

import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.JSONLDFormat.*;


/**
 * Model-driven abstract resource action handler.
 *
 * <p>Provides shared building blocks for assembling model-driven resource action handlers.</p>
 */
public abstract class Actor extends Delegator {

	private final Engine engine=Context.asset(Engine.engine());


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
	 * @param task the accepted value for the {@linkplain Guard#Task task} parametric axis
	 * @param area the accepted values for the {@linkplain Guard#Area task} parametric axis
	 *
	 * @return returns a wrapper performing role-based shape redaction and shape-based authorization
	 */
	protected Wrapper throttler(final Object task, final Object... area) { // !!! optimize/cache
		return handler -> request -> {

			final Shape shape=request.attribute(shape());

			final Shape baseline=shape.redact(  // visible to anyone taking into account task/area
					retain(Role),
					retain(Task, task),
					retain(Area, area),
					retain(Mode, Convey)
			);

			final Shape authorized=shape.redact( // visible to user taking into account task/area
					retain(Role, request.roles()),
					retain(Task, task),
					retain(Area, area),
					retain(Mode, Convey)

			);

			// request shape redactor

			final UnaryOperator<Request> pre=message -> message.attribute(shape(), message.attribute(shape()).redact(

					retain(Role, request.roles()),
					retain(Task, task),
					retain(Area, area)

			));

			// response shape redactor

			final UnaryOperator<Response> post=message -> message.attribute(shape(), message.attribute(shape()).redact(
					retain(Role, request.roles()),
					retain(Task, task),
					retain(Area, area),
					retain(Mode, Convey)
			));

			return baseline.validates(false) ? request.reply(status(Forbidden))
					: authorized.validates(false) ? request.reply(status(Unauthorized))
					: handler.handle(request.map(pre)).map(post);

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a validator wrapper.
	 *
	 * @return returns a wrapper performing model-driven
	 * {@linkplain JSONLDFormat#validate(org.eclipse.rdf4j.model.IRI, Shape, Collection)
	 * validation} of request JSON-LD bodies
	 */
	protected Wrapper validator() {
		return handler -> request -> request.body(jsonld())

				.flatMap(object -> validate(iri(request.item()), request.attribute(shape()), object).fold(
						trace -> Left(status(UnprocessableEntity, trace.toJSON())),
						model -> Right(handler.handle(request))
				))

				.fold(request::reply);
	}

	/**
	 * Creates a trimmer wrapper.
	 *
	 * @return returns a wrapper performing engine-assisted
	 * {@linkplain JSONLDFormat#trim(org.eclipse.rdf4j.model.IRI, Shape, JsonObject) trimming}
	 * of {@linkplain Response#success() successful} response JSON-LD bodies
	 */
	protected Wrapper trimmer() {
		return handler -> request -> handler.handle(request).map(response -> response.success()

				? response.body(json())
				.map(json -> response.body(json(), trim(iri(response.item()), response.attribute(shape()), json)))
				.fold(response::map)

				: response
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a creator handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#create(Request) creation}
	 */
	protected Handler _creator() {
		return engine::create;
	}

	/**
	 * Creates a relator handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#create(Request) retrieval}
	 */
	protected Handler _relator() {
		return engine::relate;
	}

	/**
	 * Creates an updater handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#update(Request) updating}
	 */
	protected Handler _updater() {
		return engine::update;
	}

	/**
	 * Creates a deleter handler.
	 *
	 * @return returns a handler performing engine-assisted resource {@linkplain Engine#delete(Request) deletion}
	 */
	protected Handler _deleter() {
		return engine::delete;
	}

}
