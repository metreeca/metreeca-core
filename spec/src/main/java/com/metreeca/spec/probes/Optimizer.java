/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.spec.probes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.Shift;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;
import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Group.group;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Values.iri;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.*;


/**
 * Shape optimizer.
 *
 * <p>Recursively removes redundant and non-validating constructs from a shape.</p>
 */
public final class Optimizer extends Shape.Probe<Shape> {

	@Override protected Shape fallback(final Shape shape) {
		return shape;
	}


	@Override public Shape visit(final Group group) {
		return group(group.getShape().accept(this));
	}

	@Override public Shape visit(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return shape.equals(or()) ? and() : trait(step, shape);
	}

	@Override public Shape visit(final Virtual virtual) {

		final Trait trait=virtual.getTrait();
		final Shift shift=virtual.getShift();

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return shape.equals(or()) ? and() : virtual(trait(step, shape), shift);
	}


	private static boolean derives(final IRI x, final IRI y) {
		return x.equals(Values.ValueType)
				|| x.equals(Values.ResoureType) && resource(y)
				|| x.equals(Values.LiteralType) && literal(y);
	}

	private static boolean resource(final IRI type) {
		return type.equals(Values.ResoureType) || type.equals(Values.BNodeType) || type.equals(Values.IRIType);
	}

	private static boolean literal(final IRI type) {
		return type.equals(Values.LiteralType) || !type.equals(Values.ValueType) && !resource(type);
	}

	@Override public Shape visit(final And and) {

		final Collection<Shape> shapes=new Merger() {

			@Override protected int minCount(final int x, final int y) { return max(x, y); }

			@Override protected int maxCount(final int x, final int y) { return min(x, y); }

			@Override protected IRI type(final IRI x, final IRI y) { return derives(x, y) ? y : derives(y, x) ? x : null; }

		}.merge(flatten(and.getShapes(), And::and, new Shape.Probe<Stream<Shape>>() {

			@Override public Stream<Shape> visit(final And conjunction) {
				return conjunction.getShapes().stream();
			}

			@Override protected Stream<Shape> fallback(final Shape shape) {
				return Stream.of(shape);
			}

		}));

		return shapes.contains(or()) ? or() // always fail
				: shapes.size() == 1 ? shapes.iterator().next()
				: and(shapes);
	}

	@Override public Shape visit(final Or or) {

		final Collection<Shape> shapes=new Merger() {

			@Override protected int minCount(final int x, final int y) { return min(x, y); }

			@Override protected int maxCount(final int x, final int y) { return max(x, y); }

			@Override protected IRI type(final IRI x, final IRI y) { return derives(x, y) ? x : derives(y, x) ? y : null; }

		}.merge(flatten(or.getShapes(), Or::or, new Shape.Probe<Stream<Shape>>() {

			@Override protected Stream<Shape> fallback(final Shape shape) {
				return Stream.of(shape);
			}

			@Override public Stream<Shape> visit(final Or disjunction) {
				return disjunction.getShapes().stream();
			}

		}));

		return shapes.contains(and()) ? and() // always pass
				: shapes.size() == 1 ? shapes.iterator().next()
				: or(shapes);
	}

	@Override public Shape visit(final Test option) {

		final Shape test=option.getTest().accept(this);
		final Shape pass=option.getPass().accept(this);
		final Shape fail=option.getFail().accept(this);

		return test.equals(and()) ? pass // always pass
				: test.equals(or()) ? fail // always fail
				: pass.equals(fail) ? pass // identical options
				: test(test, pass, fail);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Shape> flatten(final Collection<Shape> collection,
			final Function<Collection<Shape>, Shape> packer, final Shape.Probe<Stream<Shape>> lifter) {

		final Shape.Probe<Map.Entry<Step, Shape>> splitter=new Shape.Probe<Map.Entry<Step, Shape>>() {

			private int id;

			@Override protected Map.Entry<Step, Shape> fallback(final Shape shape) {
				return entry(Step.step(iri("_:", "id"+id++)), shape); // assign non-traits a unique step
			}

			@Override public Map.Entry<Step, Shape> visit(final Trait trait) {
				return entry(trait.getStep(), trait.getShape());
			}

		};

		return collection.stream()

				.map(shape -> shape.accept(this)) // optimize nested shapes
				.flatMap(shape -> shape.accept(lifter)) // merge nested collections

				.map(shape -> shape.accept(splitter)) // split traits into Map.Entry<Step, Shape>

				.collect(groupingBy(Map.Entry::getKey, // merge entries as Entry<Step, List<Shape>>
						LinkedHashMap::new, mapping(Map.Entry::getValue, toList())))

				.entrySet().stream().flatMap(e -> { // reassemble traits merging and optimizing multiple definitions

					final Step step=e.getKey();
					final List<Shape> values=e.getValue();

					return step.getIRI().getNamespace().equals("_:") ? values.stream()
							: Stream.of(trait(step, packer.apply(values).accept(this)));

				})

				.collect(toCollection(LinkedHashSet::new)); // remove duplicates preserving order
	}


	private abstract static class Merger extends Shape.Probe<Merger> {

		private int minCount=-1;
		private int maxCount=-1;

		private IRI type;


		private final Collection<Shape> shapes=new ArrayList<>();


		protected Collection<Shape> merge(final Iterable<Shape> shapes) {

			shapes.forEach(shape -> shape.accept(this));

			if ( minCount >= 0 ) { this.shapes.add(MinCount.minCount(minCount)); }
			if ( maxCount >= 0 ) { this.shapes.add(MaxCount.maxCount(maxCount)); }

			if ( type != null ) { this.shapes.add(Datatype.datatype(type)); }

			return this.shapes;
		}


		@Override public Merger visit(final MinCount minCount) {

			final int limit=minCount.getLimit();

			this.minCount=this.minCount < 0 ? limit : minCount(this.minCount, limit);

			return this;
		}

		@Override public Merger visit(final MaxCount maxCount) {

			final int limit=maxCount.getLimit();

			this.maxCount=this.maxCount < 0 ? limit : maxCount(this.maxCount, limit);

			return this;
		}

		@Override public Merger visit(final Datatype datatype) { // !!! refactor

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


		@Override protected Merger fallback(final Shape shape) {

			shapes.add(shape);

			return this;
		}


		protected abstract int minCount(final int x, final int y);

		protected abstract int maxCount(final int x, final int y);

		protected abstract IRI type(final IRI x, final IRI y);

	}

}
