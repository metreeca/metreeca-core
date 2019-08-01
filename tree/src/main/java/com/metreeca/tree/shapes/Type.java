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
	 * Creates a type constraint.
	 *
	 * @param type the expected value type
	 *
	 * @return a new type constraint for the provided {@code type}
	 *
	 * @throws NullPointerException if {@code type} is null
	 */
	public static Type type(final Object type) {
		return new Type(type);
	}

	public static Optional<Object> type(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new TypeProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object type;


	private Type(final Object type) {

		if ( type == null ) {
			throw new NullPointerException("null type");
		}

		this.type=type;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Object getType() {
		return type;
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
				&& type.equals(((Type)object).type);
	}

	@Override public int hashCode() {
		return type.hashCode();
	}

	@Override public String toString() {
		return "type("+type+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TypeProbe extends Inspector<Object> {

		@Override public Object probe(final Type type) {
			return type.getType();
		}

		@Override public Object probe(final And and) {
			return type(and.getShapes());
		}

		@Override public Object probe(final Or or) {
			return type(or.getShapes());
		}


		private Object type(final Collection<Shape> shapes) {

			final Set<Object> types=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return types.size() == 1 ? types.iterator().next() : null;

		}

	}

}
