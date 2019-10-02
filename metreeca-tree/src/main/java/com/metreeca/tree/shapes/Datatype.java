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

package com.metreeca.tree.shapes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Inspector;

import java.util.*;

import static java.util.stream.Collectors.toSet;


/**
 * Datatype value constraint.
 *
 * <p>States that each value in the focus set has a given datatype.</p>
 */
public final class Datatype implements Shape {

	/**
	 * Creates a datatype constraint.
	 *
	 * @param name the expected datatype name
	 *
	 * @return a new datatype constraint for the provided {@code name}
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public static Datatype datatype(final Object name) {
		return new Datatype(name);
	}

	public static Optional<Object> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new TypeProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object name;


	private Datatype(final Object name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		this.name=name;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Object getName() {
		return name;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Datatype
				&& name.equals(((Datatype)object).name);
	}

	@Override public int hashCode() {
		return name.hashCode();
	}

	@Override public String toString() {
		return "datatype("+name+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TypeProbe extends Inspector<Object> {

		@Override public Object probe(final Datatype datatype) {
			return datatype.getName();
		}

		@Override public Object probe(final And and) {
			return type(and.getShapes());
		}

		@Override public Object probe(final Or or) {
			return type(or.getShapes());
		}


		private Object type(final Collection<Shape> shapes) {

			final Set<Object> names=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

}
