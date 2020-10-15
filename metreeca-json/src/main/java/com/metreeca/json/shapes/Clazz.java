/*
 * Copyright © 2013-2020 Metreeca srl
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
