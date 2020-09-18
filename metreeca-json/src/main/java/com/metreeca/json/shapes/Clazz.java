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


/**
 * Class value constraint.
 *
 * <p>States that each value in the focus set is a member of a given resource class or one of its superclasses.</p>
 */
public final class Clazz extends Shape {

	public static Shape clazz(final IRI name) {
		return new Clazz(name);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI id;


	private Clazz(final IRI id) {

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
		return this == object || object instanceof Clazz
				&& id.equals(((Clazz)object).id);
	}

	@Override public int hashCode() {
		return id.hashCode();
	}

	@Override public String toString() {
		return "clazz("+id+")";
	}

}
