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
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.rest.Engine;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.engines.Flock.flock;
import static com.metreeca.rest.wrappers.Throttler.container;
import static com.metreeca.rest.wrappers.Throttler.resource;


/**
 * Shape-driven container engine.
 *
 * <p>Manages CRUD lifecycle operations on container resource descriptions defined by a shape.</p>
 */
final class ShapedContainer extends GraphEntity {

	private final Graph graph;
	private final Flock flock;

	private final Shape relate;
	private final Shape browse;
	private final Shape create;

	private final Engine delegate;


	ShapedContainer(final Graph graph, final Shape shape) {

		final Shape container=container().apply(shape);
		final Shape resource=resource().apply(shape);

		this.graph=graph;
		this.flock=flock(metas(resource)).orElseGet(Flock.None::new);

		final Shape browse=redact(resource, Form.relate, Form.digest);

		this.relate=and(redact(container, Form.relate, Form.detail), field(LDP.CONTAINS, browse));
		this.browse=browse;
		this.create=redact(resource, Form.create, Form.detail);

		this.delegate=new ShapedResource(graph, container);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> relate(final IRI resource) {
		return delegate.relate(resource);
	}

	@Override public <V, E> Result<V, E> relate(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser, final BiFunction<Shape, Collection<Statement>, V> mapper
	) {
		return delegate.relate(resource, parser, mapper);
	}

	@Override public <V, E> Result<V, E> browse(final IRI resource,
			final Function<Shape, Result<? extends Query, E>> parser, final BiFunction<Shape, Collection<Statement>, V> mapper
	) {

		return parser.apply(and(browse, filter().then(flock.anchor(resource)))).fold(

				query -> graph.query(connection -> {

					return query.map(new Query.Probe<Result<V, E>>() {

						@Override public Result<V, E> probe(final Edges edges) {
							final Map<Resource, Collection<Statement>> process=new ShapedRetriever(connection).process(query);
							return Value(mapper.apply(relate, set()));

						}

						@Override public Result<V, E> probe(final Stats stats) {
							throw new UnsupportedOperationException("to be implemented"); // !!! tbi
						}

						@Override public Result<V, E> probe(final Items items) {
							throw new UnsupportedOperationException("to be implemented"); // !!! tbi
						}

					});

				}),

				Result::Error
		);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Focus> create(final IRI resource, final IRI related,
			final Collection<Statement> model) {
		return graph.update(connection -> {

			return reserve(connection, related).map(reserved -> {

				flock.insert(connection, resource, related, model).add(model);

				// !!! validate before altering the db (snapshot isolation)

				final Focus focus=validate(connection, related, create, model);

				if ( focus.assess(Issue.Level.Error) ) {
					connection.rollback();
				} else {
					connection.commit();
				}

				return focus;

			});

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("shaped container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("shaped container deletion not supported");
	}

}
