/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.spec.shifts;

import com.metreeca.spec.Shift;

import org.eclipse.rdf4j.model.IRI;


public final class Step implements Shift {

	public static Step step(final IRI iri) {
		return new Step(iri, false);
	}

	public static Step step(final IRI iri, final boolean inverse) {
		return new Step(iri, inverse);
	}


	private final IRI iri;
	private final boolean inverse;


	public Step(final IRI iri, final boolean inverse) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		this.iri=iri;
		this.inverse=inverse;
	}


	public IRI getIRI() {
		return iri;
	}

	public boolean isInverse() {
		return inverse;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	public Step invert() {
		return new Step(iri, !inverse);
	}

	public String format() {
		return (inverse ? "^<" : "<")+iri.stringValue()+">";
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Step
				&& iri.equals(((Step)object).iri)
				&& inverse == ((Step)object).inverse;
	}

	@Override public int hashCode() {
		return iri.hashCode()^Boolean.hashCode(inverse);
	}

	@Override public String toString() {
		return format();
	}

}
