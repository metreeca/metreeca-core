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
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.Field;
import com.metreeca.rest.Engine;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.Lambdas.memoize;
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
	private final Shape shape;

	private final Function<Shape, Flock> flock=memoize(s ->
			flock(metas(resource().apply(s))).orElseGet(Flock.None::new)
	);

	private final Function<Shape, Shape> browse=memoize(s -> s
			.map(resource())
			.map(new Redactor(Form.task, Form.relate))
			.map(new Redactor(Form.view, Form.digest))
			.map(new Redactor(Form.role, Form.any))
			.map(new Optimizer())
	);

	private final Function<Shape, Shape> create=memoize(s -> s
			.map(resource())
			.map(new Redactor(Form.task, Form.create))
			.map(new Redactor(Form.view, Form.detail))
			.map(new Redactor(Form.role, Form.any))
			.map(new Optimizer())
	);

	private final Function<Shape, Field> convey=memoize(s -> field(LDP.CONTAINS, s
			.map(browse)
			.map(new Redactor(Form.mode, Form.convey))
			.map(new Optimizer())
	));

	private final Engine delegate;


	ShapedContainer(final Graph graph, final Shape shape) {

		this.graph=graph;
		this.shape=shape;

		this.delegate=new ShapedResource(graph, container().apply(shape));
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

		return parser.apply(and(shape.map(browse), filter().then(shape.map(flock).anchor(resource)))).fold(

				query -> graph.query(connection -> {

					final Collection<Statement> model=new GraphRetriever().retrieve(resource, query);

					return query.map(new Query.Probe<Result<V, E>>() {

						@Override public Result<V, E> probe(final Edges edges) {
							return Value(mapper.apply(shape.map(convey), model));
						}

						@Override public Result<V, E> probe(final Stats stats) {
							return Value(mapper.apply(Stats.Shape, model));
						}

						@Override public Result<V, E> probe(final Items items) {
							return Value(mapper.apply(Items.Shape, model));
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

				// validate before updating graph to support snapshot transactions

				final Focus focus=new GraphValidator().validate(related, shape.map(create), model);

				if ( focus.assess(Issue.Level.Error) ) {

					connection.rollback();

				} else {

					shape.map(flock).insert(connection, resource, related, model).add(model);

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
