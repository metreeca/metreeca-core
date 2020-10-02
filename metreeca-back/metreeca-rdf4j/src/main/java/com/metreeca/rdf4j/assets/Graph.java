/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.assets;

import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.RepositoryWrapper;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.time.Instant;
import java.util.*;
import java.util.function.*;

import static com.metreeca.json.Values.*;
import static java.util.Objects.requireNonNull;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;
import static org.eclipse.rdf4j.repository.util.Connections.getStatement;


/**
 * Graph store.
 *
 * <p>Manages task execution on an RDF {@linkplain Repository repository}.</p>
 *
 * <p>Nested task executions on the same graph store from the same thread will share the same connection to the backing
 * RDF repository through a {@link ThreadLocal} context variable.</p>
 */
public final class Graph implements AutoCloseable {

	/**
	 * Retrieves the default graph factory.
	 *
	 * @return the default graph factory, which creates graphs with a dummy wrapper with no defined delegate as
	 * backing repository
	 */
	public static Supplier<Graph> graph() {
		return () -> new Graph(new RepositoryWrapper());
	}


	//// Graph-Based Functions ////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Connects a graph-based supplier.
	 *
	 * @param supplier the graph-based supplier
	 * @param <R>      the type of the value generated by {@code supplier}
	 *
	 * @return a plain supplier that creates a read-only on demand connection to the to shared system {@linkplain
	 * #graph() Graph} and delegates value generation to the connected graph-based {@code supplier}
	 *
	 * @throws NullPointerException if {@code supplier} is null
	 */
	public static <R> Supplier<R> connect(final Function<RepositoryConnection, R> supplier) {

		if ( supplier == null ) {
			throw new NullPointerException("null supplier");
		}

		final Graph graph=Context.asset(graph());

		return () -> graph.exec(supplier);
	}

	/**
	 * Connects a graph-based function.
	 *
	 * @param function the graph-based function
	 * @param <V>      the type of the argument accepted by {@code function}
	 * @param <R>      the type of the value returned by {@code function}
	 *
	 * @return a plain function that creates a read-only on demand connection to the to shared system {@linkplain
	 * #graph() Graph} and delegates processing to the connected graph-based {@code function}
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V, R> Function<V, R> connect(final BiFunction<RepositoryConnection, V, R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		final Graph graph=Context.asset(graph());

		return v -> graph.exec(connection -> { return function.apply(connection, v); });
	}


	/**
	 * Creates a SPARQL query message filter.
	 *
	 * @param query       the SPARQL graph query (describe/construct) to be executed by the new filter on target
	 *                    messages; empty scripts are ignored
	 * @param customizers optional custom configuration setters for the SPARQL query operation
	 * @param <M>         the type of the target message for the new filter
	 *
	 * @return a message filter executing the SPARQL graph {@code query} on target messages with {@linkplain
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom
	 * configurations;
	 * returns the input model extended with the statements returned by {@code query}
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>> BiFunction<M, Collection<Statement>, Collection<Statement>> query(
			final String query, final BiConsumer<M, GraphQuery>... customizers
	) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( customizers == null || Arrays.stream(customizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null customizers");
		}

		final Graph graph=Context.asset(graph());

		return query.isEmpty() ? (message, model) -> model : (message, model) -> graph.exec(connection -> {

			configure(
					message, connection.prepareGraphQuery(SPARQL, query, message.request().base()), customizers
			).evaluate(
					new StatementCollector(model)
			);

			return model;

		});
	}

	/**
	 * Creates a SPARQL update housekeeping task.
	 *
	 * @param update      the SPARQL update script to be executed by the new housekeeping filter on target messages;
	 *                    empty scripts are ignored
	 * @param customizers optional custom configuration setters for the SPARQL update operation
	 * @param <M>         the type of the target message for the new filter
	 *
	 * @return a housekeeping task executing the SPARQL {@code update} script on target messages with {@linkplain
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom
	 * configurations;
	 * returns the
	 * input message without altering it
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>> Function<M, M> update(
			final String update, final BiConsumer<M, Update>... customizers
	) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		if ( customizers == null || Arrays.stream(customizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null customizers");
		}

		final Graph graph=Context.asset(graph());

		return update.isEmpty() ? message -> message : message -> graph.exec(connection -> {

			configure(message, connection.prepareUpdate(SPARQL, update, message.request().base()), customizers).execute();

			return message;

		});
	}

	/**
	 * Configures standard bindings for SPARQL message filters.
	 *
	 * <p>Configures the following pre-defined bindings for the target SPARQL operation:</p>
	 *
	 * <table summary="pre-defined bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>{@code ?time}</td>
	 * <td>an {@code xsd:dateTime} literal representing the execution system time with millisecond precision</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?this}</td>
	 * <td>the {@linkplain Message#item() focus item} of the filtered message</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?stem}</td>
	 * <td>the {@linkplain IRI#getNamespace() namespace} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?name}</td>
	 * <td>the local {@linkplain IRI#getLocalName() name} of the IRI bound to the {@code this} variable</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?task}</td>
	 * <td>the HTTP {@linkplain Request#method() method} of the original request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?base}</td>
	 * <td>the {@linkplain Request#base() base} IRI of the original request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?item}</td>
	 * <td>the {@linkplain Message#item() focus item} of the original request</td>
	 * </tr>
	 *
	 * <tr>
	 * <td>{@code ?user}</td>
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the original
	 * request or
	 * {@linkplain RDF#NIL} if no user is authenticated</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * <p>If the target message is a {@linkplain Response response}, the following additional
	 * bindings are
	 * configured:</p>
	 *
	 * <table summary="response bindings">
	 *
	 * <thead>
	 *
	 * <tr>
	 * <th>variable</th>
	 * <th>value</th>
	 * </tr>
	 *
	 * </thead>
	 *
	 * <tbody>
	 *
	 * <tr>
	 * <td>{@code ?code}</td>
	 * <td>the HTTP {@linkplain Response#status() status code} of the filtered response</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * @param message     the message to be filtered
	 * @param operation   the SPARQL operation executed by the filter
	 * @param customizers optional custom configuration setters for the SPARQL {@code operation}
	 * @param <M>         the type f the {@code message} to be filtered
	 * @param <O>         the type of the SPARQL {@code operation} to be configured
	 *
	 * @return the input {@code operation} with standard bindings and optional custom configurations applied
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>, O extends Operation> O configure(
			final M message, final O operation, final BiConsumer<M, O>... customizers
	) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( operation == null ) {
			throw new NullPointerException("null operation");
		}

		if ( customizers == null ) {
			throw new NullPointerException("null customizers");
		}

		if ( Arrays.stream(customizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null customizer");
		}

		operation.setBinding("time", literal(Instant.now(), true));

		final IRI item=iri(message.item());

		operation.setBinding("this", item);
		operation.setBinding("stem", iri(item.getNamespace()));
		operation.setBinding("name", literal(item.getLocalName()));

		final Request request=message.request();

		operation.setBinding("task", literal(request.method()));
		operation.setBinding("base", iri(request.base()));
		operation.setBinding("item", iri(request.item()));
		operation.setBinding("user",
				request.user().map(v -> v instanceof Value ? (Value)v : literal(v.toString())).orElse(RDF.NIL));

		if ( message instanceof Response ) {
			operation.setBinding("code", literal(integer(((Response)message).status())));
		}

		for (final BiConsumer<M, O> customizer : customizers) {
			customizer.accept(message, operation);
		}

		return operation;
	}


	//// Slug Generators //////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a sequential auto-incrementing slug generator.
	 *
	 * <p><strong>Warning</strong> / SPARQL doesn't natively support auto-incrementing ids: auto-incrementing slug
	 * calls are partly serialized in the system {@linkplain Graph#graph() graph} database using an internal lock
	 * object;
	 * this strategy may fail for distributed containers or external concurrent updates on the SPARQL endpoint, causing
	 * requests to fail with an {@link Response#InternalServerError} or {@link Response#Conflict}
	 * status code.</p>
	 *
	 * @return a slug generator returning an auto-incrementing numeric id unique to the focus item of the request
	 */
	public static Function<Request, String> auto() {
		return new AutoGenerator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Repository repository;

	private final ThreadLocal<RepositoryConnection> context=new ThreadLocal<>();


	/**
	 * Creates a graph store.
	 *
	 * @param repository the backing RDF repository
	 *
	 * @throws NullPointerException if {@code repository} is null
	 */
	public Graph(final Repository repository) {

		if ( repository == null ) {
			throw new NullPointerException("null repository");
		}

		this.repository=repository;
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
	 * <p>If a transaction is not already active on the shared repository connection, begins one and commits it on
	 * successful task completion; if the task throws an exception, the transaction is rolled back and the exception
	 * rethrown; in either case,  no action is taken if the transaction was already terminated inside the task.</p>
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
	 * <p>If a transaction is not already active on the shared repository connection, begins one and commits it on
	 * successful task completion; if the task throws an exception, the transaction is rolled back and the exception
	 * rethrown; in either case no action is taken if the transaction was already closed inside the task.</p>
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
			throw new IllegalStateException("closed graph store");
		}

		final RepositoryConnection shared=context.get();

		if ( shared != null ) {

			return requireNonNull(task.apply(shared), "null task return value");

		} else {

			if ( !repository.isInitialized() ) { repository.init(); }

			try ( final RepositoryConnection connection=repository.getConnection() ) {

				context.set(connection);

				try {

					if ( !connection.isActive() ) { connection.begin(); }

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

	private static final class AutoGenerator implements Function<Request, String> {

		private static final IRI Auto=iri("app://rest.metreeca.com/terms#", "auto");


		private final Graph graph=Context.asset(graph());


		@Override public String apply(final Request request) {
			return graph.exec(connection -> {

				// !!! custom name pattern
				// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf
				// .org/html/rfc5023#section-9.7)
				// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)

				final String item=request.item();
				final IRI stem=iri(item.substring(0, item.lastIndexOf('/')+1));

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
