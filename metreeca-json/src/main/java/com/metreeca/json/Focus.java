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

import org.eclipse.rdf4j.model.Value;

import java.net.URI;

import static com.metreeca.json.Values.iri;

/**
 * Shape focus value.
 *
 * <p>Provides a placeholder for a shape IRI value dynamically derived from a target IRI while performing a
 * shape-driven operation, for instance serving a linked data resource.</p>
 */
public final class Focus implements Value {

	private static final long serialVersionUID=7112279683949649474L;


	/**
	 * Creates a target focus value.
	 *
	 * @return a focus value resolving to the target IRI of a shape-driven operation
	 */
	public static Focus focus() {
		return new Focus("");
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

		return new Focus(relative);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String relative;


	private Focus(final String relative) {
		this.relative=relative;
	}


	private String resolve(final String path) {
		return URI.create(path).resolve(relative).toString();
	}

	private String convert(final String path) {
		return path.endsWith("/") ? path.substring(0, path.length()-1) : path;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Resolves this focus value.
	 *
	 * @param focus the target value for a shape-driven operation
	 *
	 * @return the value obtained by resolving this focus value against {@code focus}
	 *
	 * @throws NullPointerException     if {@code focus} is {@code null}
	 * @throws IllegalArgumentException if {@code focus} is malformed
	 */
	public Value resolve(final Value focus) {
		return !focus.isIRI() || relative.isEmpty() ? focus
				: relative.endsWith("/") ? iri(resolve(focus.stringValue()))
				: iri(convert(resolve(focus.stringValue())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String stringValue() {
		return relative;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Focus
				&& relative.equals(((Focus)object).relative);
	}

	@Override public int hashCode() {
		return relative.hashCode();
	}

	@Override public String toString() {
		return String.format("focus(%s)", relative);
	}

}
