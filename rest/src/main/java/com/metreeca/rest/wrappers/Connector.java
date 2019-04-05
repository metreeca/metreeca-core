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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.Update;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.metreeca.form.things.Values.*;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


/**
 * Graph connection manager.
 *
 * <p>Executes wrapped handlers inside a shared connection to the the system { ̰ graph
 * database}.</p>
 *
 * <p>If the incoming request is not {@linkplain Request#safe() safe}, wrapped handlers are executed inside a single
 * transaction on the shared connection, which is automatically committed on {@linkplain Response#success() successful}
 * response or rolled back otherwise.</p>
 */
public final class Connector implements Wrapper {

	/**
	 * Creates a SPARQL query message filter.
	 *
	 * @param query       the SPARQL graph query (describe/construct) to be executed by the new filter on target
	 *                    messages; empty scripts are ignored
	 * @param customizers optional custom configuration setters for the SPARQL query operation
	 * @param <M> the type of the target message for the new filter
	 *
	 * @return a message filter executing the SPARQL graph {@code query} on target messages with {@linkplain
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom configurations; returns the
	 * input model extended with the statements returned by {@code query}
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>> BiFunction<M, Model, Model> query(
			final String query, final BiConsumer<M, GraphQuery>... customizers
	) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( customizers == null ) {
			throw new NullPointerException("null customizers");
		}

		for (final BiConsumer<M, GraphQuery> customizer : customizers) {

			if ( customizer == null ) {
				throw new NullPointerException("null customizer");
			}

		}

		final Graph graph=tool(Graph.graph());

		return query.isEmpty() ? (message, model) -> model : (message, model) -> graph.update(connection -> {

			configure(
					message, connection.prepareGraphQuery(SPARQL, query, message.request().base()), customizers
			).evaluate(
					new StatementCollector(model)
			);

			return model;

		});
	}

	/**
	 * Creates a SPARQL update housekeeping filter.
	 *
	 * @param update      the SPARQL update script to be executed by the new housekeeping filter on target messages;
	 *                    empty scripts are ignored
	 * @param customizers optional custom configuration setters for the SPARQL update operation
	 * @param <M> the type of the target message for the new filter
	 *
	 * @return a housekeeping filter executing the SPARQL {@code update} script on target messages with {@linkplain
	 * #configure(Message, Operation, BiConsumer[]) standard bindings} and optional custom configurations; returns the
	 * input model without altering it
	 *
	 * @throws NullPointerException if any argument is null or if {@code customizers} contains null values
	 */
	@SafeVarargs public static <M extends Message<M>> BiFunction<M, Model, Model> update(
			final String update, final BiConsumer<M, Update>... customizers
	) {

		if ( update == null ) {
			throw new NullPointerException("null update");
		}

		if ( customizers == null ) {
			throw new NullPointerException("null customizers");
		}

		for (final BiConsumer<M, Update> customizer : customizers) {

			if ( customizer == null ) {
				throw new NullPointerException("null customizer");
			}

		}

		final Graph graph=tool(Graph.graph());

		return update.isEmpty() ? (message, model) -> model : (message, model) -> graph.update(connection -> {

			configure(message, connection.prepareUpdate(SPARQL, update, message.request().base()), customizers).execute();

			return model;

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
	 * <td>the IRI identifying the {@linkplain Request#user() user} submitting the original request</td>
	 * </tr>
	 *
	 * </tbody>
	 *
	 * </table>
	 *
	 * <p>If the target message is a {@linkplain Response response}, the following additional bindings are
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

		for (final BiConsumer<M, O> customizer : customizers) {

			if ( customizer == null ) {
				throw new NullPointerException("null customizer");
			}

		}

		operation.setBinding("time", time(true));

		final IRI item=message.item();

		operation.setBinding("this", item);
		operation.setBinding("stem", iri(item.getNamespace()));
		operation.setBinding("name", literal(item.getLocalName()));

		final Request request=message.request();

		operation.setBinding("task", literal(request.method()));
		operation.setBinding("base", iri(request.base()));
		operation.setBinding("item", request.item());
		operation.setBinding("user", request.user());

		if ( message instanceof Response ) {
			operation.setBinding("code", literal(integer(((Response)message).status())));
		}

		for (final BiConsumer<M, O> customizer : customizers) {
			customizer.accept(message, operation);
		}

		return operation;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.graph());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> consumer -> {
			if ( request.safe() ) {

				graph.query(connection -> {

					handler.handle(request).accept(consumer);

				});

			} else {

				graph.update(connection -> {
					handler.handle(request).map(response -> {

						if ( !response.success() && connection.isActive() ) {
							connection.rollback();
						}

						return response;

					}).accept(consumer);
				});

			}
		};
	}

}
