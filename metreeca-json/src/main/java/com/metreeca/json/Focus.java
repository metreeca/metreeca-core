/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json;

/**
 * Shape focus.
 *
 * <p>Provides a placeholder for a shape value dynamically derived from a target IRI while performing a shape-driven
 * operation, for instance serving a linked data resource.</p>
 */
@FunctionalInterface public interface Focus {

	/**
	 * Resolves this focus value.
	 *
	 * @param iri the target IRI for a shape-driven operation
	 *
	 * @return the IRI obtained by resolving this focus value against {@code iri}
	 *
	 * @throws NullPointerException     if {@code iri} is {@code null}
	 * @throws IllegalArgumentException if {@code iri} is malformed
	 */
	public String resolve(final String iri);


	/**
	 * Chains a focus value.
	 *
	 * @param focus the focus value to be chained to this focus value
	 *
	 * @return a combined focus value sequentially resolving target IRIs against {@code focus} and this focus
	 * value,
	 * in order
	 *
	 * @throws NullPointerException if {@code focus} is null
	 */
	public default Focus then(final Focus focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return iri -> focus.resolve(resolve(iri));
	}

}
