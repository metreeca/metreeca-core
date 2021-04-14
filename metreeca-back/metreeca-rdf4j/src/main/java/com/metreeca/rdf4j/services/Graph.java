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

package com.metreeca.rdf4j.services;

import com.metreeca.json.Frame;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.time.Instant;
import java.util.*;
import java.util.function.*;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Frame.model;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.task;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;

import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.stream.Collectors.toCollection;


/**
 * Graph store.
 *
 * <p>Manages task execution on an RDF {@linkplain Repository repository}.</p>
 *
 * <p>Nested task executions on the same thread will share the same connection to the backing RDF repository through a
 * {@link ThreadLocal} scope variable.</p>
 */
public final class Graph implements AutoCloseable {

	private static final ThreadLocal<RepositoryConnection> context=new ThreadLocal<>();


	/**
	 * Retrieves the default graph factory.
	 *
	 * @return the default graph factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Graph> graph() {
		return () -> { throw new IllegalStateException("undefined graph service"); };
	}


	//// Transactions //////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a transaction wrapper.
	 *
	 * @return a wrapper ensuring that requests are handled within a single graph transaction
	 */
	public static Wrapper txn() {

		final Graph graph=service(graph());

		return handler -> request -> consumer -> graph.update(task(connection ->
				handler.handle(request).accept(consumer)
		));
	}


	//// SPARQL Processors /////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a SPARQL query message filter.
	 *
	 * @param query       the SPARQL graph query (describe/construct) to be executed by the new filter on target
	 *                    messages; empty scripts are ignored
	 * @param customizers optional custom configuration setters for the SPARQL query operation
	 * @param <M>         the type of the target message for the new filter
	 *
	 * @return a message filter executing the SPARQL graph {@code query} on target messages with {@linkplain
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom configurations; returns the
	 * input model extended with the statements returned by {@code query}
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>> BiFunction<M, Frame, Frame> query(
			final String query, final BiConsumer<M, GraphQuery>... customizers
	) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( customizers == null || Arrays.stream(customizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null customizers");
		}

		final Graph graph=service(graph());

		return query.isEmpty() ? (message, frame) -> frame : (message, frame) -> graph.query(connection -> {

			final ArrayList<Statement> model=model(frame).collect(toCollection(ArrayList::new));

			configure(
					message, connection.prepareGraphQuery(SPARQL, query, message.request().base()), customizers
			).evaluate(
					new StatementCollector(model)
			);

			return frame(frame.focus(), model);

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
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom configurations; returns the
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

		final Graph graph=service(graph());

		return update.isEmpty() ? message -> message : message -> graph.update(connection -> {

			configure(message, connection.prepareUpdate(SPARQL, update, message.request().base()), customizers).execute();

			return message;

		});
	}


	/**
	 * Configures standard bindings for SPARQL operations.
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

		if ( customizers == null || Arrays.stream(customizers).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null customizers");
		}

		operation.setBinding("time", literal(Instant.now().truncatedTo(MILLIS).atZone(UTC)));

		final IRI item=iri(message.item());

		operation.setBinding("this", item);
		operation.setBinding("stem", iri(item.getNamespace()));
		operation.setBinding("name", literal(item.getLocalName()));

		final Request request=message.request();

		operation.setBinding("task", literal(request.method()));
		operation.setBinding("base", iri(request.base()));
		operation.setBinding("item", iri(request.item()));
		operation.setBinding("user", request.user()
				.map(v -> v instanceof Value ? (Value)v : literal(v.toString()))
				.orElse(RDF.NIL)
		);

		if ( message instanceof Response ) {
			operation.setBinding("code", literal(((Response)message).status()));
		}

		for (final BiConsumer<M, O> customizer : customizers) {
			customizer.accept(message, operation);
		}

		return operation;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Repository repository;


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
	 * Executes a query on this graph store.
	 *
	 * @param query the query to be executed; takes as argument a connection to the repository backing this graph store
	 * @param <V>   the type of the value returned by {@code query}
	 *
	 * @return the value returned by {@code query}
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public <V> V query(final Function<RepositoryConnection, V> query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( repository == null ) {
			throw new IllegalStateException("closed graph store");
		}

		final RepositoryConnection shared=context.get();

		if ( shared != null ) { return query.apply(shared); } else {

			if ( !repository.isInitialized() ) { repository.init(); }

			try ( final RepositoryConnection connection=repository.getConnection() ) {

				context.set(connection);

				return query.apply(connection);

			} finally {

				context.remove();

			}

		}
	}

	/**
	 * Executes an update inside a transaction on this graph store.
	 *
	 * <ul>
	 *
	 *      <li>if a transaction is not already active on the underlying storage, begins one and commits it on
	 *      successful update completion;</li>
	 *
	 *      <li>if the update throws an exception, rolls back the transaction and rethrows the exception;</li>
	 *
	 *      <li>in either case, no action is taken if the transaction was already terminated inside the update.</li>
	 *
	 * </ul>
	 *
	 * @param update the update to be executed
	 * @param <V>    the type of the value returned by {@code update}
	 *
	 * @return the value returned by {@code update}
	 *
	 * @throws NullPointerException if {@code  update} is null
	 */
	public <V> V update(final Function<RepositoryConnection, V> update) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		return query(connection -> {
			if ( connection.isActive() ) { return update.apply(connection); } else {

				try {

					connection.begin();

					final V value=update.apply(connection);

					if ( connection.isActive() ) { connection.commit(); }

					return value;

				} finally {

					if ( connection.isActive() ) { connection.rollback(); }

				}

			}
		});
	}

}
