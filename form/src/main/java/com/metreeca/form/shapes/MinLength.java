/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;


/**
 * Lexical minimum length constraint.
 *
 * <p>States that the length of the lexical representation of each term in the focus set is greater than or equal to
 * the
 * given minimum value.</p>
 */
public final class MinLength implements Shape {

	public static MinLength minLength(final int limit) {
		return new MinLength(limit);
	}


	private final int limit;


	public MinLength(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;
	}


	public int getLimit() {
		return limit;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof MinLength
				&& limit == ((MinLength)object).limit;
	}

	@Override public int hashCode() {
		return Integer.hashCode(limit);
	}

	@Override public String toString() {
		return "minLength("+limit+")";
	}

}
