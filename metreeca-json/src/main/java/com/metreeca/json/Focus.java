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

package com.metreeca.json;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.util.function.UnaryOperator;

import static com.metreeca.json.Values.iri;

/**
 * Shape focus value.
 *
 * <p>Provides a placeholder for a shape IRI value dynamically derived from a target IRI while performing a
 * shape-driven operation, for instance serving a linked data resource.</p>
 */
public abstract class Focus implements Value {

	/**
	 * Creates a target focus value.
	 *
	 * @return a focus value resolving to the target IRI of a shape-driven operation
	 */
	public static Focus focus() {
		return new Focus() {

			@Override public String stringValue() { return ""; }

			@Override public IRI resolve(final IRI base) { return base; }

		};
	}

	/**
	 * Creates a relative focus value.
	 *
	 * @param relative the relative IRI of the focus value
	 *
	 * @return a focus value resolving {@code relative} against the target IRI of a shape-driven operation; trailing
	 * slashes in the resolved IRI are removed unless {@code relative} includes one
	 *
	 * @throws NullPointerException if {@code relative} is null
	 */
	public static Focus focus(final String relative) {

		if ( relative == null ) {
			throw new NullPointerException("null relative IRI");
		}

		final boolean slash=relative.endsWith("/");

		final UnaryOperator<String> resolve=path -> URI.create(path).resolve(relative).toString();
		final UnaryOperator<String> convert=path -> path.endsWith("/") ? path.substring(0, path.length()-1) : path;

		return new Focus() {

			@Override public String stringValue() { return relative; }

			@Override public IRI resolve(final IRI base) {
				return relative.isEmpty() ? base
						: slash ? iri(resolve.apply(base.stringValue()))
						: iri(convert.apply(resolve.apply(base.stringValue())));
			}

		};

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Focus() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Resolves this focus value.
	 *
	 * @param base the target IRI for a shape-driven operation
	 *
	 * @return the IRI obtained by resolving this focus value against {@code base}
	 *
	 * @throws NullPointerException     if {@code base} is {@code null}
	 * @throws IllegalArgumentException if {@code base} is malformed
	 */
	public abstract IRI resolve(final IRI base);

}
