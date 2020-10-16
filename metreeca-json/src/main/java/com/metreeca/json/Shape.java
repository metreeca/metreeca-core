/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.Value;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;


/**
 * Linked data shape constraint.
 */
public abstract class Shape {

	//// Shape Shorthands //////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape exactly(final Value... values) { return and(all(values), range(values)); }


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
	public final Shape then(final Shape... shapes) {

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return when(this, and(shapes));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if this shape is empty.
	 *
	 * @return {@code true} if this shape is equivalent to either {@link And#and()} or {@link Or#or()}; {@code false}
	 * otherwise
	 */
	public boolean empty() {
		return map(new ShapeEvaluator()) != null;
	}

	/**
	 * Redacts {@linkplain Guard guard} annotations of this shape.
	 *
	 * @param evaluators the guard evaluation functions; take as arguments a guard annotation and return {@code true},
	 *                   if the guarded shape is to be included in the redacted shape, {@code false} if it is to be
	 *                   removed, {@code null} if the guard is to be retained as is
	 *
	 * @return a copy of this shape redacted according to {@code evaluators}.
	 *
	 * @throws NullPointerException if {@code evaluators} is null or contains null elements
	 */
	@SafeVarargs public final Shape redact(final Function<Guard, Boolean>... evaluators) {

		if ( evaluators == null || Arrays.stream(evaluators).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null evaluators");
		}

		return map(new ShapeRedactor(evaluators));
	}

	/**
	 * Extends this shape with inferred constraints.
	 *
	 * @return a copy of this shape extended with inferred constraints
	 */
	public Shape expand() {
		return map(new ShapeInferencer());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract <V> V map(final Probe<V> probe);

	public final <V> V map(final Function<Shape, V> mapper) {

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

		public V probe(final Range range) { return probe((Shape)range); }

		public V probe(final Lang lang) { return probe((Shape)lang); }


		public V probe(final MinExclusive minExclusive) { return probe((Shape)minExclusive); }

		public V probe(final MaxExclusive maxExclusive) { return probe((Shape)maxExclusive); }

		public V probe(final MinInclusive minInclusive) { return probe((Shape)minInclusive); }

		public V probe(final MaxInclusive maxInclusive) { return probe((Shape)maxInclusive); }


		public V probe(final MinLength minLength) { return probe((Shape)minLength); }

		public V probe(final MaxLength maxLength) { return probe((Shape)maxLength); }

		public V probe(final Pattern pattern) { return probe((Shape)pattern); }

		public V probe(final Like like) { return probe((Shape)like); }

		public V probe(final Stem stem) { return probe((Shape)stem); }


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount) { return probe((Shape)minCount); }

		public V probe(final MaxCount maxCount) { return probe((Shape)maxCount); }


		public V probe(final All all) { return probe((Shape)all); }

		public V probe(final Any any) { return probe((Shape)any); }


		public V probe(final Localized localized) { return probe((Shape)localized); }


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
