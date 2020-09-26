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

import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.internal;


/**
 * Class value constraint.
 *
 * <p>States that each value in the focus set is a member of a given resource class or one of its superclasses.</p>
 */
public final class Clazz extends Shape {

	/**
	 * Creates a class constraint.
	 *
	 * @param name the expected class name
	 *
	 * @return a new datatype constraint for the provided {@code name}
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public static Shape clazz(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return clazz(internal(name));
	}

	/**
	 * Creates a class constraint.
	 *
	 * @param iri the expected class IRI
	 *
	 * @return a new datatype constraint for the provided {@code iri}
	 *
	 * @throws NullPointerException if {@code iri} is null
	 */
	public static Shape clazz(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return new Clazz(iri);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;


	private Clazz(final IRI iri) {
		this.iri=iri;
	}


	public IRI iri() {
		return iri;
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
		return this == object || object instanceof Clazz
				&& iri.equals(((Clazz)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return "clazz("+format(iri)+")";
	}

}
