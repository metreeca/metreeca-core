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

package com.metreeca.rdf.formats;

import com.metreeca.rdf.Values;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Inspector;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.*;


/**
 * Shape optimizer.
 *
 * <p>Recursively removes redundant constructs from a shape.</p>
 */
public final class _Optimizer extends Traverser<Shape> {

	private static final Shape.Probe<Stream<Shape>> AndFlattener=new Inspector<Stream<Shape>>() {

		@Override public Stream<Shape> probe(final Shape shape) {
			return Stream.of(shape);
		}

		@Override public Stream<Shape> probe(final And and) {
			return and.getShapes().stream().flatMap(s -> s.map(this));
		}

	};

	private static final Shape.Probe<Stream<Shape>> OrFlattener=new Inspector<Stream<Shape>>() {

		@Override public Stream<Shape> probe(final Shape shape) {
			return Stream.of(shape);
		}

		@Override public Stream<Shape> probe(final Or or) {
			return or.getShapes().stream().flatMap(s -> s.map(this));
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

		// !!! @Override protected Object datatype(final Object x, final Object y) { return derives(x, y) ? y : derives(y, x) ? x : null; }

		return shapes(and, AndFlattener, AndPacker, (clazz, constraints)
				-> clazz.equals(MinCount.class) ? Stream.of(MinCount.minCount(constraints.map(s1 -> (MinCount)s1).map(MinCount::getLimit).reduce(0, Math::max)))
				: clazz.equals(MaxCount.class) ? Stream.of(MaxCount.maxCount(constraints.map(s -> (MaxCount)s).map(MaxCount::getLimit).reduce(Integer.MAX_VALUE, Math::min)))
				: constraints.distinct()
		);
	}

	@Override public Shape probe(final Or or) {

		// !!! @Override protected Object datatype(final Object x, final Object y) { return derives(x, y) ? x : derives(y, x) ? y : null; }

		return shapes(or, OrFlattener, OrPacker, (clazz, constraints)
				-> clazz.equals(MinCount.class) ? Stream.of(MinCount.minCount(constraints.map(s -> (MinCount)s).map(MinCount::getLimit).reduce(Integer.MAX_VALUE, Math::min)))
				: clazz.equals(MaxCount.class) ? Stream.of(MaxCount.maxCount(constraints.map(s -> (MaxCount)s).map(MaxCount::getLimit).reduce(0, Math::max)))
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

	private static boolean derives(final Object upper, final Object lower) {
		return upper.equals(Values.ValueType)
				|| upper.equals(Values.ResourceType) && resource(lower)
				|| upper.equals(Values.LiteralType) && literal(lower);
	}

	private static boolean resource(final Object type) {
		return type.equals(Values.ResourceType) || type.equals(Values.BNodeType) || type.equals(Values.IRIType);
	}

	private static boolean literal(final Object type) {
		return type.equals(Values.LiteralType) || !type.equals(Values.ValueType) && !resource(type);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shapes(
			final Shape shape,
			final Shape.Probe<Stream<Shape>> flattener,
			final Function<Collection<Shape>, Shape> packer,
			final BiFunction<Class<?>, Stream<Shape>, Stream<Shape>> merger
	) {

		return packer.apply(shape

				.map(flattener)

				.collect(groupingBy(

						s -> s.map(new Inspector<Object>() {

							@Override public Object probe(final Shape shape) { return shape.getClass(); }

							@Override public Object probe(final Field field) { return field.getName(); }

						}),

						mapping(s -> s.map(new Inspector<Stream<Shape>>() {

							@Override public Stream<Shape> probe(final Shape shape) {
								return Stream.of(shape);
							}

							@Override public Stream<Shape> probe(final Field field) {
								return Stream.of(field.getShape().map(_Optimizer.this));
							}

						}), reducing(Stream::concat))

				))

				.entrySet()
				.stream()

				.flatMap(entry -> {

					final Object name=entry.getKey();
					final Stream<Shape> shapes=entry.getValue().orElseGet(Stream::empty);

					return name instanceof Class ? merger.apply((Class<?>)name, shapes)
							: Stream.of(field(name, packer.apply(shapes.collect(toList())).map(this)));

				})

				.collect(toList())

		);
	}

}
