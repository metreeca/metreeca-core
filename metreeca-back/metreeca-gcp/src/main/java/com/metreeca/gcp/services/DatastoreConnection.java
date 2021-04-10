/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

import com.metreeca.json.Values;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.*;
import com.google.cloud.datastore.StructuredQuery.Builder;
import com.google.cloud.datastore.StructuredQuery.Filter;
import org.eclipse.rdf4j.common.iteration.CloseableIteratorIteration;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF4J;
import org.eclipse.rdf4j.query.Query;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.repository.base.AbstractRepositoryConnection;
import org.eclipse.rdf4j.rio.RDFHandler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.metreeca.json.Values.namespace;
import static com.metreeca.json.Values.statement;
import static com.metreeca.rest.Xtream.task;

import static com.google.cloud.datastore.StructuredQuery.CompositeFilter.and;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;
import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.hasAncestor;

final class DatastoreConnection extends AbstractRepositoryConnection {

	private static final int Layout=1;

	private static final String Repository="Repository";
	private static final String Namespace="Namespace";
	private static final String Statement="Statement";

	private static final String Name="name";
	private static final String Subject="subject";
	private static final String Predicate="predicate";
	private static final String Object="object";
	private static final String Datatype="datatype";
	private static final String Context="context";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Transaction transaction;

	private final Datastore datastore;
	private final ValueFactory factory;

	private final Key repository;
	private final PathElement root;


	DatastoreConnection(final DatastoreRepository repository) {

		super(repository);

		this.datastore=repository.getDatastore();
		this.factory=repository.getValueFactory();

		this.repository=datastore.newKeyFactory().setKind(Repository).newKey(repository.getName());
		this.root=PathElement.of(Repository, repository.getName());
	}


	void init() {

		final Transaction txn=datastore.newTransaction();

		try {

			txn.put(Optional
					.ofNullable(txn.get(repository))
					.map(FullEntity::newBuilder)
					.orElseGet(() -> FullEntity.newBuilder(repository))
					.set("layout", Layout) // !!! check version and update
					.build()
			);

			txn.commit();

		} finally {

			if ( txn.isActive() ) { txn.rollback(); }

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V> V exec(final Function<DatastoreReaderWriter, V> task) {
		return task.apply(transaction != null ? transaction : datastore);
	}


	private Stream<Resource> stream(final Resource fallback, final Resource... contexts) {
		return contexts.length == 0 ? Stream.of(fallback) : Arrays.stream(contexts);
	}

	private <T> Stream<T> stream(final Iterator<T> results) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(results, Spliterator.ORDERED), false);
	}


	//// Codecs ////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resource(final Resource resource) {
		return resource.isBNode() ? "_:"+resource.stringValue() // !!! uniqueness?
				: resource.isIRI() ? resource.stringValue() // !!! relative
				: unsupported(resource.getClass());
	}

	private Resource resource(final String resource) {
		return resource.startsWith("_:") ? factory.createBNode(resource.substring("_:".length())) // !!! uniqueness?
				: factory.createIRI(resource); // !!! relative
	}


	private String predicate(final IRI predicate) {
		return predicate.stringValue(); // !!! curies
	}

	private IRI predicate(final String predicate) {
		return factory.createIRI(predicate); // !!! curies
	}


	private String value(final Value value) {
		return value.stringValue();
	}

	private String datatype(final Value value) {
		return value.isBNode() ? Values.BNodeType.stringValue() // !!! uniqueness?
				: value.isIRI() ? Values.IRIType.stringValue()
				: value.isLiteral() ? datatype((Literal)value)
				: unsupported(value.getClass());
	}

	private String datatype(final Literal value) {
		return value.getLanguage().map(lang -> "@"+lang).orElseGet(() -> value.getDatatype().stringValue());
	}

	private Value value(final String value, final String datatype) {
		return datatype.equals(Values.BNodeType.stringValue()) ? factory.createBNode(value) // !!! uniqueness?
				: datatype.equals(Values.IRIType.stringValue()) ? factory.createIRI(value)
				: datatype.startsWith("@") ? factory.createLiteral(value, datatype.substring(1))
				: factory.createLiteral(value, factory.createIRI(datatype));
	}


	private <V> V unsupported(final Class<?> type) {
		throw new UnsupportedOperationException(String.format("unsupported type <%s>", type.getName()));
	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean isActive() {
		return transaction != null;
	}


	@Override public void begin() {

		if ( isActive() ) {
			throw new RepositoryException(new IllegalStateException("transaction already active"));
		}

		transaction=datastore.newTransaction();
	}

	@Override public void commit() {

		if ( !isActive() ) {
			throw new RepositoryException(new IllegalStateException("no active transaction"));
		}

		try { transaction.commit(); } finally { transaction=null; }
	}

	@Override public void rollback() {

		if ( !isActive() ) {
			throw new RepositoryException(new IllegalStateException("no active transaction"));
		}

		try { transaction.rollback(); } finally { transaction=null; }
	}


	//// Namespaces ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public RepositoryResult<Namespace> getNamespaces() {
		return new RepositoryResult<>(new CloseableIteratorIteration<>(exec(connection -> stream(connection.run(

				query(com.google.cloud.datastore.Query.newEntityQueryBuilder())

		)).map(entity -> namespace(

				entity.getKey().getName(),
				entity.getString(Name)

		))).iterator()));
	}

	@Override public String getNamespace(final String prefix) {

		if ( prefix == null ) {
			throw new NullPointerException("null prefix");
		}

		return exec((connection -> Optional
				.ofNullable(connection.get(key(prefix)))
				.map(entity -> entity.getString(Name))
				.orElse(null)
		));
	}


	@Override public void setNamespace(final String prefix, final String name) {

		if ( prefix == null ) {
			throw new NullPointerException("null prefix");
		}

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		exec(task(connection -> connection.put(FullEntity
				.newBuilder(key(prefix))
				.set(Name, name)
				.build()
		)));
	}

	@Override public void removeNamespace(final String prefix) {

		if ( prefix == null ) {
			throw new NullPointerException("null prefix");
		}

		exec(task(connection -> connection.delete(key(prefix))));
	}

	@Override public void clearNamespaces() {
		exec(task(connection -> connection.delete(stream(connection.run(

				query(com.google.cloud.datastore.Query.newKeyQueryBuilder())

		)).toArray(Key[]::new))));
	}


	private Key key(final String prefix) {
		return datastore
				.newKeyFactory()
				.setKind(Namespace)
				.addAncestor(root)
				.newKey(prefix);
	}

	private <T> StructuredQuery<T> query(final Builder<T> builder) {
		return builder
				.setKind(Namespace)
				.setFilter(hasAncestor(repository))
				.build();
	}


	//// Statements ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public RepositoryResult<Resource> getContextIDs() {
		return exec(connection -> new RepositoryResult<>(new CloseableIteratorIteration<>(stream(connection.run(

				com.google.cloud.datastore.Query
						.newProjectionEntityQueryBuilder()
						.setKind(Statement)
						.setFilter(hasAncestor(repository))
						.addProjection(Context)
						.setDistinctOn(Context)
						.build()

		)).map(entity ->

				resource(entity.getString(Context))

		).iterator())));
	}

	@Override public long size(final Resource... contexts) {
		return exec(connection -> stream(null, contexts).mapToLong(context -> datastore.run(com.google.cloud.datastore.Query

				.newKeyQueryBuilder()
				.setKind(Statement)
				.setFilter(context == null
						? hasAncestor(repository)
						: and(hasAncestor(repository), eq(Context, resource(context)))
				)
				.setOffset(Integer.MAX_VALUE)
				.build()

		).getSkippedResults()).sum());
	}


	@Override public RepositoryResult<Statement> getStatements(
			final Resource subject, final IRI predicate, final Value object,
			final boolean includeInferred, final Resource... contexts
	) {

		return new RepositoryResult<>(new CloseableIteratorIteration<>(stream(null, contexts).flatMap(context -> {

			final ProjectionEntityQuery.Builder builder=com.google.cloud.datastore.Query
					.newProjectionEntityQueryBuilder()
					.setKind(Statement)
					.setFilter(and(
							hasAncestor(repository),
							filters(subject, predicate, object, context)
					));

			if ( subject == null ) { builder.addProjection(Subject); }
			if ( predicate == null ) { builder.addProjection(Predicate); }
			if ( object == null ) { builder.addProjection(Object, Datatype); }
			if ( context == null ) { builder.addProjection(Context); }

			return exec(connection -> stream(connection.run(builder.build())).map(entity -> statement(

					subject != null ? subject : resource(entity.getString(Subject)),
					predicate != null ? predicate : predicate(entity.getString(Predicate)),
					object != null ? object : value(entity.getString(Object), entity.getString(Datatype)),
					context != null ? context : resource(entity.getString(Context))

			)));

		}).iterator()));

	}

	@Override public void exportStatements(
			final Resource subject, final IRI predicate, final Value object,
			final boolean includeInferred, final RDFHandler handler, final Resource... contexts
	) {

		handler.startRDF();

		getNamespaces().stream().forEach(namespace ->
				handler.handleNamespace(namespace.getPrefix(), namespace.getName())
		);

		getStatements(subject, predicate, object, includeInferred, contexts).stream().forEach(
				handler::handleStatement
		);

		handler.endRDF();

	}


	// !!! batch add/remove

	@Override protected void addWithoutCommit(
			final Resource subject, final IRI predicate, final Value object,
			final Resource... contexts
	) {
		exec(task(connection -> stream(RDF4J.NIL, contexts).forEach(context -> {

			final String s=resource(subject);
			final String p=predicate(predicate);
			final String o=value(object);
			final String d=datatype(object);
			final String c=resource(context);

			final KeyQuery builder=com.google.cloud.datastore.Query
					.newKeyQueryBuilder()
					.setKind(Statement)
					.setFilter(and(
							hasAncestor(repository),
							eq(Subject, s),
							eq(Predicate, p),
							eq(Object, o),
							eq(Datatype, d),
							eq(Context, c)
					))
					.setOffset(Integer.MAX_VALUE)
					.build();

			if ( connection.run(builder).getSkippedResults() == 0 ) {

				final IncompleteKey key=datastore
						.newKeyFactory()
						.setKind(Statement)
						.addAncestor(root)
						.newKey();

				connection.add(FullEntity.newBuilder(key)
						.set(Subject, s)
						.set(Predicate, p)
						.set(Object, o)
						.set(Datatype, d)
						.set(Context, c)
						.build()
				);
			}

		})));
	}

	@Override protected void removeWithoutCommit(
			final Resource subject, final IRI predicate, final Value object,
			final Resource... contexts
	) {
		stream(null, contexts).forEach(context -> exec(task(connection -> connection.delete(stream(connection.run(

				com.google.cloud.datastore.Query
						.newKeyQueryBuilder()
						.setKind(Statement)
						.setFilter(and(
								hasAncestor(repository),
								filters(subject, predicate, object, context)
						))
						.build()

		)).toArray(Key[]::new)))));
	}


	private Filter[] filters(final Resource subject, final IRI predicate, final Value object, final Resource context) {
		return Stream.of(

				subject == null ? null : eq(Subject, resource(subject)),
				predicate == null ? null : eq(Predicate, predicate(predicate)),
				object == null ? null : eq(Object, value(object)),
				object == null ? null : eq(Datatype, datatype(object)),
				context == null ? null : eq(Context, resource(context))

		).filter(Objects::nonNull).toArray(Filter[]::new);
	}


	//// Operations ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Query prepareQuery(final QueryLanguage ql, final String query, final String base) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public BooleanQuery prepareBooleanQuery(final QueryLanguage ql, final String query, final String base) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public TupleQuery prepareTupleQuery(final QueryLanguage ql, final String query, final String base) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public GraphQuery prepareGraphQuery(final QueryLanguage ql, final String query, final String base) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Update prepareUpdate(final QueryLanguage ql, final String update, final String base) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void close() throws RepositoryException {

		if ( transaction != null ) {
			try { transaction.rollback(); } finally { transaction=null; }
		}

		super.close();
	}

}
