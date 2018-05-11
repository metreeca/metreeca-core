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
import com.metreeca.spec.Values;

import org.eclipse.rdf4j.model.IRI;


/**
 * Hint annotation.
 *
 * <p>Provides the IRI of a resource hinting at possible values for the enclosing shape (e.g. an LDP container).</p>
 */
public final class Hint implements Shape {

	public static Hint hint(final IRI iri) {
		return new Hint(iri);
	}


	private final IRI iri;


	public Hint(final IRI iri) {

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
		return this == object || object instanceof Hint
				&& iri.equals(((Hint)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return "type("+Values.format(iri)+")";
	}

}
