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

package com.metreeca.tree;

import com.metreeca.tree.shapes.*;

import java.util.Collection;
import java.util.function.Function;

import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.When.when;

import static java.util.Arrays.asList;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	//// Shape Metadata ////////////////////////////////////////////////////////////////////////////////////////////////

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
	 * Textual annotation.
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


	//// Parametric Axes and Values ////////////////////////////////////////////////////////////////////////////////////

	public static final String Role="role";
	public static final String Task="task";
	public static final String View="view";
	public static final String Mode="mode";

	public static final String Create="create";
	public static final String Relate="relate";
	public static final String Update="update";
	public static final String Delete="delete";

	public static final String Digest="digest";
	public static final String Detail="detail";

	public static final String Convey="convey";
	public static final String Filter="filter";


	//// Shorthands ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Object... values) { return and(all(values), in(values)); }


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Guard role(final String... roles) { return guard(Role, roles); }

	public static Guard task(final String... tasks) { return guard(Task, tasks); }

	public static Guard view(final String... views) { return guard(View, views); }

	public static Guard mode(final String... modes) { return guard(Mode, modes); }


	public static Guard create() { return task(Create); }

	public static Guard relate() { return task(Relate); }

	public static Guard update() { return task(Update); }

	public static Guard delete() { return task(Delete); }


	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Guard server() { return task(Relate, Delete); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Guard client() { return task(Create, Relate, Delete); }


	public static Guard digest() { return view(Digest); }

	public static Guard detail() { return view(Detail); }


	public static Guard convey() { return mode(Convey); }

	public static Guard filter() { return mode(Filter); }


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
	 * @return this shape, if {@code shapes} is empty; a {@linkplain When#when(Shape, Shape) conditional} shape applying
	 * this shape as test condition to {@code shapes}, otherwise
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
	 * @return this shape, if {@code shapes} is empty; a {@linkplain When#when(Shape, Shape) conditional} shape applying
	 * this shape as test condition to {@code shapes}, otherwise
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

		return shapes.isEmpty() ? this : when(this, shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
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

		//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Meta meta);

		public V probe(final Guard guard);


		//// Term Constraints //////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Type Type);

		public V probe(final Kind kind);


		public V probe(final MinExclusive minExclusive);

		public V probe(final MaxExclusive maxExclusive);

		public V probe(final MinInclusive minInclusive);

		public V probe(final MaxInclusive maxInclusive);


		public V probe(final MinLength minLength);

		public V probe(final MaxLength maxLength);

		public V probe(final Pattern pattern);

		public V probe(final Like like);


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V probe(final MinCount minCount);

		public V probe(final MaxCount maxCount);

		public V probe(final In in);

		public V probe(final All all);

		public V probe(final Any any);


		//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

		public V probe(final Field field);


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and);

		public V probe(final Or or);

		public V probe(final When when);

	}

}
