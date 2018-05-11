/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;

import com.metreeca.spec.probes.Optimizer;
import com.metreeca.spec.probes.Redactor;
import com.metreeca.spec.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.jeep.Jeep.set;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.In.in;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.MinCount.minCount;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.When.when;

import static java.util.Arrays.asList;


public interface Shape {

	public static boolean empty(final Shape shape) { // !!! review

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.equals(and()) || shape.equals(or());
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

	public static Shape create(final Shape... shapes) { return create(asList(shapes));}

	public static Shape create(final Collection<Shape> shapes) { return shape(Spec.task, set(Spec.create), shapes); }

	public static Shape relate(final Shape... shapes) { return relate(asList(shapes));}

	public static Shape relate(final Collection<Shape> shapes) { return shape(Spec.task, set(Spec.relate), shapes); }

	public static Shape update(final Shape... shapes) {return update(asList(shapes));}

	public static Shape update(final Collection<Shape> shapes) { return shape(Spec.task, set(Spec.update), shapes); }

	public static Shape delete(final Shape... shapes) {return delete(asList(shapes));}

	public static Shape delete(final Collection<Shape> shapes) { return shape(Spec.task, set(Spec.delete), shapes); }


	/**
	 * Marks shapes as server-defined read-only.
	 *
	 * @param shapes
	 *
	 * @return
	 */
	public static Shape server(final Shape... shapes) { return server(asList(shapes)); }

	/**
	 * Marks shapes as server-defined read-only.
	 *
	 * @param shapes
	 *
	 * @return
	 */
	public static Shape server(final Collection<Shape> shapes) {
		return shape(Spec.task, set(Spec.relate, Spec.delete), shapes);
	}

	/**
	 * Marks shapes as client-defined write-once.
	 *
	 * @param shapes
	 *
	 * @return
	 */
	public static Shape client(final Shape... shapes) { return client(asList(shapes)); }

	/**
	 * Marks shapes as client-defined write-once.
	 *
	 * @param shapes
	 *
	 * @return
	 */
	public static Shape client(final Collection<Shape> shapes) {
		return shape(Spec.task, set(Spec.create, Spec.relate, Spec.delete), shapes);
	}


	public static Shape digest(final Shape... shapes) { return digest(asList(shapes)); }

	public static Shape digest(final Collection<Shape> shapes) { return shape(Spec.view, set(Spec.digest), shapes); }

	public static Shape detail(final Shape... shapes) {return detail(asList(shapes));}

	public static Shape detail(final Collection<Shape> shapes) { return shape(Spec.view, set(Spec.detail), shapes); }


	public static Shape verify(final Shape... shapes) { return verify(asList(shapes)); }

	public static Shape verify(final Collection<Shape> shapes) { return shape(Spec.mode, set(Spec.verify), shapes); }

	public static Shape filter(final Shape... shapes) { return filter(asList(shapes)); }

	public static Shape filter(final Collection<Shape> shapes) { return shape(Spec.mode, set(Spec.filter), shapes); }


	public static Shape shape(final IRI variable, final Set<Value> values, final Collection<Shape> shapes) {
		return shapes.isEmpty() ? when(variable, values)
				: test(when(variable, values), shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
	}


	//// Parametric Probes /////////////////////////////////////////////////////////////////////////////////////////////

	public static Probe<Shape> role(final Value... roles) {
		return probe(Spec.role, new HashSet<>(asList(roles)));
	}

	public static Probe<Shape> role(final Set<? extends Value> roles) {
		return probe(Spec.role, roles);
	}

	public static Probe<Shape> task(final Value task) {
		return probe(Spec.task, set(task));
	}

	public static Probe<Shape> view(final Value view) {
		return probe(Spec.view, set(view));
	}

	public static Probe<Shape> mode(final Value mode) {
		return probe(Spec.mode, set(mode));
	}


	public static Probe<Shape> probe(final IRI variable, final Set<? extends Value> values) {
		return new Probe<Shape>() {
			@Override protected Shape fallback(final Shape shape) {
				return shape
						.accept(new Redactor(map(entry(variable, values))))
						.accept(new Optimizer());
			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V accept(final Probe<V> probe);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract static class Probe<V> {

		//// Term Constraints //////////////////////////////////////////////////////////////////////////////////////////

		public V visit(final Datatype datatype) { return fallback(datatype); }

		public V visit(final Clazz clazz) { return fallback(clazz); }


		public V visit(final MinExclusive minExclusive) { return fallback(minExclusive); }

		public V visit(final MaxExclusive maxExclusive) { return fallback(maxExclusive); }

		public V visit(final MinInclusive minInclusive) { return fallback(minInclusive); }

		public V visit(final MaxInclusive maxInclusive) { return fallback(maxInclusive); }


		public V visit(final MinLength minLength) { return fallback(minLength); }

		public V visit(final MaxLength maxLength) { return fallback(maxLength); }

		public V visit(final Pattern pattern) { return fallback(pattern); }

		public V visit(final Like like) { return fallback(like); }


		public V visit(final Custom custom) { return fallback(custom); }


		//// Set Constraints ///////////////////////////////////////////////////////////////////////////////////////////

		public V visit(final MinCount minCount) { return fallback(minCount); }

		public V visit(final MaxCount maxCount) { return fallback(maxCount); }

		public V visit(final In in) { return fallback(in); }

		public V visit(final All all) { return fallback(all); }

		public V visit(final Any any) { return fallback(any); }


		//// Structural Constraints ////////////////////////////////////////////////////////////////////////////////////

		public V visit(final Trait trait) { return fallback(trait); }

		public V visit(final Virtual virtual) { return fallback(virtual); }


		//// Logical Constraints ///////////////////////////////////////////////////////////////////////////////////////

		public V visit(final And and) { return fallback(and); }

		public V visit(final Or or) { return fallback(or); }

		public V visit(final Test test) { return fallback(test); }

		public V visit(final When when) { return fallback(when); }


		//// Annotations ///////////////////////////////////////////////////////////////////////////////////////////////

		public V visit(final Alias alias) { return fallback(alias); }

		public V visit(final Label label) { return fallback(label); }

		public V visit(final Notes notes) { return fallback(notes); }

		public V visit(final Placeholder placeholder) { return fallback(placeholder); }

		public V visit(final Default dflt) { return fallback(dflt); }

		public V visit(final Hint hint) { return fallback(hint); }

		public V visit(final Group group) { return fallback(group); }


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		protected V fallback(final Shape shape) { return null; }

	}

}
