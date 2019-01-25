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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Option.option;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.iri;

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


	@Override public Shape probe(final Field field) {

		final IRI iri=field.getIRI();
		final Shape shape=field.getShape().map(this);

		return shape.equals(or()) ? and() : field(iri, shape);
	}


	@Override public Shape probe(final And and) {

		final Collection<Shape> shapes=new Merger() {

			@Override protected int minCount(final int x, final int y) { return max(x, y); }

			@Override protected int maxCount(final int x, final int y) { return min(x, y); }

			@Override protected IRI type(final IRI x, final IRI y) { return derives(x, y) ? y : derives(y, x) ? x : null; }

		}.merge(flatten(and.getShapes(), And::and, new Visitor<Stream<Shape>>() {

			@Override public Stream<Shape> probe(final Shape shape) { return Stream.of(shape); }

			@Override public Stream<Shape> probe(final And and) { return and.getShapes().stream(); }

		}));

		return shapes.contains(or()) ? or() // always fail
				: shapes.size() == 1 ? shapes.iterator().next()
				: and(shapes);
	}

	@Override public Shape probe(final Or or) {

		final Collection<Shape> shapes=new Merger() {

			@Override protected int minCount(final int x, final int y) { return min(x, y); }

			@Override protected int maxCount(final int x, final int y) { return max(x, y); }

			@Override protected IRI type(final IRI x, final IRI y) { return derives(x, y) ? x : derives(y, x) ? y : null; }

		}.merge(flatten(or.getShapes(), Or::or, new Visitor<Stream<Shape>>() {

			@Override public Stream<Shape> probe(final Shape shape) {
				return Stream.of(shape);
			}

			@Override public Stream<Shape> probe(final Or or) {
				return or.getShapes().stream();
			}

		}));

		return shapes.contains(and()) ? and() // always pass
				: shapes.size() == 1 ? shapes.iterator().next()
				: or(shapes);
	}

	@Override public Shape probe(final Option option) {

		final Shape test=option.getTest().map(this);
		final Shape pass=option.getPass().map(this);
		final Shape fail=option.getFail().map(this);

		return test.equals(and()) ? pass // always pass
				: test.equals(or()) ? fail // always fail
				: pass.equals(fail) ? pass // identical options
				: option(test, pass, fail);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static boolean derives(final IRI x, final IRI y) {
		return x.equals(Values.ValueType)
				|| x.equals(Values.ResourceType) && resource(y)
				|| x.equals(Values.LiteralType) && literal(y);
	}

	private static boolean resource(final IRI type) {
		return type.equals(Values.ResourceType) || type.equals(Values.BNodeType) || type.equals(Values.IRIType);
	}

	private static boolean literal(final IRI type) {
		return type.equals(Values.LiteralType) || !type.equals(Values.ValueType) && !resource(type);
	}


	private Set<Shape> flatten(final Collection<Shape> collection,
			final Function<Collection<Shape>, Shape> packer, final Shape.Probe<Stream<Shape>> lifter) {

		final Shape.Probe<Map.Entry<IRI, Shape>> splitter=new Visitor<Map.Entry<IRI, Shape>>() {

			private int id;

			@Override public Map.Entry<IRI, Shape> probe(final Shape shape) {
				return entry(iri("_:", "id"+id++), shape); // assign non-fields a unique step
			}

			@Override public Map.Entry<IRI, Shape> probe(final Field field) {
				return entry(field.getIRI(), field.getShape());
			}

		};

		return collection.stream()

				.map(shape -> shape.map(this)) // optimize nested shapes
				.flatMap(shape -> shape.map(lifter)) // merge nested collections

				.map(shape -> shape.map(splitter)) // split fields into Map.Entry<IRI, Shape>

				.collect(groupingBy(Map.Entry::getKey, // merge entries as Entry<Shift, List<Shape>>
						LinkedHashMap::new, mapping(Map.Entry::getValue, toList())))

				.entrySet().stream().flatMap(e -> { // reassemble fields merging and optimizing multiple definitions

					final IRI iri=e.getKey();
					final List<Shape> values=e.getValue();

					return iri.getNamespace().equals("_:") ? values.stream()
							: Stream.of(field(iri, packer.apply(values).map(this)));

				})

				.collect(toCollection(LinkedHashSet::new)); // remove duplicates preserving order
	}


	private abstract static class Merger extends Visitor<Merger> {

		private int minCount=-1;
		private int maxCount=-1;

		private IRI type;


		private final Collection<Shape> shapes=new ArrayList<>();



		@Override public Merger probe(final Shape shape) {

			shapes.add(shape);

			return this;
		}


		@Override public Merger probe(final MinCount minCount) {

			final int limit=minCount.getLimit();

			this.minCount=this.minCount < 0 ? limit : minCount(this.minCount, limit);

			return this;
		}

		@Override public Merger probe(final MaxCount maxCount) {

			final int limit=maxCount.getLimit();

			this.maxCount=this.maxCount < 0 ? limit : maxCount(this.maxCount, limit);

			return this;
		}

		@Override public Merger probe(final Datatype datatype) { // !!! refactor

			final IRI iri=datatype.getIRI();

			if ( this.type == null ) {

				this.type=iri;

			} else {

				final IRI merged=type(this.type, iri);

				if ( merged != null ) {

					this.type=merged;

				} else {
					shapes.add(datatype);
				}
			}

			return this;

		}



		protected abstract int minCount(final int x, final int y);

		protected abstract int maxCount(final int x, final int y);

		protected abstract IRI type(final IRI x, final IRI y);


		protected Collection<Shape> merge(final Iterable<Shape> shapes) {

			shapes.forEach(shape -> shape.map(this));

			if ( minCount >= 0 ) { this.shapes.add(MinCount.minCount(minCount)); }
			if ( maxCount >= 0 ) { this.shapes.add(MaxCount.maxCount(maxCount)); }

			if ( type != null ) { this.shapes.add(Datatype.datatype(type)); }

			return this.shapes;
		}

	}

}
