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

import com.metreeca.tray.rdf.graphs.*;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;

import java.io.File;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;


/**
 * Graph store.
 */
public abstract class Graph implements AutoCloseable {

	public static final Supplier<Graph> Factory=() -> {

		final Setup setup=tool(Setup.Factory);
		final String type=setup.get("graph", "memory");

		switch ( type ) {

			case "memory":

				return RDF4JMemory.Factory.get();

			case "native":

				return RDF4JNative.Factory.get();

			case "remote":

				return RDF4JRemote.Factory.get();

			case "sparql":

				return SPARQL.Factory.get();

			case "virtuoso":

				return Virtuoso.Factory.get();

			default:

				throw new UnsupportedOperationException("unknown graph type ["+type+"]");

		}
	};


	private static final ThreadLocal<RepositoryConnection> connection=new ThreadLocal<>();


	protected static File storage(final Setup setup) {
		return new File(Setup.storage(setup), "graph");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String info;

	private final IsolationLevel isolation;
	private final Repository repository;


	protected Graph(final String info, final IsolationLevel isolation, final Supplier<Repository> repository) {

		if ( info == null ) {
			throw new NullPointerException("null description");
		}

		if ( info.isEmpty() ) {
			throw new IllegalArgumentException("empty description");
		}

		if ( isolation == null ) {
			throw new NullPointerException("null isolation");
		}

		if ( repository == null ) {
			throw new NullPointerException("null repository");
		}

		this.info=info;

		this.isolation=isolation;
		this.repository=repository.get();
	}


	public String info() {
		return info;
	}


	@Override public void close() {
		repository.shutDown();
	}


	public RepositoryConnection connect() {

		final RepositoryConnection connection=Graph.connection.get();

		if ( connection != null ) {

			return new RepositoryConnectionWrapper(repository, connection) {

				@Override public void close() throws RepositoryException {}

			};

		} else {

			if ( !repository.isInitialized() ) {
				repository.initialize();
			}

			final RepositoryConnection wrapper=new RepositoryConnectionWrapper(repository, repository.getConnection()) {

				@Override public void close() throws RepositoryException {
					try { super.close(); } finally { Graph.connection.remove(); }
				}

			};

			wrapper.setIsolationLevel(isolation);

			Graph.connection.set(wrapper); // !!! ThreadLocal removal relies on connection being closed… review

			return wrapper;

		}
	}

	public RepositoryConnection connect(final IsolationLevel isolation) {

		if ( isolation == null ) {
			throw new NullPointerException("null isolation");
		}

		final RepositoryConnection connection=connect();

		connection.setIsolationLevel(isolation);

		return connection;
	}


	//// !!! Legacy API ////////////////////////////////////////////////////////////////////////////////////////////////

	public <R> R browse(final Function<RepositoryConnection, R> browser) {

		if ( browser == null ) {
			throw new NullPointerException("null browser");
		}

		return exec(browser);
	}

	public <R> R update(final Function<RepositoryConnection, R> updater) {

		if ( updater == null ) {
			throw new NullPointerException("null updater");
		}

		return exec(isolation.equals(IsolationLevels.NONE) ? updater : connection -> {
			if ( connection.isActive() ) {

				return updater.apply(connection);

			} else {

				try {

					connection.begin(isolation);

					final R value=updater.apply(connection);

					connection.commit();

					return value;

				} catch ( final Throwable t ) {

					try { throw t; } finally { connection.rollback(); }

				}

			}
		});
	}


	private <R> R exec(final Function<RepositoryConnection, R> task) {

		final RepositoryConnection shared=connection.get();

		if ( shared != null ) {

			return task.apply(shared);

		} else {

			if ( !repository.isInitialized() ) {
				repository.initialize();
			}

			try (final RepositoryConnection connection=repository.getConnection()) {

				Graph.connection.set(connection);

				return task.apply(connection);

			} finally {
				connection.remove();
			}

		}
	}

}
