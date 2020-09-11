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

import java.net.URI;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Guard.guard;
import static com.metreeca.json.shapes.In.in;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
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


	//// Shape Metadata ///////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Alias annotation.
	 *
	 * <p>The associated string value provides an alternate property name for reporting values for
	 * the enclosing shape (e.g. in the context of JSON-based serialization results).</p>
	 */
	public static final String Alias="alias";

	/**
	 * Label annotation.
	 *
	 * <p>The associated string value provides a human-readable textual label for the enclosing shape.</p>
	 */
	public static final String Label="label";

	/**
	 * Notes annotation.
	 *
	 * <p>The associated string value provides a human-readable textual description for the enclosing shape.</p>
	 */
	public static final String Notes="notes";

	/**
	 * Placeholder annotation.
	 *
	 * <p>The associated string value provides a human-readable textual placeholder for the expected values
	 * of the enclosing shape.</p>
	 */
	public static final String Placeholder="placeholder";

	/**
	 * Default value annotation.
	 *
	 * <p>The associated object value provides the default for the expected values of enclosing shape.</p>
	 */
	public static final String Default="default";

	/**
	 * Hint annotation.
	 *
	 * <p>The associated string value identifies a resource hinting at possible values for the enclosing shape.</p>
	 */
	public static final String Hint="hint";

	/**
	 * Group annotation.
	 *
	 * <p>Identifies the enclosing shape as a group for presentation purposes; the associated value provides a
	 * client-dependent suggested representation mode (list, form, tabbed panes, …).</p>
	 */
	public static final String Group="group"; // !!! define standard representations hints

	/**
	 * Indexing annotation.
	 *
	 * <p>The associated boolean value provides a storage indexing hint for the enclosing shape.</p>
	 */
	public static final String Index="index";


	//// Parametric Axes and Values ///////////////////////////////////////////////////////////////////////////////////

	public static final String Role="role";
	public static final String Task="task";
	public static final String Area="area";
	public static final String Mode="mode";

	public static final String Create="create";
	public static final String Relate="relate";
	public static final String Update="update";
	public static final String Delete="delete";

	public static final String Holder="holder";
	public static final String Digest="digest";
	public static final String Detail="detail";

	public static final String Convey="convey";
	public static final String Filter="filter";


	//// Shorthands ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Object... values) { return and(all(values), in(values)); }


	//// Parametric Guards ////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape role(final Object... roles) { return guard(Role, roles); }

	public static Shape task(final Object... tasks) { return guard(Task, tasks); }

	public static Shape area(final Object... areas) { return guard(Area, areas); }

	public static Shape mode(final Object... modes) { return guard(Mode, modes); }


	public static Shape create() { return task(Create); }

	public static Shape relate() { return task(Relate); }

	public static Shape update() { return task(Update); }

	public static Shape delete() { return task(Delete); }


	/*
	 * Marks shapes as server-defined internal.
	 */
	public static Shape hidden() { return task(Delete); }

	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server() { return task(Relate, Delete); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client() { return task(Create, Relate, Delete); }


	public static Shape holder() { return area(Holder); }

	public static Shape member() { return area(Digest, Detail); }

	public static Shape digest() { return area(Digest); }

	public static Shape detail() { return area(Detail); }


	public static Shape convey() { return mode(Convey); }

	public static Shape filter() { return mode(Filter); }


	//// Relative IRIs ////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a target focus value.
	 *
	 * @return a focus value resolving to the target IRI of a shape-driven operation
	 */
	public static Focus focus() {
		return iri -> iri;
	}

	/**
	 * Creates a relative focus value.
	 *
	 * @param iri the relative IRI of the focus value
	 *
	 * @return a focus value resolving {@code iri} against the target IRI of a shape-driven operation; trailing slashes
	 * in the resolved IRI are removed unless {@code iri} includes one
	 *
	 * @throws NullPointerException if {@code iri} is null
	 */
	public static Focus focus(final String iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		final Focus resolve=path -> URI.create(path).resolve(iri).toString();
		final Focus convert=path -> path.endsWith("/") ? path.substring(0, path.length()-1) : path;

		return iri.isEmpty() ? path -> path
				: iri.endsWith("/") ? resolve
				: resolve.then(convert);
	}


	//// Evaluation ///////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests if a shape is always matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal to an {@linkplain And#and() empty conjunction}, ignoring
	 * {@linkplain Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean pass(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Boolean.TRUE.equals(shape.map(Evaluator.Instance));
	}

	/**
	 * Tests if a shape is never matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal to an {@linkplain Or#or() empty disjunction}, ignoring
	 * {@linkplain
	 * Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean fail(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Boolean.FALSE.equals(shape.map(Evaluator.Instance));
	}

	/**
	 * Tests if a shape is empty.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true} if {@code shape} is equal either to an {@linkplain And#and() empty conjunction} or to an
	 * {@linkplain Or#or() empty disjunction}, ignoring {@linkplain Meta annotations}; {@code false} otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean empty(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(Evaluator.Instance) != null;
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
	 * Use this shape as a test condition.
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
	 * Use this shape as a test condition.
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


	/**
	 * Shape probe.
	 *
	 * <p>Generates a result by probing shapes.</p>
	 *
	 * @param <V> the type of the generated result value
	 */
	public static interface Probe<V> {

		//// Annotations //////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Meta meta);

		public V probe(final Guard guard);


		//// Term Constraints /////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Datatype datatype);

		public V probe(final Clazz clazz);


		public V probe(final MinExclusive minExclusive);

		public V probe(final MaxExclusive maxExclusive);

		public V probe(final MinInclusive minInclusive);

		public V probe(final MaxInclusive maxInclusive);


		public V probe(final MinLength minLength);

		public V probe(final MaxLength maxLength);

		public V probe(final Pattern pattern);

		public V probe(final Like like);


		//// Set Constraints //////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount);

		public V probe(final MaxCount maxCount);


		public V probe(final In in);

		public V probe(final All all);

		public V probe(final Any any);


		//// Structural Constraints ///////////////////////////////////////////////////////////////////////////////////

		public V probe(final Field field);


		//// Logical Constraints //////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and);

		public V probe(final Or or);

		public V probe(final When when);

	}

}
