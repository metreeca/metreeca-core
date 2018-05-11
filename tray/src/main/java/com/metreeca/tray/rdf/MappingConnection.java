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

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.common.iteration.Iteration;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.repository.util.RDFLoader;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.*;
import java.net.URL;
import java.util.function.BiConsumer;


final class MappingConnection implements RepositoryConnection {

	private final _Mapping mapping;
	private final MappingRepository repository;

	private final RepositoryConnection connection;


	MappingConnection(final _Mapping mapping, final MappingRepository repository, final RepositoryConnection connection) {
		this.mapping=mapping;
		this.repository=repository;
		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Repository getRepository() {
		return repository;
	}

	@Override public void setParserConfig(final ParserConfig config) {
		connection.setParserConfig(config);
	}

	@Override public ParserConfig getParserConfig() {
		return connection.getParserConfig();
	}

	@Override public ValueFactory getValueFactory() {
		return connection.getValueFactory();
	}

	@Override public boolean isOpen() throws RepositoryException {
		return connection.isOpen();
	}

	@Override public void close() throws RepositoryException {
		connection.close();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Query prepareQuery(final String query) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareQuery(mapping.internal(QueryLanguage.SPARQL, query)));
	}

	@Override public Query prepareQuery(
			final QueryLanguage ql, final String query
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareQuery(ql, mapping.internal(ql, query)));
	}

	@Override public Query prepareQuery(
			final QueryLanguage ql, final String query, final String baseURI
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareQuery(ql, mapping.internal(ql, query), mapping.internal(baseURI)));
	}


	@Override public BooleanQuery prepareBooleanQuery(final String query) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareBooleanQuery(mapping.internal(QueryLanguage.SPARQL, query)));
	}

	@Override public BooleanQuery prepareBooleanQuery(
			final QueryLanguage ql, final String query
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareBooleanQuery(ql, mapping.internal(ql, query)));
	}

	@Override public BooleanQuery prepareBooleanQuery(
			final QueryLanguage ql, final String query, final String baseURI
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareBooleanQuery(ql, mapping.internal(ql, query), mapping.internal(baseURI)));
	}


	@Override public TupleQuery prepareTupleQuery(final String query) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareTupleQuery(mapping.internal(QueryLanguage.SPARQL, query)));
	}

	@Override public TupleQuery prepareTupleQuery(
			final QueryLanguage ql, final String query
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareTupleQuery(ql, mapping.internal(ql, query)));
	}

	@Override public TupleQuery prepareTupleQuery(
			final QueryLanguage ql, final String query, final String baseURI
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareTupleQuery(ql, mapping.internal(ql, query), mapping.internal(baseURI)));
	}


	@Override public GraphQuery prepareGraphQuery(final String query) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareGraphQuery(mapping.internal(QueryLanguage.SPARQL, query)));
	}

	@Override public GraphQuery prepareGraphQuery(
			final QueryLanguage ql, final String query
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareGraphQuery(ql, mapping.internal(ql, query)));
	}

	@Override public GraphQuery prepareGraphQuery(
			final QueryLanguage ql, final String query, final String baseURI
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareGraphQuery(ql, mapping.internal(ql, query), mapping.internal(baseURI)));
	}


	@Override public Update prepareUpdate(final String update) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareUpdate(mapping.internal(QueryLanguage.SPARQL, update)));
	}

	@Override public Update prepareUpdate(
			final QueryLanguage ql, final String update
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareUpdate(ql, mapping.internal(ql, update)));
	}

	@Override public Update prepareUpdate(
			final QueryLanguage ql, final String update, final String baseURI
	) throws RepositoryException, MalformedQueryException {
		return mapping.external(connection.prepareUpdate(ql, mapping.internal(ql, update), mapping.internal(baseURI)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public long size(final Resource... contexts) throws RepositoryException {
		return connection.size(mapping.internal(contexts));
	}

	@Override public boolean isEmpty() throws RepositoryException {
		return connection.isEmpty();
	}


	@Override public RepositoryResult<Resource> getContextIDs() throws RepositoryException {
		return mapping.external(mapping::external, connection.getContextIDs());
	}


	@Override public boolean hasStatement(
			final Resource subj, final IRI pred, final Value obj,
			final boolean includeInferred, final Resource... contexts
	) throws RepositoryException {
		return connection.hasStatement(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj),
				includeInferred, mapping.internal(contexts));
	}

	@Override public boolean hasStatement(
			final Statement st, final boolean includeInferred, final Resource... contexts
	) throws RepositoryException {
		return connection.hasStatement(mapping.internal(st), includeInferred, mapping.internal(contexts));
	}

	@Deprecated @Override public boolean hasStatement(
			final Resource subj, final URI pred, final Value obj,
			final boolean includeInferred, final Resource... contexts) throws RepositoryException {
		return connection.hasStatement(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj),
				includeInferred, mapping.internal(contexts));
	}


	@Override public RepositoryResult<Statement> getStatements(
			final Resource subj, final IRI pred, final Value obj,
			final boolean includeInferred, final Resource... contexts
	) throws RepositoryException {
		return mapping.external(mapping::external, connection.getStatements(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj),
				includeInferred, mapping.internal(contexts)));
	}

	@Override public RepositoryResult<Statement> getStatements(
			final Resource subj, final IRI pred, final Value obj, final Resource... contexts
	) throws RepositoryException {
		return mapping.external(mapping::external, connection.getStatements(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj), mapping.internal(contexts)));
	}

	@Deprecated @Override public RepositoryResult<Statement> getStatements(
			final Resource subj, final URI pred, final Value obj,
			final boolean includeInferred, final Resource... contexts
	) throws RepositoryException {
		return mapping.external(mapping::external, connection.getStatements(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj),
				includeInferred, mapping.internal(contexts)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void add(
			final Resource subject, final IRI predicate, final Value object, final Resource... contexts
	) throws RepositoryException {
		connection.add(
				mapping.internal(subject), mapping.internal(predicate), mapping.internal(object),
				mapping.internal(contexts));
	}

	@Override public void add(final Statement st, final Resource... contexts) throws RepositoryException {
		connection.add(mapping.internal(st), mapping.internal(contexts));
	}

	@Override public void add(
			final Iterable<? extends Statement> statements, final Resource... contexts
	) throws RepositoryException {

		connection.add(mapping.internal(statements), mapping.internal(contexts));
	}

	@Override public <E extends Exception> void add(
			final Iteration<? extends Statement, E> statements, final Resource... contexts
	) throws RepositoryException, E {
		connection.add(mapping.internal(statements), mapping.internal(contexts));
	}

	@Deprecated @Override public void add(
			final Resource subject, final URI predicate, final Value object, final Resource... contexts
	) throws RepositoryException {
		connection.add(
				mapping.internal(subject), mapping.internal(predicate), mapping.internal(object),
				mapping.internal(contexts));
	}


	@Override public void remove(
			final Resource subject, final IRI predicate, final Value object, final Resource... contexts
	) throws RepositoryException {
		connection.remove(
				mapping.internal(subject), mapping.internal(predicate), mapping.internal(object),
				mapping.internal(contexts));
	}

	@Override public void remove(final Statement st, final Resource... contexts) throws RepositoryException {
		connection.remove(mapping.internal(st), mapping.internal(contexts));
	}

	@Override public void remove(
			final Iterable<? extends Statement> statements, final Resource... contexts
	) throws RepositoryException {
		connection.remove(mapping.internal(statements), mapping.internal(contexts));
	}

	@Override public <E extends Exception> void remove(
			final Iteration<? extends Statement, E> statements, final Resource... contexts
	) throws RepositoryException, E {
		connection.remove(mapping.internal(statements), mapping.internal(contexts));
	}

	@Deprecated @Override public void remove(
			final Resource subject, final URI predicate, final Value object, final Resource... contexts
	) throws RepositoryException {
		connection.remove(
				mapping.internal(subject), mapping.internal(predicate), mapping.internal(object),
				mapping.internal(contexts));
	}


	@Override public void clear(final Resource... contexts) throws RepositoryException {
		connection.clear(mapping.internal(contexts));
	}


	//// !!! will break if parsing/formatting is handled by delegate connection ////////////////////////////////////////

	@Override public void export(
			final RDFHandler handler, final Resource... contexts
	) throws RepositoryException, RDFHandlerException {
		connection.export(mapping.external(handler), mapping.internal(contexts));
	}

	@Override public void exportStatements(
			final Resource subj, final IRI pred, final Value obj,
			final boolean includeInferred, final RDFHandler handler, final Resource... contexts
	) throws RepositoryException, RDFHandlerException {
		connection.exportStatements(
				mapping.internal(subj), mapping.internal(pred), mapping.internal(obj),
				includeInferred, mapping.external(handler), mapping.internal(contexts));
	}


	@Override public void add(
			final InputStream in, final String baseURI, final RDFFormat dataFormat, final Resource... contexts
	) throws IOException, RDFParseException, RepositoryException {
		add(contexts, (loader, handler) -> {
			try {
				loader.load(in, mapping.internal(baseURI), dataFormat, handler);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Override public void add(
			final Reader reader, final String baseURI, final RDFFormat dataFormat, final Resource... contexts
	) throws IOException, RDFParseException, RepositoryException {
		add(contexts, (loader, handler) -> {
			try {
				loader.load(reader, mapping.internal(baseURI), dataFormat, handler);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Override public void add(
			final URL url, final String baseURI, final RDFFormat dataFormat, final Resource... contexts
	) throws IOException, RDFParseException, RepositoryException {
		add(contexts, (loader, handler) -> {
			try {
				loader.load(url, mapping.internal(baseURI), dataFormat, handler);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	@Override public void add(
			final File file, final String baseURI, final RDFFormat dataFormat, final Resource... contexts
	) throws IOException, RDFParseException, RepositoryException {
		add(contexts, (loader, handler) -> {
			try {
				loader.load(file, mapping.internal(baseURI), dataFormat, handler);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	private void add(final Resource[] contexts, final BiConsumer<RDFLoader, RDFHandler> sink) throws IOException {
		final boolean txn=!connection.isActive();

		try {

			if ( txn ) { connection.begin(); }

			sink.accept(new RDFLoader(getParserConfig(), getValueFactory()), new AbstractRDFHandler() {
				@Override public void handleStatement(final Statement st) throws RDFHandlerException {
					if ( contexts == null || contexts.length == 0 ) {
						connection.add(
								mapping.internal(st.getSubject()),
								mapping.internal(st.getPredicate()),
								mapping.internal(st.getObject()),
								mapping.internal(st.getContext()));
					} else {
						connection.add(
								mapping.internal(st.getSubject()),
								mapping.internal(st.getPredicate()),
								mapping.internal(st.getObject()),
								mapping.internal(contexts));
					}
				}
			});

			if ( txn ) { connection.commit(); }

		} catch ( final UncheckedIOException e ) {

			if ( txn ) { connection.rollback(); }

			throw e.getCause();

		} catch ( final Exception e ) {

			if ( txn ) { connection.rollback(); }

			throw e;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public RepositoryResult<Namespace> getNamespaces() throws RepositoryException {
		return mapping.external(mapping::external, connection.getNamespaces());
	}

	@Override public String getNamespace(final String prefix) throws RepositoryException {
		return mapping.external(connection.getNamespace(prefix));
	}

	@Override public void setNamespace(final String prefix, final String name) throws RepositoryException {
		connection.setNamespace(prefix, mapping.internal(name));
	}

	@Override public void removeNamespace(final String prefix) throws RepositoryException {
		connection.removeNamespace(prefix);
	}

	@Override public void clearNamespaces() throws RepositoryException {
		connection.clearNamespaces();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Deprecated @Override public void setAutoCommit(final boolean autoCommit) throws RepositoryException {
		connection.setAutoCommit(autoCommit);
	}

	@Deprecated @Override public boolean isAutoCommit() throws RepositoryException {
		return connection.isAutoCommit();
	}

	@Override public boolean isActive() throws UnknownTransactionStateException, RepositoryException {
		return connection.isActive();
	}

	@Override public void setIsolationLevel(final IsolationLevel level) throws IllegalStateException {
		connection.setIsolationLevel(level);
	}

	@Override public IsolationLevel getIsolationLevel() {
		return connection.getIsolationLevel();
	}

	@Override public void begin() throws RepositoryException {
		connection.begin();
	}

	@Override public void begin(final IsolationLevel level) throws RepositoryException {
		connection.begin(level);
	}

	@Override public void commit() throws RepositoryException {
		connection.commit();
	}

	@Override public void rollback() throws RepositoryException {
		connection.rollback();
	}

}
