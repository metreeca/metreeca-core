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

package com.metreeca.json.shifts;

import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.json.Values.format;

/**
 * Predicate path.
 */
public final class Step extends Path {

	public static Path step(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return new Step(iri);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;


	private Step(final IRI iri) {
		this.iri=iri;
	}


	public IRI iri() {
		return iri;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {
		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Step
				&& iri.equals(((Step)object).iri);
	}

	@Override public int hashCode() {
		return iri.hashCode();
	}

	@Override public String toString() {
		return format(iri);
	}

}
