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

import com.metreeca.json.*;
import com.metreeca.json.queries.*;
import com.metreeca.rdf4j.services.GraphFacts.Options;
import com.metreeca.rest.services.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.rdf4j.services.Graph.graph;
import static com.metreeca.rdf4j.services.Graph.txn;
import static com.metreeca.rest.Toolbox.service;

import static java.util.Objects.requireNonNull;


/**
 * Model-driven graph engine.
 *
 * <p>Manages graph transactions and handles model-driven CRUD actions on linked data resources stored in the shared
 * {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements Engine {

	/**
	 * Maximum number of resources returned by items queries.
	 *
	 * @return an {@linkplain #set(Supplier, Object) option} with a default value of {@code 1000}
	 */
	public static Supplier<Integer> items() {
		return () -> 1_000;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Object> options=new LinkedHashMap<>();

	private final Graph graph=service(graph());


	/**
	 * Retrieves an engine option.
	 *
	 * @param option the option to be retrieved; must return a non-null default value
	 * @param <V>    the type of the option to be retrieved
	 *
	 * @return the value previously {@linkplain #set(Supplier, Object) configured} for {@code option} or its default
	 * value, if no custom value was configured; in the latter case the returned value is cached
	 *
	 * @throws NullPointerException if {@code option} is null or returns a null value
	 */
	@SuppressWarnings("unchecked")
	private <V> V get(final Supplier<V> option) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		return (V)options.computeIfAbsent(option, key ->
				requireNonNull(key.get(), "null option return value")
		);
	}

	/**
	 * Configures an engine option.
	 *
	 * @param option the option to be configured; must return a non-null default value
	 * @param value  the value to be configured for {@code option}
	 * @param <V>    the type of the option to be configured
	 *
	 * @return this engine
	 *
	 * @throws NullPointerException if either {@code option} or {@code value} is null
	 */
	public <V> GraphEngine set(final Supplier<V> option, final V value) {

		if ( option == null ) {
			throw new NullPointerException("null option");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		options.put(option, value);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Frame> create(final Frame frame, final Shape shape) {
		return graph.exec(txn(connection -> {

			return Optional.of(frame.focus())

					.filter(item
							-> !connection.hasStatement(item, null, null, true)
							&& !connection.hasStatement(null, null, item, true)
					)

					.map(item -> {

						connection.add(frame.model());

						return frame;

					});

		}));
	}

	@Override public Optional<Frame> relate(final Frame frame, final Query query) {
		return Optional

				.of(query.map(new QueryProbe(frame.focus(), this::get)))

				.map(model -> frame(frame.focus(), model));
	}

	@Override public Optional<Frame> update(final Frame frame, final Shape shape) {
		return graph.exec(txn(connection -> {

			return Optional

					.of(Items.items(shape).map(new QueryProbe(frame.focus(), this::get)))

					.filter(model -> !model.isEmpty())

					.map(model -> {

						connection.remove(model);
						connection.add(frame.model());

						return frame;

					});

		}));
	}

	@Override public Optional<Frame> delete(final Frame frame, final Shape shape) {
		return graph.exec(txn(connection -> {

			return Optional

					.of(Items.items(shape).map(new QueryProbe(frame.focus(), this::get)))

					.filter(model -> !model.isEmpty())

					.map(model -> {

						connection.remove(model);

						return frame(frame.focus(), model);

					});

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class QueryProbe extends Query.Probe<Collection<Statement>> {

		private final IRI resource;
		private final Options options;


		QueryProbe(final IRI resource, final Options options) {
			this.resource=resource;
			this.options=options;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		@Override public Collection<Statement> probe(final Items items) {
			return new GraphItems(options).process(resource, items);
		}

		@Override public Collection<Statement> probe(final Terms terms) {
			return new GraphTerms(options).process(resource, terms);
		}

		@Override public Collection<Statement> probe(final Stats stats) {
			return new GraphStats(options).process(resource, stats);
		}

	}

}
