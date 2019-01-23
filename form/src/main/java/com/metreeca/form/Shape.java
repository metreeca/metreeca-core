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

import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.probes.Visitor;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Maps;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Sets.set;

import static java.util.Arrays.asList;


/**
 * Linked data shape constraint.
 */
public interface Shape {

	public static final Shape Pass=and();
	public static final Shape Fail=or();


	public static Shape pass() {
		return Pass;
	}

	public static boolean pass(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Pass.equals(shape);
	}


	public static Shape fail() {
		return Fail;
	}

	public static boolean fail(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Fail.equals(shape);
	}


	public static boolean empty(final Shape shape) {
		return pass(shape) || fail(shape);
	}


	//// Shorthands ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape required() { return and(minCount(1), maxCount(1)); }

	public static Shape optional() { return maxCount(1); }

	public static Shape repeatable() { return minCount(1); }

	public static Shape multiple() { return and(); }


	public static Shape only(final Value... values) {
		return only(asList(values));
	}

	public static Shape only(final Collection<Value> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		if ( values.isEmpty() ) {
			throw new IllegalArgumentException("empty values");
		}

		return and(all(values), in(values));
	}


	//// Parametric Shapes /////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape role(final Set<? extends Value> roles, final Shape... shapes) {
		return shape(Form.role, roles, asList(shapes));
	}

	public static Shape role(final Set<? extends Value> roles, final Collection<Shape> shapes) {
		return shape(Form.role, roles, shapes);
	}


	public static Shape create(final Shape... shapes) { return create(asList(shapes));}

	public static Shape create(final Collection<Shape> shapes) { return shape(Form.task, set(Form.create), shapes); }

	public static Shape relate(final Shape... shapes) { return relate(asList(shapes));}

	public static Shape relate(final Collection<Shape> shapes) { return shape(Form.task, set(Form.relate), shapes); }

	public static Shape update(final Shape... shapes) {return update(asList(shapes));}

	public static Shape update(final Collection<Shape> shapes) { return shape(Form.task, set(Form.update), shapes); }

	public static Shape delete(final Shape... shapes) {return delete(asList(shapes));}

	public static Shape delete(final Collection<Shape> shapes) { return shape(Form.task, set(Form.delete), shapes); }


	/**
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server(final Shape... shapes) { return server(asList(shapes)); }

	/**
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server(final Collection<Shape> shapes) {
		return shape(Form.task, set(Form.relate, Form.delete), shapes);
	}

	/**
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client(final Shape... shapes) { return client(asList(shapes)); }

	/**
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client(final Collection<Shape> shapes) {
		return shape(Form.task, set(Form.create, Form.relate, Form.delete), shapes);
	}


	public static Shape digest(final Shape... shapes) { return digest(asList(shapes)); }

	public static Shape digest(final Collection<Shape> shapes) { return shape(Form.view, set(Form.digest), shapes); }

	public static Shape detail(final Shape... shapes) {return detail(asList(shapes));}

	public static Shape detail(final Collection<Shape> shapes) { return shape(Form.view, set(Form.detail), shapes); }


	public static Shape verify(final Shape... shapes) { return verify(asList(shapes)); }

	public static Shape verify(final Collection<Shape> shapes) { return shape(Form.mode, set(Form.verify), shapes); }

	public static Shape filter(final Shape... shapes) { return filter(asList(shapes)); }

	public static Shape filter(final Collection<Shape> shapes) { return shape(Form.mode, set(Form.filter), shapes); }


	public static Shape shape(final IRI variable, final Collection<? extends Value> values, final Shape... shapes) {
		return shape(variable, values, asList(shapes));
	}

	public static Shape shape(final IRI variable, final Collection<? extends Value> values, final Collection<Shape> shapes) {
		return shapes.isEmpty() ? when(variable, values)
				: Option.option(when(variable, values), shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
	}


	//// Parametric Probes /////////////////////////////////////////////////////////////////////////////////////////////

	public static Probe<Shape> role(final Value... roles) {
		return probe(Form.role, new HashSet<>(asList(roles)));
	}

	public static Probe<Shape> role(final Set<? extends Value> roles) {
		return probe(Form.role, roles);
	}

	public static Probe<Shape> task(final Value task) {
		return probe(Form.task, set(task));
	}

	public static Probe<Shape> view(final Value view) {
		return probe(Form.view, set(view));
	}

	public static Probe<Shape> mode(final Value mode) {
		return probe(Form.mode, set(mode));
	}


	public static Probe<Shape> probe(final IRI variable, final Set<? extends Value> values) {
		return new Visitor<Shape>() {
			@Override public Shape probe(final Shape shape) {
				return shape
						.map(new Redactor(Maps.map(entry(variable, values))))
						.map(new Optimizer());
			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V map(final Probe<V> probe);


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

		public V probe(final When when);


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

		public V probe(final Trait trait);

		public V probe(final Virtual virtual);


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V probe(final And and);

		public V probe(final Or or);

		public V probe(final Option option);

	}

}
