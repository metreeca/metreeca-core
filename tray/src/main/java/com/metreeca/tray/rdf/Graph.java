/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf;

import com.metreeca.tray.rdf.graphs.RDF4JMemory;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;


/**
 * Graph store.
 *
 * <p>Manages task execution on an RDF {@linkplain Repository repository}.</p>
 *
 * <p>Nested task executions on the same graph store from the same thread will share the same connection to the backing
 * RDF repository through a {@link ThreadLocal} context variable.</p>
 */
public abstract class Graph implements AutoCloseable {

	/**
	 * Graph factory.
	 *
	 * <p>By default creates a graph backed by a RDF4J Memory store with no persistence.</p>
	 */
	public static final Supplier<Graph> Factory=RDF4JMemory::new;


	private static final ThreadLocal<RepositoryConnection> context=new ThreadLocal<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the backing RDF repository.
	 *
	 * @return the backing RDF repository for this graph store; will be initializated and shut down as required by the
	 * calling code
	 */
	protected abstract Repository repository();

	/**
	 * Retrieves the transaction isolation level.
	 *
	 * @return the isolation level for transactions on connection managed by this graph store
	 */
	protected abstract IsolationLevel isolation();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void close() {

		final Repository repository=repository();

		if ( repository.isInitialized() ) {
			repository.shutDown();
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Executes a task on this graph store.
	 *
	 * @param task the task to be executed; takes as argument a connection to the backing repository of this graph
	 *             store
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public Graph query(final Consumer<RepositoryConnection> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(connection -> {

			task.accept(connection);

			return this;

		});
	}

	/**
	 * Executes a task on this graph store.
	 *
	 * @param task the task to be executed; takes as argument a connection to the backing repository of this graph
	 *             store
	 * @param <V>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <V> V query(final Function<RepositoryConnection, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(task);
	}


	/**
	 * Executes a task inside a transaction on this graph store.
	 *
	 * <p>If a transaction is not already active on the shared repository connection, begins one at the {@linkplain
	 * #isolation() isolation} level required by this store and commits it on successful task completion; if the task
	 * throws an exception, the transaction is rolled back and the exception rethrown; in either case,  no action is
	 * taken if the transaction was already terminated inside the task.</p>
	 *
	 * @param task the task to be executed; takes as argument a connection to the backing repository of this graph
	 *             store
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public Graph update(final Consumer<RepositoryConnection> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return update(connection -> {

			task.accept(connection);

			return this;

		});
	}

	/**
	 * Executes a task inside a transaction on this graph store.
	 *
	 * <p>If a transaction is not already active on the shared repository connection, begins one at the {@linkplain
	 * #isolation() isolation} level required by this store and commits it on successful task completion; if the task
	 * throws an exception, the transaction is rolled back and the exception rethrown; in either case no action is
	 * taken if the transaction was already closed inside the task.</p>
	 *
	 * @param task the task to be executed; takes as argument a connection to the backing repository of this graph
	 *             store
	 * @param <V>  the type of the value returned by {@code task}
	 *
	 * @return the value returned by {@code task}
	 *
	 * @throws NullPointerException if {@code task} is null or returns a null value
	 */
	public <V> V update(final Function<RepositoryConnection, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(connection -> {
			if ( connection.isActive() ) {

				return task.apply(connection);

			} else {

				try {

					if ( !connection.isActive() ) { connection.begin(); }

					final V value=task.apply(connection);

					if ( connection.isActive() ) { connection.commit(); }

					return value;

				} catch ( final Throwable t ) {

					try { throw t; } finally {
						if ( connection.isActive() ) { connection.rollback(); }
					}

				}

			}
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <R> R exec(final Function<RepositoryConnection, R> task) {

		final RepositoryConnection shared=context.get();

		if ( shared != null ) {

			return task.apply(shared);

		} else {

			final Repository repository=repository();

			if ( !repository.isInitialized() ) { repository.initialize(); }

			try (final RepositoryConnection connection=repository.getConnection()) {

				connection.setIsolationLevel(isolation());

				context.set(connection);

				return requireNonNull(task.apply(connection), "null task return value");

			} finally {

				context.remove();

			}

		}
	}

}
