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

package com.metreeca.rest._work;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

public final class _RDFCasts {
	/**
	 * Converts an object to an IRI.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return an IRI obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to an IRI
	 */
	public static IRI _iri(final Object object) {
		return as(object, IRI.class);
	}

	/**
	 * Converts an object to a value.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return a value obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to a value
	 */
	public static Value _value(final Object object) {
		return as(object, Value.class);
	}

	private static <T> T as(final Object object, final Class<T> type) {
		if ( object == null || type.isInstance(object) ) {

			return type.cast(object);

		} else {

			throw new UnsupportedOperationException(String.format("unsupported type {%s} / expected %s",
					object.getClass().getName(), type.getName()
			));

		}
	}
}
