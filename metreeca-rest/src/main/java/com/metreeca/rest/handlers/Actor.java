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
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.Forbidden;
import static com.metreeca.rest.Response.Unauthorized;
import static com.metreeca.rest.services.Engine.engine;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.function.Function.identity;


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


	protected Wrapper splitter(final boolean traverse) {
		return handler -> request -> handler.handle(request.shape(
				traverse ? engine.resource(request.shape()) : engine.container(request.shape())
		));
	}

	protected Wrapper throttler(final String task, final String view) { // !!! optimize/cache
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

			return baseline.map(new Evaluator()) != null ? request.reply(response -> response.status(Forbidden))
					: authorized.map(new Evaluator()) != null ? request.reply(response -> response.status(Unauthorized))
					: handler.handle(request.map(pre)).map(post);

		};
	}


	protected Wrapper validator() {
		return handler -> request -> engine.validate(request).fold(handler::handle, request::reply);
	}

	protected Wrapper trimmer() {
		return handler -> request -> handler.handle(request).map(response ->
				response.success()? engine.trim(response).fold(identity(), response::map) : response
		);
	}


	protected Handler creator() {
		return engine::create;
	}

	protected Handler relator() {
		return engine::relate;
	}

	protected Handler updater() {
		return engine::update;
	}

	protected Handler deleter() {
		return engine::delete;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Evaluator extends Traverser<Boolean> {

		@Override public Boolean probe(final Meta meta) {
			return true;
		}


		@Override public Boolean probe(final Field field) {
			return null;
		}

		@Override public Boolean probe(final And and) {
			return and.getShapes().stream()
					.filter(shape -> !(shape instanceof Meta))
					.map(shape -> shape.map(this))
					.reduce(true, (x, y) -> x == null || y == null ? null : x && y);
		}

		@Override public Boolean probe(final Or or) {
			return or.getShapes().stream()
					.filter(shape -> !(shape instanceof Meta))
					.map(shape -> shape.map(this))
					.reduce(false, (x, y) -> x == null || y == null ? null : x || y);
		}

		@Override public Boolean probe(final When when) {

			final Boolean test=when.getTest().map(this);
			final Boolean pass=when.getPass().map(this);
			final Boolean fail=when.getFail().map(this);

			return TRUE.equals(test) ? pass
					: FALSE.equals(test) ? fail
					: TRUE.equals(pass) && TRUE.equals(fail) ? TRUE
					: FALSE.equals(pass) && FALSE.equals(fail) ? FALSE
					: null;
		}

	}

}
