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

package com.metreeca.rest.services;

import com.metreeca.rest.*;

import java.util.function.Supplier;
import java.util.function.UnaryOperator;


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


	//// Transaction Management
	// //////////////////////////////////////////////////////////////////////////////////////

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


	//// Payload Management
	// //////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Trims message payloads.
	 *
	 * <p>Rewrites the engine-specific message {@linkplain Message#body(Format) payload} retaining only the subset
	 * compatible with the envelope of the message {@linkplain Message#shape() shape}.</p>
	 *
	 * @param message the message whose engine-specific payload is to be trimmed
	 * @param <M>     the type of {@code message}
	 *
	 * @return a value providing access to the given {@code message} with an updated payload, if its engine-specific
	 * {@linkplain Message#body(Format) payload} is well-formed and compatible with its {@linkplain Message#shape()
	 * shape}; an error providing access to a failure response builder possibly reporting a validation trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Result<M, UnaryOperator<Response>> trim(final M message);

	/**
	 * Validates message payloads.
	 *
	 * <p>Validates the engine-specific message {@linkplain Message#body(Format) payload} against the message
	 * {@linkplain Message#shape() shape}.</p>
	 *
	 * @param <M>     the type of {@code message}
	 * @param message the message whose engine-specific payload is to be validated
	 *
	 * @return a value providing access to {@code message}, if its engine-specific {@linkplain Message#body(Format)
	 * payload} is well-formed and compatible with its {@linkplain Message#shape() shape}; an error providing access
	 * to a failure response builder possibly reporting a validation trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Result<M, UnaryOperator<Response>> validate(final M message);


	//// CRUD Actions
	// ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Handles creation requests.
	 *
	 * <p>Handles creation requests on the linked data resource identified by the request {@linkplain Request#item()
	 * item} possibly using an engine-specific request {@linkplain Message#body(Format) payload} and the message
	 * {@linkplain Message#shape() shape}.</p>
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
	 * item} possibly using the message {@linkplain Message#shape() shape}.</p>
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
	 * {@linkplain Message#shape() shape}.</p>
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
	 * item} possibly using  the message {@linkplain Message#shape() shape}.</p>
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
