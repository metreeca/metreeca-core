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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Redactor;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.format;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Parametric constraint.
 *
 * <p>States that the focus set meets this shape only if at least one of the externally defined values of an axis
 * variable is included in a given set of target values.</p>
 *
 * @see Redactor
 */
public final class Guard implements Shape {

	public static Guard guard(final IRI axis, final Value... values) {
		return guard(axis, asList(values));
	}

	public static Guard guard(final IRI axis, final Collection<? extends Value> values) {
		return new Guard(axis, values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI axis;
	private final Set<Value> values;


	private Guard(final IRI axis, final Collection<? extends Value> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis IRI");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.isEmpty() ) {
			throw new IllegalArgumentException("empty values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.axis=axis;
		this.values=new HashSet<>(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getAxis() {
		return axis;
	}

	public Set<Value> getValues() {
		return unmodifiableSet(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Applies this guard.
	 *
	 * @param shapes the shapes this guard is to be applied to
	 *
	 * @return this guard, if {@code shapes} is empty; a {@linkplain When#when(Shape, Shape) conditional} shape applying
	 * this guard to {@code shapes}, otherwise
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public Shape then(final Shape... shapes) {
		return then(asList(shapes));
	}

	/**
	 * Applies this guard.
	 *
	 * @param shapes the shapes this guard is to be applied to
	 *
	 * @return this guard, if {@code shapes} is empty; a {@linkplain When#when(Shape, Shape) conditional} shape applying
	 * this guard to {@code shapes}, otherwise
	 *
	 * @throws NullPointerException if {@code shapes} is null or contains null items
	 */
	public Shape then(final Collection<Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		return shapes.isEmpty() ? this : when(this, shapes.size() == 1 ? shapes.iterator().next() : and(shapes));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Guard
				&& axis.equals(((Guard)object).axis)
				&& values.equals(((Guard)object).values);
	}

	@Override public int hashCode() {
		return axis.hashCode()^values.hashCode();
	}

	@Override public String toString() {
		return "guard("+format(axis)+",\n"
				+values.stream().map(v -> indent(format(v))).collect(joining(",\n"))+"\n)";
	}

}
