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

package com.metreeca.spec.shapes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.IRI;


/**
 * Class value constraint.
 *
 * <p>States that each term in the focus set is an instance of a given class or one of its superclasses.</p>
 */
public final class Clazz implements Shape {

	public static Clazz clazz(final IRI iri) {
		return new Clazz(iri);
	}


	private final IRI iri;


	private Clazz(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		this.iri=iri;
	}


	public IRI getIRI() {
		return iri;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Clazz
				&& iri.equals(((Clazz)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return "clazz("+Values.format(iri)+")";
	}

}
