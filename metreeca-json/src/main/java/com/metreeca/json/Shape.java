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

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.In.in;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static java.util.Arrays.asList;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	/**
	 * Retrieves the default shape asset factory.
	 *
	 * @return the default shape factory, which returns an {@linkplain Or#or() empty disjunction}, that is a shape
	 * the always fail to validate
	 */
	public static Supplier<Shape> shape() {
		return Or::or;
	}


	//// Shape Shorthands //////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Value... values) { return and(all(values), in(values)); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Removes annotations.
	 *
	 * @return a copy of this shape without {@linkplain Meta annotations}.
	 */
	public default Shape constraints() {
		return map(new Probe<Shape>() {

			@Override public Shape probe(final Meta meta) { return and(); }

			@Override public Shape probe(final Field field) {
				return field(field.name(), field.shape().map(this));
			}

			@Override public Shape probe(final And and) {
				return and(and.shapes().stream().map(this));
			}

			@Override public Shape probe(final Or or) {
				return or(or.shapes().stream().map(this));
			}

			@Override public Shape probe(final When when) {
				return when(when.test().map(this), when.pass().map(this), when.fail().map(this));
			}

			@Override public Shape probe(final Shape shape) { return shape; }

		});
	}


	/**
	 * Creates a conditional shape.
	 *
	 * @param shapes the shapes this shape is to be applied as a test condition
	 *
	 * @return a {@linkplain When#when(Shape, Shape) conditional} shape applying this shape as test condition to {@code
	 * shapes}
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public default Shape then(final Shape... shapes) {
		return then(asList(shapes));
	}

	/**
	 * Creates a conditional shape.
	 *
	 * @param shapes the shapes this shape is to be applied as a test condition
	 *
	 * @return a {@linkplain When#when(Shape, Shape) conditional} shape applying this shape as test condition to {@code
	 * shapes}
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public default Shape then(final Collection<Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		return when(this, shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V map(final Probe<V> probe);

	public default <V> V map(final Function<Shape, V> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return mapper.apply(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Shape probe.
	 *
	 * <p>Generates a result by probing shapes.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public abstract static class Probe<V> implements Function<Shape, V> {

		public V apply(final Shape shape) {
			return shape == null ? null : shape.map(this);
		}


		//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Meta meta) { return probe((Shape)meta); }

		public V probe(final Guard guard) { return probe((Shape)guard); }


		//// Value Constraints /////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Datatype datatype) { return probe((Shape)datatype); }

		public V probe(final Clazz clazz) { return probe((Shape)clazz); }


		public V probe(final MinExclusive minExclusive) { return probe((Shape)minExclusive); }

		public V probe(final MaxExclusive maxExclusive) { return probe((Shape)maxExclusive); }

		public V probe(final MinInclusive minInclusive) { return probe((Shape)minInclusive); }

		public V probe(final MaxInclusive maxInclusive) { return probe((Shape)maxInclusive); }


		public V probe(final MinLength minLength) { return probe((Shape)minLength); }

		public V probe(final MaxLength maxLength) { return probe((Shape)maxLength); }

		public V probe(final Pattern pattern) { return probe((Shape)pattern); }

		public V probe(final Like like) { return probe((Shape)like); }


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount) { return probe((Shape)minCount); }

		public V probe(final MaxCount maxCount) { return probe((Shape)maxCount); }

		public V probe(final In in) { return probe((Shape)in); }

		public V probe(final All all) { return probe((Shape)all); }

		public V probe(final Any any) { return probe((Shape)any); }


		//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Field field) { return probe((Shape)field); }


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and) { return probe((Shape)and); }

		public V probe(final Or or) { return probe((Shape)or); }

		public V probe(final When when) { return probe((Shape)when); }


		//// Fallback //////////////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Probes a generic shape.
		 *
		 * @param shape the generic shape to be probed
		 *
		 * @return the result generated by probing {@code shape}; by default {@code null}
		 */
		public V probe(final Shape shape) { return null; }

	}

}
