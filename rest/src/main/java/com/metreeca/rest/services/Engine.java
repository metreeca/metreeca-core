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

package com.metreeca.rest.services;

import com.metreeca.rest.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.Trace;

import java.util.function.Supplier;


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


	//// Shape Splitting ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Extract the container shape from a combo container/resource shape.
	 *
	 * @param shape the (possibly) combined container/resource shape to be processed
	 *
	 * @return the container section extracted from {@code shape}, if one is presente, or {@code shape}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Shape container(final Shape shape);

	/**
	 * Extract the resource shape from a combo container/resource shape.
	 *
	 * @param shape the (possibly) combined container/resource shape to be processed
	 *
	 * @return the resource section extracted from {@code shape}, if one is present, or {@code shape}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Shape resource(final Shape shape);


	//// Payload Management ////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Trims the message payload.
	 *
	 * <p>Rewrites the engine-specific message {@linkplain Message#body(Format) payload} retaining only the subset
	 * compatible with the envelope of the message {@linkplain Message#shape() shape}.</p>
	 *
	 * @param message the message whose engine-specific payload is to be trimmed
	 * @param <M>     the type of {@code message}
	 *
	 * @return a value providing access to the given {@code message} with an updated payload, if its engine-specific
	 * {@linkplain Message#body(Format) payload} is well-formed and compatible with its {@linkplain Message#shape()
	 * shape}; an error providing access to a failure possibly {@linkplain Failure#trace(Trace) containing} a validation
	 * trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Result<M, Failure> trim(final M message);

	/**
	 * Validates the message payload.
	 *
	 * <p>Validates the engine-specific message {@linkplain Message#body(Format) payload} against the message
	 * {@linkplain Message#shape() shape}.</p>
	 *
	 * @param <M>     the type of {@code message}
	 * @param message the message whose engine-specific payload is to be validated
	 *
	 * @return a value providing access to {@code message}, if its engine-specific {@linkplain Message#body(Format)
	 * payload} is well-formed and compatible with its {@linkplain Message#shape() shape}; an error providing access to
	 * a failure possibly {@linkplain Failure#trace(Trace) containing} a validation trace, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public <M extends Message<M>> Result<M, Failure> validate(final M message);


	//// CRUD Actions //////////////////////////////////////////////////////////////////////////////////////////////////

	public Future<Response> create(final Request request); // !!! tbd

	public Future<Response> relate(final Request request); // !!! tbd

	public Future<Response> update(final Request request); // !!! tbd

	public Future<Response> delete(final Request request); // !!! tbd

}
