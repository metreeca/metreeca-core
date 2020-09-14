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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static java.util.stream.Collectors.toSet;


/**
 * Datatype value constraint.
 *
 * <p>States that each value in the focus set has a given datatype.</p>
 */
public final class Datatype extends Shape {

	/**
	 * Creates a datatype constraint.
	 *
	 * @param name the expected datatype name
	 *
	 * @return a new datatype constraint for the provided {@code name}
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public static Shape datatype(final IRI name) {
		return new Datatype(name);
	}

	public static Optional<IRI> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new TypeProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI id;


	private Datatype(final IRI id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		this.id=id;
	}


	public IRI id() {
		return id;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Datatype
				&& id.equals(((Datatype)object).id);
	}

	@Override public int hashCode() {
		return id.hashCode();
	}

	@Override public String toString() {
		return "datatype("+id+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TypeProbe extends Probe<IRI> {

		@Override public IRI probe(final Datatype datatype) {
			return datatype.id();
		}

		@Override public IRI probe(final And and) {
			return type(and.shapes());
		}

		@Override public IRI probe(final Or or) {
			return type(or.shapes());
		}


		private IRI type(final Collection<Shape> shapes) {

			final Set<IRI> names=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

}
