/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.rest.Engine;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.engines.Flock.flock;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded description container engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
final class SimpleContainer extends GraphEntity {

	private final Graph graph;
	private final Shape shape;

	private final Flock flock;

	private final Engine delegate;


	SimpleContainer(final Graph graph, final Map<IRI, Value> metadata) {

		this.graph=graph;
		this.shape=pass();
		this.flock=flock(metadata).orElseGet(Flock.Basic::new);

		this.delegate=new SimpleResource(graph, metadata);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> relate(final IRI resource) {
		return delegate.relate(resource);
	}

	@Override public <V, E> Result<V, E> relate(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser,
			final BiFunction<Shape, Collection<Statement>, V> mapper
	) {
		return delegate.relate(resource, parser, mapper);
	}

	@Override public <V, E> Result<V, E> browse(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser,
			final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		return parser.apply(shape).fold(

				query -> {

					if ( query.equals(edges(shape)) ) {

						return graph.query(connection -> {

							return Value(mapper.apply(shape, flock
									.items(connection, resource)
									.flatMap(item -> Stream.concat(
											Stream.of(statement(resource, LDP.CONTAINS, item)),
											new GraphRetriever()._retrieve(item, true).stream()
									))
									.collect(toList())
							));

						});

					} else {

						throw new UnsupportedOperationException("simple container filtered browsing not supported");

					}
				},

				Result::Error
		);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		return graph.update(connection -> {

			return reserve(connection, related).map(reserved -> {

				final Focus focus=new GraphValidator().validate(related, shape, model);

				if ( focus.assess(Issue.Level.Error) ) {

					connection.rollback();

				} else {

					flock.insert(connection, resource, reserved, model).add(model);

					connection.commit();

				}

				return focus;

			});

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("simple container deletion not supported");
	}

}
