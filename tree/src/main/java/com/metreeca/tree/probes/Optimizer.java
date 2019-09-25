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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.*;


/**
 * Shape optimizer.
 *
 * <p>Recursively removes redundant and non-validating constructs from a shape.</p>
 */
public final class Optimizer extends Traverser<Shape> {

	@Override public Shape probe(final Shape shape) {
		return shape;
	}


	@Override public Shape probe(final All all) {
		return all.getValues().isEmpty()? and() : all;
	}

	@Override public Shape probe(final Any any) {
		return any.getValues().isEmpty()? or() : any;
	}


	@Override public Shape probe(final Field field) {

		final Object name=field.getName();
		final Shape shape=field.getShape().map(this);

		return shape.equals(or()) ? and() : field(name, shape);
	}


	@Override public Shape probe(final And and) {

		final class AndMerger extends Merger {

			@Override protected int minCount(final int x, final int y) { return max(x, y); }

			@Override protected int maxCount(final int x, final int y) { return min(x, y); }

		}

		final class AndInspector extends Inspector<Stream<Shape>> {

			@Override public Stream<Shape> probe(final Shape shape) { return Stream.of(shape); }

			@Override public Stream<Shape> probe(final And and) { return and.getShapes().stream(); }

		}

		final Collection<Shape> shapes=new AndMerger().merge(flatten(and.getShapes(), And::and, new AndInspector()));

		return shapes.contains(or()) ? or() // always fail
				: shapes.size() == 1 ? shapes.iterator().next()
				: and(shapes);
	}

	@Override public Shape probe(final Or or) {

		final class OrMerger extends Merger {

			@Override protected int minCount(final int x, final int y) { return min(x, y); }

			@Override protected int maxCount(final int x, final int y) { return max(x, y); }

		}

		final class OrInspector extends Inspector<Stream<Shape>> {

			@Override public Stream<Shape> probe(final Shape shape) {
				return Stream.of(shape);
			}

			@Override public Stream<Shape> probe(final Or or) {
				return or.getShapes().stream();
			}

		}

		final Collection<Shape> shapes=new OrMerger().merge(flatten(or.getShapes(), Or::or, new OrInspector()));

		return shapes.contains(and()) ? and() // always pass
				: shapes.size() == 1 ? shapes.iterator().next()
				: or(shapes);
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

	private Set<Shape> flatten(final Collection<Shape> collection,
			final Function<Collection<Shape>, Shape> packer, final Shape.Probe<Stream<Shape>> lifter
	) {

		final class Id {}

		final Shape.Probe<Map.Entry<Object, Shape>> splitter=new Inspector<Map.Entry<Object, Shape>>() {

			private int id;

			@Override public Map.Entry<Object, Shape> probe(final Shape shape) {
				return new SimpleImmutableEntry<>(new Id(), shape); // assign non-fields a unique step
			}

			@Override public Map.Entry<Object, Shape> probe(final Field field) {
				return new SimpleImmutableEntry<>(field.getName(), field.getShape());
			}

		};

		return collection.stream()

				.map(shape -> shape.map(this)) // optimize nested shapes
				.flatMap(shape -> shape.map(lifter)) // merge nested collections

				.map(shape -> shape.map(splitter)) // split fields into Map.Entry<Object, Shape>

				.collect(groupingBy(Map.Entry::getKey, // merge entries as Entry<IRI, List<Shape>>
						LinkedHashMap::new, mapping(Map.Entry::getValue, toList())))

				.entrySet().stream().flatMap(e -> { // reassemble fields merging and optimizing multiple definitions

					final Object name=e.getKey();
					final List<Shape> values=e.getValue();

					return name instanceof Id ? values.stream()
							: Stream.of(field(name, packer.apply(values).map(this)));

				})

				.collect(toCollection(LinkedHashSet::new)); // remove duplicates preserving order
	}


	private abstract static class Merger extends Inspector<Merger> {

		private int minCount=-1;
		private int maxCount=-1;


		private final Collection<Shape> shapes=new ArrayList<>();



		@Override public Merger probe(final Shape shape) {

			shapes.add(shape);

			return this;
		}


		@Override public Merger probe(final MinCount minCount) {

			final int limit=minCount.getLimit();

			this.minCount=(this.minCount < 0) ? limit : minCount(this.minCount, limit);

			return this;
		}

		@Override public Merger probe(final MaxCount maxCount) {

			final int limit=maxCount.getLimit();

			this.maxCount=(this.maxCount < 0) ? limit : maxCount(this.maxCount, limit);

			return this;
		}



		protected abstract int minCount(final int x, final int y);

		protected abstract int maxCount(final int x, final int y);


		protected Collection<Shape> merge(final Iterable<Shape> shapes) {

			shapes.forEach(shape -> shape.map(this));

			if ( minCount >= 0 ) { this.shapes.add(MinCount.minCount(minCount)); }
			if ( maxCount >= 0 ) { this.shapes.add(MaxCount.maxCount(maxCount)); }

			return this.shapes;
		}

	}

}
