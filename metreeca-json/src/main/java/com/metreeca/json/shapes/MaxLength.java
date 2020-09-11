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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;


/**
 * Lexical maximum length constraint.
 *
 * <p>States that the length of the lexical representation of each term in the focus set is less than or equal to the
 * given maximum value.</p>
 */
public final class MaxLength implements Shape {

	public static MaxLength maxLength(final int limit) {
		return new MaxLength(limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int limit;


	private MaxLength(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int limit() {
		return limit;
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
		return this == object || object instanceof MaxLength
				&& limit == ((MaxLength)object).limit;
	}

	@Override public int hashCode() {
		return Integer.hashCode(limit);
	}

	@Override public String toString() {
		return "maxLength("+limit+")";
	}

}
