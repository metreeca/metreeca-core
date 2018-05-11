/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf;

import com.metreeca.jeep.rdf.Cell;
import com.metreeca.spec.Issue.Level;
import com.metreeca.spec.Query;
import com.metreeca.spec.Report;
import com.metreeca.spec.Shape;
import com.metreeca.spec.sparql.SPARQLEngine;
import com.metreeca.spec.sparql.SPARQLWriter;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.graphs.*;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.base.RepositoryConnectionWrapper;

import java.io.File;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.jeep.Jeep.concat;
import static com.metreeca.spec.Issue.issue;
import static com.metreeca.spec.Report.trace;
import static com.metreeca.spec.shapes.And.and;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;


/**
 * Graph store.
 */
public abstract class Graph implements AutoCloseable {

	public static final Tool<Graph> Tool=tools -> {

		final Setup setup=tools.get(Setup.Tool);
		final String type=setup.get("graph", "memory");

		switch ( type ) {

			case "memory":

				return RDF4JMemory.Tool.create(tools);

			case "native":

				return RDF4JNative.Tool.create(tools);

			case "remote":

				return RDF4JRemote.Tool.create(tools);

			case "sparql":

				return SPARQL.Tool.create(tools);

			case "virtuoso":

				return Virtuoso.Tool.create(tools);

			default:

				throw new UnsupportedOperationException("unknown graph type ["+type+"]");

		}
	};


	private static final ThreadLocal<RepositoryConnection> connection=new ThreadLocal<>();


	protected static File storage(final Setup setup) {
		return new File(Setup.storage(setup), "graph");
	}


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


	public String info() { // !!! rename as info()
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

	public boolean isTransactional() { // !!! replace with test on connection and remove
		return !isolation.equals(IsolationLevels.NONE);
	}


	public Graph map(final String external, final String internal) {

		if ( external == null ) {
			throw new NullPointerException("null external");
		}

		if ( internal == null ) {
			throw new NullPointerException("null internal");
		}

		return external.isEmpty() || internal.isEmpty() || external.equals(internal) ? this : new MappingGraph(
				external, internal, info, isolation, () -> repository
		);
	}


	public boolean contains(final Resource resource) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		return browse(connection -> SPARQLEngine.contains(connection, resource));
	}

	public Cell get(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return get(new com.metreeca.spec.queries.Graph(shape));
	}

	public Cell get(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return browse(connection -> SPARQLEngine._browse(connection, query));
	}


	public Report set(final Shape shape, final Cell cell) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( cell == null ) {
			throw new NullPointerException("null cell");
		}

		return update(connection -> {

			final boolean transactional=isTransactional(); // disable shape-driven validation if false // !!! just downgrade

			// upload statements to repository and validate against shape

			final Report report=new SPARQLWriter(connection).process(transactional ? shape : and(), cell);

			// validate shape envelope

			final Collection<Statement> envelope=report.outline();

			final Collection<Statement> outliers=transactional ? cell.model().stream()
					.filter(statement -> !envelope.contains(statement))
					.collect(toList()) : emptySet();

			// extend validation report with statements outside shape envelope

			final Report extended=outliers.isEmpty() ? report : trace(concat(report.getIssues(), outliers.stream()
					.map(outlier -> issue(Level.Error, "unexpected statement "+outlier, shape))
					.collect(toList())
			), report.getFrames());

			// drop outlining frames for better readability

			final Report pruned=extended.prune(Level.Info).orElseGet(Report::trace);

			// log warnings and errors // !!! here?

			if ( pruned.assess(Level.Warning) ) {
				// !!! convert report to log records
				// !!! factor with other com.metreeca.next.handlers
			}

			return pruned;

		});
	}


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

				// !!! restrict supplied connection to guard against third-party code (e.g. no access to repository)

				return task.apply(connection);

			} finally {
				connection.remove();
			}

		}
	}

}
