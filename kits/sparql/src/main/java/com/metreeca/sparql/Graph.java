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

package com.metreeca.sparql;

import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.function.*;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.repository.util.Connections.getStatement;

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
	 * Retrieves the default graph factory.
	 *
	 * @return the default graph factory, which throws an exception reporting the tool as undefined
	 */
	public static Supplier<Graph> graph() {
		return () -> { throw new IllegalStateException("undefined graph tool"); };
	}


	/**
	 * Creates a sequential auto-incrementing slug generator.
	 *
	 * <p><strong>Warning</strong> / SPARQL doesn't natively support auto-incrementing ids: auto-incrementing slug
	 * calls are partly serialized in the system {@linkplain Graph#graph() graph} database using an internal lock
	 * object; this strategy may fail for distributed containers or external concurrent updates on the SPARQL endpoint,
	 * causing requests to fail with an {@link Response#InternalServerError} or {@link Response#Conflict} status
	 * code.</p>
	 *
	 * @return a slug generator returning an auto-incrementing numeric id unique to the focus item of the request
	 */
	public static BiFunction<Request, Collection<Statement>, String> auto() {
		return new AutoGenerator();
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
	public Graph exec(final Consumer<RepositoryConnection> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return exec(connection -> {

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
	public <V> V exec(final Function<RepositoryConnection, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( repository == null ) {
			throw new IllegalStateException("undefined repository");
		}

		final RepositoryConnection shared=context.get();

		if ( shared != null ) {

			return requireNonNull(task.apply(shared), "null task return value");

		} else {

			if ( !repository.isInitialized() ) { repository.init(); }

			try (final RepositoryConnection connection=repository.getConnection()) {

				context.set(connection);

				try {

					if ( !connection.isActive() ) { connection.begin(isolation); }

					final V value=requireNonNull(task.apply(connection), "null task return value");

					if ( connection.isActive() ) { connection.commit(); }

					return value;

				} catch ( final Throwable t ) {

					try { throw t; } finally {
						if ( connection.isActive() ) { connection.rollback(); }
					}

				}

			} finally {

				context.remove();

			}

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AutoGenerator implements BiFunction<Request, Collection<Statement>, String> {

		private static final IRI Auto=iri("app://rest.metreeca.com/terms#", "auto");


		private final Graph graph=tool(graph());


		@Override public String apply(final Request request, final Collection<Statement> model) {
			return graph.exec(connection -> {

				// !!! custom name pattern
				// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
				// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)

				final IRI stem=iri(request.stem());

				long id=getStatement(connection, stem, Auto, null)
						.map(Statement::getObject)
						.filter(value -> value instanceof Literal)
						.map(value -> {
							try {
								return ((Literal)value).longValue();
							} catch ( final NumberFormatException e ) {
								return 0L;
							}
						})
						.orElse(0L);

				IRI iri;

				do {

					iri=iri(stem.stringValue(), String.valueOf(++id));

				} while ( connection.hasStatement(iri, null, null, true)
						|| connection.hasStatement(null, null, iri, true) );

				connection.remove(stem, Auto, null);
				connection.add(stem, Auto, literal(id));

				return String.valueOf(id);

			});
		}
	}
}
