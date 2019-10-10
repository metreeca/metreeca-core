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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.*;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.*;


/**
 * Shape optimizer.
 *
 * <p>Recursively removes redundant constructs from a shape.</p>
 */
public class Optimizer extends Traverser<Shape> {

	private static final Shape.Probe<Stream<Shape>> AndFlattener=new Inspector<Stream<Shape>>() {

		@Override public Stream<Shape> probe(final Shape shape) {
			return Stream.of(shape);
		}

		@Override public Stream<Shape> probe(final And and) {
			return and.getShapes().stream();
		}

	};

	private static final Shape.Probe<Stream<Shape>> OrFlattener=new Inspector<Stream<Shape>>() {

		@Override public Stream<Shape> probe(final Shape shape) {
			return Stream.of(shape);
		}

		@Override public Stream<Shape> probe(final Or or) {
			return or.getShapes().stream();
		}

	};


	private static final Function<Collection<Shape>, Shape> AndPacker=shapes
			-> shapes.contains(or()) ? or() // always fail
			: shapes.size() == 1 ? shapes.iterator().next()
			: and(shapes);

	private static final Function<Collection<Shape>, Shape> OrPacker=shapes
			->  shapes.contains(and()) ? and() // always pass
			: shapes.size() == 1 ? shapes.iterator().next()
			: or(shapes);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape probe(final Shape shape) {
		return shape;
	}


	@Override public Shape probe(final All all) {
		return all.getValues().isEmpty() ? and() : all;
	}

	@Override public Shape probe(final Any any) {
		return any.getValues().isEmpty() ? or() : any;
	}


	@Override public Shape probe(final Field field) {

		final Object name=field.getName();
		final Shape shape=field.getShape().map(this);

		return shape.equals(or()) ? and() : field(name, shape);
	}


	@Override public Shape probe(final And and) {
		return optimize(and.getShapes().stream(), AndFlattener, AndPacker, (clazz, constraints)
				-> clazz.equals(MinCount.class) ? Stream.of(minCount(max(constraints, s -> ((MinCount)s).getLimit())))
				: clazz.equals(MaxCount.class) ? Stream.of(maxCount(min(constraints, s -> ((MaxCount)s).getLimit())))
				: clazz.equals(Datatype.class) ? datatypes(constraints, (x, y) -> derives(x, y)) // ignore super-types
				: constraints.distinct()
		);
	}

	@Override public Shape probe(final Or or) {
		return optimize(or.getShapes().stream(), OrFlattener, OrPacker, (clazz, constraints)
				-> clazz.equals(MinCount.class) ? Stream.of(minCount(min(constraints, s -> ((MinCount)s).getLimit())))
				: clazz.equals(MaxCount.class) ? Stream.of(maxCount(max(constraints, s -> ((MaxCount)s).getLimit())))
				: clazz.equals(Datatype.class) ? datatypes(constraints, (x, y) -> derives(y, x)) // ignore sub-types
				: constraints.distinct()
		);
	}

	@Override public Shape probe(final When when) {

		final Shape test=when.getTest().map(this);
		final Shape pass=when.getPass().map(this);
		final Shape fail=when.getFail().map(this);

		return test.equals(and()) ? pass // always pass
				: test.equals(or()) ? fail // always fail
				: pass.equals(fail) ? pass // identical options
				: when(test, pass, fail);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int min(final Stream<Shape> constraints, final ToIntFunction<Shape> mapper) {
		return constraints.mapToInt(mapper).min().orElse(Integer.MAX_VALUE);
	}

	private int max(final Stream<Shape> constraints, final ToIntFunction<Shape> mapper) {
		return constraints.mapToInt(mapper).max().orElse(Integer.MIN_VALUE);
	}


	private Stream<Datatype> datatypes(final Stream<Shape> shapes, final BiPredicate<Object, Object> ignore) {

		final Set<Datatype> datatypes=shapes.map(s -> (Datatype)s).collect(toSet());

		return datatypes.stream().filter(datatype -> datatypes.stream()
				.filter(reference -> !datatype.equals(reference))
				.noneMatch(reference -> ignore.test(datatype.getName(), reference.getName()))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected boolean derives(final Object upper, final Object lower) {
		return false;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape optimize(
			final Stream<Shape> shapes,
			final Shape.Probe<Stream<Shape>> flattener,
			final Function<Collection<Shape>, Shape> packer,
			final BiFunction<Class<?>, Stream<Shape>, Stream<? extends Shape>> merger
	) {
		return packer.apply(shapes

				.map(s -> s.map(this))
				.flatMap(s -> s.map(flattener))

				.collect(groupingBy(

						s -> s.map(new Inspector<Object>() {

							@Override public Object probe(final Shape shape) { return shape.getClass(); }

							@Override public Object probe(final Field field) { return field.getName(); }

						}),

						LinkedHashMap::new, // preserve ordering

						mapping(s -> s.map(new Inspector<Stream<Shape>>() {

							@Override public Stream<Shape> probe(final Shape shape) {
								return Stream.of(shape);
							}

							@Override public Stream<Shape> probe(final Field field) {
								return Stream.of(field.getShape().map(Optimizer.this));
							}

						}), reducing(Stream::concat))

				))

				.entrySet()
				.stream()

				.flatMap(entry -> {

					final Object name=entry.getKey();
					final Stream<Shape> nested=entry.getValue().orElseGet(Stream::empty);

					return name instanceof Class ? merger.apply((Class<?>)name, nested)
							: Stream.of(field(name, packer.apply(nested.collect(toList())).map(this)));

				})

				.collect(toList())

		);
	}

}
