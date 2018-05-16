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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.metreeca.spec.things.Strings.indent;
import static com.metreeca.spec.things.Values.format;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Parametric logical constraint.
 *
 * <p>States that the focus set meets this shape only if at least one of the values of an externally defined variable is
 * included in a given set of target values.</p>
 *
 * @see com.metreeca.spec.probes.Redactor
 */
public final class When implements Shape {

	public static When when(final IRI variable, final Value... values) {
		return when(variable, asList(values));
	}

	public static When when(final IRI variable, final Collection<? extends Value> values) {
		return new When(variable, values);
	}


	private final IRI iri;
	private final Set<Value> values;


	public When(final IRI iri, final Collection<? extends Value> values) {

		if ( iri == null ) {
			throw new NullPointerException("null variable IRI");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.isEmpty() ) {
			throw new IllegalArgumentException("empty values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.iri=iri;
		this.values=new HashSet<>(values);
	}


	public IRI getIRI() {
		return iri;
	}

	public Set<Value> getValues() {
		return unmodifiableSet(values);
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof When
				&& iri.equals(((When)object).iri)
				&& values.equals(((When)object).values);
	}

	@Override public int hashCode() {
		return iri.hashCode()^values.hashCode();
	}

	@Override public String toString() {
		return "when("+format(iri)+",\n"
				+values.stream().map(v -> indent(format(v))).collect(joining(",\n"))+"\n)";
	}

}
