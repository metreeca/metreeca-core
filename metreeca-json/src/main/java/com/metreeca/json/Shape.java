/*
 * Copyright © 2013-2021 Metreeca srl
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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.Mode;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;


/**
 * Linked data shape constraint.
 */
public abstract class Shape {

	/**
	 * The default predicate linking resource collections to their items.
	 */
	public static final IRI Contains=LDP.CONTAINS;


	//// Shape Shorthands //////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape exactly(final Value... values) { return and(all(values), range(values)); }


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
	 * Traverses this shape.
	 *
	 * @param path the property path to be traversed
	 *
	 * @return the optional shape reached following {@code path} from this shape or an empty optional, if {@code path}
	 * includes unknown steps
	 *
	 * @throws NullPointerException if {@code path} is null or contains null elements
	 */
	public Optional<Shape> walk(final Collection<IRI> path) {

		if ( path == null || path.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path");
		}

		Optional<Shape> shape=Optional.of(this);

		for (final IRI step : path) {
			shape=shape.flatMap(s -> field(s, step)).map(Field::shape);
		}

		return shape;
	}

	/**
	 * Identifies statements implied by this shape.
	 *
	 * @param focus the initial focus values for shape traversal
	 *
	 * @return a set of statements implied by this shape when recursively traversed starting from {@code focus} values
	 *
	 * @throws NullPointerException if {@code focus} is null or contains null elements
	 */
	public Set<Statement> outline(final Value... focus) {

		if ( focus == null || stream(focus).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null focus");
		}

		return redact(Mode).map(new ShapeOutliner(focus)).collect(toSet());
	}


	/**
	 * Extends this shape with inferred constraints.
	 *
	 * @return a copy of this shape extended with inferred constraints
	 */
	public Shape expand() {
		return map(new ShapeInferencer());
	}


	/**
	 * Localizes this shape.
	 *
	 * @param tags the target language tag set
	 *
	 * @return a copy of this shape where tag sets of {@link Lang lang} shapes are replaced with their intersection with
	 * {@code tags} or {@code tags}, if the intersection is empty; if {@code tags} is empty or contains a wildcard
	 * ("{@code *}") the shape is not modified
	 *
	 * @throws NullPointerException if {@code tags} is null or contains null elements
	 */
	public Shape localize(final String... tags) {

		if ( tags == null || stream(tags).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		return localize(asList(tags));
	}

	/**
	 * Localizes this shape.
	 *
	 * @param tags the target language tag set
	 *
	 * @return a copy of this shape where tag sets of {@link Lang lang} shapes are replaced with their intersection with
	 * {@code tags} or {@code tags}, if the intersection is empty; if {@code tags} is empty or contains a wildcard
	 * ("{@code *}") the shape is not modified
	 *
	 * @throws NullPointerException if {@code tags} is null or contains null elements
	 */
	public Shape localize(final Collection<String> tags) {

		if ( tags == null || tags.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null tags");
		}

		return tags.isEmpty() || tags.contains("*") ? this : map(new ShapeLocalizer(tags));
	}


	/**
	 * Redacts guards in this shape.
	 *
	 * @param axis the axis to be retained
	 *
	 * @return a copy of this shape where {@link Guard} shapes along {@code axis} are selectively replaced with an empty
	 * {@link And#and() and} shape
	 *
	 * @throws NullPointerException if {@code axis} is null
	 */
	public Shape redact(final String axis) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		return map(new ShapeRedactor(axis, null));
	}

	/**
	 * Redacts guards in this shape.
	 *
	 * @param axis   the axis to be retained
	 * @param values the axis values to be retained
	 *
	 * @return a copy of this shape where {@link Guard} shapes along {@code axis} are selectively replaced with an empty
	 * {@link And#and() and} shape, if their {@link Guard#values() values set} intersect {@code values}, or an empty
	 * {@link Or#or() and} shape, otherwise
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or contains null elements
	 */
	public Shape redact(final String axis, final Object... values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null || stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return map(new ShapeRedactor(axis, asList(values)));
	}

	/**
	 * Redacts guards in this shape.
	 *
	 * @param axis   the axis to be retained
	 * @param values the axis values to be retained
	 *
	 * @return a copy of this shape where {@link Guard} shapes along {@code axis} are selectively replaced with an empty
	 * {@link And#and() and} shape, if their {@link Guard#values() values set} intersect {@code values}, or an empty
	 * {@link Or#or() and} shape, otherwise
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or contains null elements
	 */
	public Shape redact(final String axis, final Collection<Object> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return map(new ShapeRedactor(axis, values));
	}


	/**
	 * Extracts the conveying form of this shape.
	 *
	 * @return a copy of this shape where constraint shapes are retained only inside {@linkplain Guard#convey(Shape...)
	 * conveying} {@linkplain #then(Shape...) conditional} shapes
	 */
	public Shape convey() {
		return map(new ShapePruner(false));
	}

	/**
	 * Extracts the filtering form of this shape.
	 *
	 * @param anchor a target value filtering matches will be linked to
	 *
	 * @return a copy of this shape where constraint shapes are retained only inside {@linkplain Guard#filter(Shape...)
	 * filtering} {@linkplain #then(Shape...) conditional} shapes, extended to link matches to the {@code anchor} value
	 *
	 * @throws NullPointerException if {@code anchor} is null
	 */
	public Shape filter(final Value anchor) {

		if ( anchor == null ) {
			throw new NullPointerException("null anchor");
		}

		return map(new ShapePruner(true)).map(shape -> anchor.isIRI() && anchor.stringValue().endsWith("/")

				// container: connect to the anchor using ldp:contains, unless otherwise specified in the shape

				? shape.empty() ? and(field(inverse(Contains), all(focus())), shape) : shape

				// resource: constraint to the anchor

				: and(all(focus()), shape)

		);
	}


	/**
	 * Uniquely label fields in this shape.
	 *
	 * @param labels a supplier of unique labels
	 *
	 * @return a copy of this shape where fields are assigned a unique label supplied by {@code labels}
	 *
	 * @throws NullPointerException if {@code labels} is null
	 */
	public Shape label(final Supplier<String> labels) {

		if ( labels == null ) {
			throw new NullPointerException("null labels");
		}

		return map(new ShapeLabeller(labels));
	}

	/**
	 * Resolve focus values in this shape.
	 *
	 * @param focus the value {@linkplain Focus focus} values should be resolved against
	 *
	 * @return a copy of this shape where focus values are replaced with the value obtained by resolving them against
	 * {@code focus}
	 *
	 * @throws IllegalArgumentException if {@code focus} is null
	 */
	public Shape resolve(final Value focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return map(new ShapeResolver(focus));
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
	public final Shape then(final Shape... shapes) {

		if ( shapes == null || stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return shapes.length == 0 ? this : when(this, and(shapes));
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

		@Override public final V apply(final Shape shape) {
			return shape == null ? null : shape.map(this);
		}


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

		public V probe(final Link link) { return probe((Shape)link); }


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Guard guard) { return probe((Shape)guard); }

		public V probe(final When when) { return probe((Shape)when); }

		public V probe(final And and) { return probe((Shape)and); }

		public V probe(final Or or) { return probe((Shape)or); }


		//// Fallback //////////////////////////////////////////////////////////////////////////////////////////////////

		/**
		 * Probes a generic shape.
		 *
		 * @param shape the generic shape to be probed
		 *
		 * @return the result generated by probing {@code shape}; by default {@code null}
		 */
		protected V probe(final Shape shape) { return null; }

	}

}
