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
 * Type value constraint.
 *
 * <p>States that each value in the focus set has a given extended RDF datatype.</p>
 */
public final class Type implements Shape {

	/**
	 * Creates a name constraint.
	 *
	 * @param name the expected type name
	 *
	 * @return a new name constraint for the provided {@code name}
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public static Type type(final String name) {
		return new Type(name);
	}

	public static Optional<String> type(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new TypeProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String name;


	private Type(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		this.name=name;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getName() {
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
		return this == object || object instanceof Type
				&& name.equals(((Type)object).name);
	}

	@Override public int hashCode() {
		return name.hashCode();
	}

	@Override public String toString() {
		return "type("+name+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TypeProbe extends Inspector<String> {

		@Override public String probe(final Type type) {
			return type.getName();
		}

		@Override public String probe(final And and) {
			return type(and.getShapes());
		}

		@Override public String probe(final Or or) {
			return type(or.getShapes());
		}


		private String type(final Collection<Shape> shapes) {

			final Set<String> names=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

}
