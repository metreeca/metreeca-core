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

package com.metreeca.rest.assets;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.Forbidden;
import static com.metreeca.rest.Response.Unauthorized;
import static com.metreeca.rest.formats.JSONLDFormat.shape;


/**
 * Model-driven storage engine.
 *
 * <p>Handles model-driven CRUD operations on resource managed by a specific storage backend.</p>
 *
 * <p>When acting as a wrapper, ensures that requests are handled on a single connection to the storage backend.</p>
 */
public interface Engine extends Wrapper {

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a throttler wrapper.
	 *
	 * @param task the accepted value for the {@linkplain Guard#Task task} parametric axis
	 * @param view the accepted values for the {@linkplain Guard#View task} parametric axis
	 *
	 * @return returns a wrapper performing role-based shape redaction and shape-based authorization
	 */
	public static Wrapper throttler(final Object task, final Object view) { // !!! optimize/cache
		return handler -> request -> {

			final Shape shape=request.attribute(shape()) // visible taking into account task/area

					.redact(Task, task)
					.redact(View, view)
					.redact(Mode, Convey);

			final Shape baseline=shape // visible to anyone

					.redact(Role);

			final Shape authorized=shape // visible to user

					.redact(Role, request.roles());

			// request shape redactor

			final UnaryOperator<Request> pre=message -> message.attribute(shape(), message.attribute(shape())

					.redact(Role, request.roles())
					.redact(Task, task)
					.redact(View, view)

			);

			// response shape redactor

			final UnaryOperator<Response> post=message -> message.attribute(shape(), message.attribute(shape())

					.redact(Role, request.roles())
					.redact(Task, task)
					.redact(View, view)
					.redact(Mode, Convey)

			);

			return baseline.empty() ? request.reply(status(Forbidden))
					: authorized.empty() ? request.reply(status(Unauthorized))
					: handler.handle(request.map(pre)).map(post);

		};
	}


	//// CRUD Operations ///////////////////////////////////////////////////////////////////////////////////////////////

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
	 * Handles browsing requests.
	 *
	 * <p>Handles browsing requests on the linked data container identified by the request {@linkplain Request#item()
	 * item} possibly using the message {@linkplain JSONLDFormat#shape() shape}.</p>
	 *
	 * @param request a browsing request for the managed linked data container
	 *
	 * @return a lazy response generated for the managed linked data container in reaction to the retrieval {@code
	 * request}
	 *
	 * @throws NullPointerException if {@code request} is null
	 */
	public Future<Response> browse(final Request request);

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

}
