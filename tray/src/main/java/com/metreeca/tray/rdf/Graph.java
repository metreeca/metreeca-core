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

package com.metreeca.tray.rdf;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryReadOnlyException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

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
	 * Read-only isolation level.
	 *
	 * <p>Disables update transactions.</p>
	 */
	public static final IsolationLevel READ_ONLY=new IsolationLevel() {

		@Override public boolean isCompatibleWith(final IsolationLevel level) { return equals(level); }

		@Override public IRI getURI() { return RDF.NIL; }

		@Override public String toString() { return "READ_ONLY"; }

	};


	/**
	 * Retrieves the default graph factory.
	 *
	 * @return the default graph factory, which creates graphs backed by a RDF4J Memory store with no persistence
	 */
	public static Supplier<Graph> graph() {
		return () -> new Graph() {{
			repository(new SailRepository(new MemoryStore()));
		}};
	}


	private static final ThreadLocal<RepositoryConnection> context=new ThreadLocal<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Repository repository;
	private IsolationLevel isolation=IsolationLevels.SNAPSHOT;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Retrieves the backing RDF repository.
	 *
	 * @return the backing RDF repository for this graph store; {@code null} if the backing RDF repository was not
	 * {@linkplain #repository(Repository) configured}
	 */
	protected Repository repository() {
		return repository;
	}

	/**
	 * Configures the backing RDF repository.
	 *
	 * @param repository the backing RDF repository for this graph store; will be initializated and shut down as
	 *                   required by the calling code
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code repository} is null
	 */
	protected Graph repository(final Repository repository) {

		if ( repository == null ) {
			throw new NullPointerException("null repository");
		}

		this.repository=repository;

		return this;
	}


	/**
	 * Retrieves the transaction isolation level.
	 *
	 * @return the isolation level for transactions on connection managed by this graph store; defaults to {@link
	 * IsolationLevels#SNAPSHOT} if not {@linkplain #isolation(IsolationLevel) configured}
	 */
	public IsolationLevel isolation() {
		return isolation;
	}

	/**
	 * Configures the transaction isolation level.
	 *
	 * @param isolation the isolation level for transactions on connection managed by this graph store
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code isolation} is null
	 */
	public Graph isolation(final IsolationLevel isolation) {

		if ( isolation == null ) {
			throw new NullPointerException("null isolation");
		}

		this.isolation=isolation;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void close() {
		try {

			if ( repository != null && repository.isInitialized() ) { repository.shutDown(); }

		} finally {

			repository=null;

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
	 * #isolation(IsolationLevel) isolation} level required by this store and commits it on successful task completion;
	 * if the task throws an exception, the transaction is rolled back and the exception rethrown; in either case,  no
	 * action is taken if the transaction was already terminated inside the task.</p>
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
	 * #isolation(IsolationLevel) isolation} level required by this store and commits it on successful task completion;
	 * if the task throws an exception, the transaction is rolled back and the exception rethrown; in either case no
	 * action is taken if the transaction was already closed inside the task.</p>
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

				if ( connection.getIsolationLevel().equals(READ_ONLY) ) {
					throw new RepositoryReadOnlyException("isolation level set to "+READ_ONLY);
				}

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

	private <V> V exec(final Function<RepositoryConnection, V> task) {

		final RepositoryConnection shared=context.get();

		if ( shared != null ) {

			return task.apply(shared);

		} else {

			if ( repository == null ) {
				throw new IllegalStateException("undefined repository");
			}

			if ( !repository.isInitialized() ) { repository.init(); }

			try (final RepositoryConnection connection=repository.getConnection()) {

				connection.setIsolationLevel(isolation);

				context.set(connection);

				return requireNonNull(task.apply(connection), "null task return value");

			} finally {

				context.remove();

			}

		}
	}

}
