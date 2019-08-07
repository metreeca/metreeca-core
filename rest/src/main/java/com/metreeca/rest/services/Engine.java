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

import java.util.function.Supplier;


/**
 * Model-driven storage engine.
 *
 * <p>Manages storage transactions and performs model-driven CRUD actions on resources and containers.</p>
 */
public interface Engine extends Handler {

	/**
	 * Retrieves the default engine factory.
	 *
	 * @return the default engine factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Engine> engine() {
		return () -> { throw new IllegalStateException("undefined engine service"); };
	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

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

}
