/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

import static com.metreeca.json.Shape.expanded;
import static com.metreeca.json.shapes.Guard.*;
import static java.util.stream.Collectors.toSet;

final class JSONLDCodec {

	static Shape driver(final Shape shape) { // !!! caching
		return expanded(shape.redact( // add inferred constraints to drive json shorthands
				retain(Role),
				retain(Task),
				retain(Area),
				retain(Mode, Convey) // remove internal filtering shapes
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Optional<IRI> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new DatatypeProbe()));
	}

	static Optional<IRI> _clazz(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ClazzProbe()));
	}


	private static final class DatatypeProbe extends Probe<IRI> {

		@Override public IRI probe(final Datatype datatype) {
			return datatype.id();
		}

		@Override public IRI probe(final And and) {
			return type(and.shapes().stream());
		}

		@Override public IRI probe(final Or or) {
			return type(or.shapes().stream());
		}

		@Override public IRI probe(final When when) {
			return type(Stream.of(when.pass(), when.fail()));
		}

		private IRI type(final Stream<Shape> shapes) {

			final Set<IRI> names=shapes
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

	private static final class ClazzProbe extends Probe<IRI> {

		@Override public IRI probe(final Clazz clazz) {
			return clazz.id();
		}

		@Override public IRI probe(final And and) {
			return clazz(and.shapes().stream());
		}

		@Override public IRI probe(final Or or) {
			return clazz(or.shapes().stream());
		}

		@Override public IRI probe(final When when) {
			return clazz(Stream.of(when.pass(), when.fail()));
		}


		private IRI clazz(final Stream<Shape> shapes) {

			final Set<IRI> names=shapes
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Optional<Integer> minCount(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new MinCountProbe()));
	}

	static Optional<Integer> maxCount(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new MaxCountProbe()));
	}


	//// ;(jdk) replacing compareTo() with Math.min/max() causes a NullPointerException during Integer unboxing ////////

	private static final BinaryOperator<Integer> min=(x, y) ->
			x == null ? y : y == null ? x : x.compareTo(y) <= 0 ? x : y;

	private static final BinaryOperator<Integer> max=(x, y) ->
			x == null ? y : y == null ? x : x.compareTo(y) >= 0 ? x : y;


	private static final class MinCountProbe extends Probe<Integer> {

		@Override public Integer probe(final MinCount minCount) {
			return minCount.limit();
		}


		@Override public Integer probe(final And and) {
			return and.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, max);
		}

		@Override public Integer probe(final Or or) {
			return or.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, min);
		}

		@Override public Integer probe(final When when) {
			return min.apply(
					when.pass().map(this),
					when.fail().map(this));
		}

	}

	private static final class MaxCountProbe extends Probe<Integer> {

		@Override public Integer probe(final MaxCount maxCount) {
			return maxCount.limit();
		}


		@Override public Integer probe(final And and) {
			return and.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, min);
		}

		@Override public Integer probe(final Or or) {
			return or.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, max);
		}

		@Override public Integer probe(final When when) {
			return max.apply(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONLDCodec() {}

}
