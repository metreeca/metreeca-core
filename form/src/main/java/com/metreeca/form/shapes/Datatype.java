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

package com.metreeca.form.shapes;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Inspector;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;


/**
 * Datatype value constraint.
 *
 * <p>States that each term in the focus set has a given extended RDF datatype.</p>
 *
 * <p>For validation purposes IRI references and blank nodes are considered to be respectively of {@link
 * Form#IRIType} and {@link Form#BNodeType} datatype.</p>
 */
public final class Datatype implements Shape {

	/**
	 * Creates a datatype constraint.
	 *
	 * <p>Beyond literal datatypes, the following extend values are supported:</p>
	 *
	 * <ul>
	 *     <li>{@link Form#BNodeType} for blank nodes;</li>
	 *     <li>{@link Form#IRIType} for IRI references;</li>
	 *     <li>{@link Form#ResourceType} for blank nodes or IRI references;</li>
	 *     <li>{@link Form#LiteralType} or {@link RDFS#LITERAL} for any literal.</li>
	 * </ul>
	 *
	 * @param iri the expected extended datatype
	 *
	 * @return a new datatype constraint for the provided {@code iri}
	 *
	 * @throws NullPointerException if {@code iri} is null
	 */
	public static Datatype datatype(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return new Datatype(iri);
	}

	public static Optional<IRI> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new DatatypeProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;


	private Datatype(final IRI iri) {
		this.iri=iri;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getIRI() {
		return iri;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Datatype
				&& iri.equals(((Datatype)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return "datatype("+Values.format(iri)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class DatatypeProbe extends Inspector<IRI> {

		@Override public IRI probe(final Datatype datatype) {
			return datatype.getIRI();
		}

		@Override public IRI probe(final And and) {

			final Set<IRI> iris=and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return iris.size() == 1 ? iris.iterator().next() : null;
		}

	}

}
