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

package com.metreeca.form;

import org.eclipse.rdf4j.model.IRI;


/**
 * Focus-shifting operator.
 */
public final class Shift {

	public static Shift shift(final IRI iri) {
		return new Shift(iri, false);
	}


	private final IRI iri;

	private final boolean inverse;


	private Shift(final IRI iri, final boolean inverse) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		this.iri=iri;
		this.inverse=inverse;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getIRI() {
		return iri;
	}


	public boolean isInverse() {
		return inverse;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Shift inverse() { return inverse(!inverse); }

	public Shift inverse(final boolean inverse) {
		return inverse == this.inverse ? this  : new Shift(iri, inverse);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Shift
				&& iri.equals(((Shift)object).iri)
				&& inverse == ((Shift)object).inverse;
	}

	@Override public int hashCode() {
		return iri.hashCode()^Boolean.hashCode(inverse);
	}

	@Override public String toString() {
		return (inverse ? "^<" : "<")+iri.stringValue()+">";
	}

}
