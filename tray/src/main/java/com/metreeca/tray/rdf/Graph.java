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
import com.metreeca.tray.sys._Setup;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/**
 * Graph store.
 */
public abstract class Graph implements AutoCloseable {

	public static final Supplier<Graph> Factory=RDF4JMemory::new;


	private static final ThreadLocal<RepositoryConnection> context=new ThreadLocal<>();


	protected static File storage(final _Setup setup) {
		return new File(_Setup.storage(setup), "graph");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IsolationLevel isolation;
	private final Repository repository;


	protected Graph(final IsolationLevel isolation, final Supplier<Repository> repository) {

		if ( isolation == null ) {
			throw new NullPointerException("null isolation");
		}

		if ( repository == null ) {
			throw new NullPointerException("null repository");
		}

		this.isolation=isolation;
		this.repository=repository.get();
	}


	@Override public void close() {
		repository.shutDown();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Graph browse(final Consumer<RepositoryConnection> browser) {

		if ( browser == null ) {
			throw new NullPointerException("null browser");
		}

		return exec(connection -> {

			browser.accept(connection);
			return this;

		});
	}

	public <R> R browse(final Function<RepositoryConnection, R> browser) {

		if ( browser == null ) {
			throw new NullPointerException("null browser");
		}

		return exec(browser);
	}


	public Graph update(final Consumer<RepositoryConnection> updater) {

		if ( updater == null ) {
			throw new NullPointerException("null updater");
		}

		return update(connection -> {

			updater.accept(connection);

			return this;

		});
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

					if ( !connection.isActive() ) { connection.begin(isolation); }

					final R value=updater.apply(connection);

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


	private <R> R exec(final Function<RepositoryConnection, R> task) {

		final RepositoryConnection shared=context.get();

		if ( shared != null ) {

			return task.apply(shared);

		} else {

			if ( !repository.isInitialized() ) {
				repository.initialize();
			}

			try (final RepositoryConnection connection=repository.getConnection()) {

				Graph.context.set(connection);

				return task.apply(connection);

			} finally {

				context.remove();

			}

		}
	}

}
