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

package com.metreeca.rest.assets;

import com.metreeca.json.Shape;
import com.metreeca.json.Trace;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.IRI;

import javax.json.*;
import java.util.Collection;
import java.util.function.*;

import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.JSONLDFormat.*;


/**
 * Model-driven storage engine.
 *
 * <p>Manages storage transactions, performs storage-specific shape/payload tasks and handles model-driven CRUD actions
 * on resources and containers.</p>
 */
public interface Engine {

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}


	//// Transaction Management ////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task within a storage transaction.
	 *
	 * <p>If a transaction is not already active on the underlying storage, begins one and commits it on successful
	 * task completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in
	 * either case, no action is taken if the transaction was already terminated inside the task.</p>
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public default void exec(final Runnable task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		exec(() -> {

			task.run();

			return this;

		});
	}

	/**
	 * Executes a task within a storage transaction.
	 *
	 * <p>If a transaction is not already active on the underlying storage, begins one and commits it on successful
	 * task completion; if the task throws an exception, the transaction is rolled back and the exception rethrown; in
	 * either case, no action is taken if the transaction was already terminated inside the task.</p>
	 *
	 * <p>Falls back to plain task execution if transactions are not supported by this engine.</p>
	 *
	 * @param task the task to be executed
	 * @param <R>  the type of the value returned by the task
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <R> R exec(final Supplier<R> task);


	//// CRUD Actions //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles creation requests.
	 *
	 * <p>Handles creation requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a creation request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the creation {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> create(final Request request);

	/**
	 * Handles retrieval requests.
	 *
	 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using the message {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a retrieval request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the retrieval {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> relate(final Request request);

	/**
	 * Handles updating requests.
	 *
	 * <p>Handles updating requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request an updating request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the updating {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> update(final Request request);

	/**
	 * Handles deletion requests.
	 *
	 * <p>Handles deletion requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using  the message {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a deletion request for the managed linked data resource
	 *
	 * @return a lazy response generated for the managed linked data resource in reaction to the deletion {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> delete(final Request request);


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a connector wrapper.
	 *
	 * @return returns a wrapper processing request inside a single {@linkplain Engine#exec(Runnable) engine
	 * transaction}
	 */
	public default Wrapper connector() {
		return handler -> request -> consumer -> exec(() ->
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
	public default Wrapper throttler(final Object task, final Object... area) { // !!! optimize/cache
		return handler -> request -> {

			final Shape shape=request.attribute(shape());

			final Shape baseline=shape.redact(  // visible to anyone taking into account task/area
					retain(Role, true),
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


	/**
	 * Creates a validator wrapper.
	 *
	 * @return returns a wrapper performing model-driven {@linkplain JSONLDFormat#validate(IRI, Shape, Collection)
	 * validation} of request JSON-LD bodies
	 */
	public default Wrapper validator() {

		final class Formatter implements Function<Trace, JsonObject> { // ;(java8) no private interface methods
			@Override public JsonObject apply(final Trace trace) {

				final JsonObjectBuilder builder=Json.createObjectBuilder();

				if ( !trace.issues().isEmpty() ) {

					final JsonObjectBuilder errors=Json.createObjectBuilder();

					trace.issues().forEach((detail, values) -> {

						final JsonArrayBuilder objects=Json.createArrayBuilder();

						values.forEach(value -> {

							if ( value != null ) { objects.add(format(value)); }

						});

						errors.add(detail, objects.build());

					});

					builder.add("", errors);
				}

				trace.fields().forEach((name, nested) -> {

					if ( !nested.empty() ) {
						builder.add(name.toString(), apply(nested));
					}

				});

				return builder.build();
			}
		}


		return handler -> request -> request.body(jsonld())

				.flatMap(object -> validate(iri(request.item()), request.attribute(shape()), object).fold(
						trace -> Left(status(UnprocessableEntity, new Formatter().apply(trace))),
						model -> Right(handler.handle(request))
				))

				.fold(request::reply);
	}

	/**
	 * Creates a trimmer wrapper.
	 *
	 * @return returns a wrapper performing engine-assisted {@linkplain JSONLDFormat#trim(IRI, Shape, JsonObject)
	 * trimming} of {@linkplain Response#success() successful} response JSON-LD bodies
	 */
	public default Wrapper trimmer() {
		return handler -> request -> handler.handle(request).map(response -> response.success()

				? response.body(json())
				.map(json -> response.body(json(), trim(iri(response.item()), response.attribute(shape()), json)))
				.fold(response::map)

				: response
		);
	}

}
