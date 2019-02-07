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

package com.metreeca.form;

import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.function.Function;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.When.when;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Arrays.asList;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	public static boolean empty(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return And.pass().equals(shape);
	}

	/**
	 * Tests if a shape is a constant.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true}, if {@code shape} is an {@linkplain And#pass() empty conjunction}; {@code false}, if {@code
	 * shape} is an {@linkplain Or#fail() empty disjunction}; {@code null}, otherwise; {@linkplain Meta metadata}
	 * annotation are ignored in the evaluation process
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static Boolean constant(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final class Evaluator extends Traverser<Boolean> {

			@Override public Boolean probe(final Meta meta) {
				return true;
			}


			@Override public Boolean probe(final Field field) {
				return null;
			}

			@Override public Boolean probe(final And and) {
				return and.getShapes().stream()
						.filter(shape -> !(shape instanceof Meta))
						.map(shape -> shape.map(this))
						.reduce(true, (x, y) -> x == null || y == null ? null : x && y);
			}

			@Override public Boolean probe(final Or or) {
				return or.getShapes().stream()
						.filter(shape -> !(shape instanceof Meta))
						.map(shape -> shape.map(this))
						.reduce(false, (x, y) -> x == null || y == null ? null : x || y);
			}

			@Override public Boolean probe(final When when) {

				final Boolean test=when.getTest().map(this);
				final Boolean pass=when.getPass().map(this);
				final Boolean fail=when.getFail().map(this);

				return TRUE.equals(test) ? pass
						: FALSE.equals(test) ? fail
						: TRUE.equals(pass) && TRUE.equals(fail) ? TRUE
						: FALSE.equals(pass) && FALSE.equals(fail) ? FALSE
						: null;
			}

		}

		return shape.map(new Evaluator());
	}


	//// Shorthands ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Value... values) { return and(all(values), in(values)); }


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Guard role(final Value... roles) { return guard(Form.role, roles); }

	public static Guard task(final Value... tasks) { return guard(Form.task, tasks); }

	public static Guard view(final Value... views) { return guard(Form.view, views); }

	public static Guard mode(final Value... modes) { return guard(Form.mode, modes); }


	public static Guard create() { return task(Form.create); }

	public static Guard relate() { return task(Form.relate); }

	public static Guard update() { return task(Form.update); }

	public static Guard delete() { return task(Form.delete); }


	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Guard server() { return task(Form.relate, Form.delete); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Guard client() { return task(Form.create, Form.relate, Form.delete); }

	/*
	 * Marks shapes as internal use only.
	 */
	public static Guard hidden() { return task(); }


	public static Guard digest() { return view(Form.digest); }

	public static Guard detail() { return view(Form.detail); }


	public static Guard verify() { return mode(Form.verify); }

	public static Guard filter() { return mode(Form.filter); }


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
