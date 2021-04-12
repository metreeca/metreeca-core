/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.json.Values.format;


/**
 * Datatype value constraint.
 *
 * <p>States that each value in the focus set has a given datatype.</p>
 */
public final class Datatype extends Shape {

	/**
	 * Creates a datatype constraint.
	 *
	 * @param iri the expected datatype IRI
	 *
	 * @return a new datatype constraint for the provided {@code iri}
	 *
	 * @throws NullPointerException if {@code iri} is null
	 */
	public static Shape datatype(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return new Datatype(iri);
	}


	public static boolean typed(final Shape shape, final IRI iri) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return datatype(shape).filter(iri::equals).isPresent();
	}

	public static Optional<IRI> datatype(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Optional.ofNullable(shape.map(new _Inspector<IRI>() {

			@Override public IRI probe(final Datatype datatype) { return datatype.iri(); }

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;


	private Datatype(final IRI iri) {
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
		return this == object || object instanceof Datatype
				&& iri.equals(((Datatype)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return "datatype("+format(iri)+")";
	}

}
