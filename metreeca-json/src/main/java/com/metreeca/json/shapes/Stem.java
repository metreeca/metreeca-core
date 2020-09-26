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
 * Lexical stem constraint.
 *
 * <p>States that the lexical representation of each value in the focus set starts with the given stem.</p>
 */
public final class Stem extends Shape {

	public static Shape stem(final String prefix) {

		if ( prefix == null ) {
			throw new NullPointerException("null prefix");
		}

		return new Stem(prefix);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String prefix;


	private Stem(final String prefix) {
		this.prefix=prefix;
	}


	public String prefix() {
		return prefix;
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
		return this == object || object instanceof Stem
				&& prefix.equals(((Stem)object).prefix);
	}

	@Override public int hashCode() {
		return prefix.hashCode();
	}

	@Override public String toString() {
		return "stem("+prefix+")";
	}

}
