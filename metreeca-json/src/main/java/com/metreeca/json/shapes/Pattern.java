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
 * Lexical pattern constraint.
 *
 * <p>States that the lexical representation of each term in the focus set matches a given {@linkplain
 * java.util.regex.Pattern regular expression}.</p>
 */
public final class Pattern extends Shape {

	public static Shape pattern(final String expression) {

		if ( expression == null ) {
			throw new NullPointerException("null expression");
		}

		return pattern(expression, "");
	}

	public static Shape pattern(final String expression, final String flags) {

		if ( expression == null ) {
			throw new NullPointerException("null expression");
		}

		if ( expression.isEmpty() ) {
			throw new IllegalArgumentException("empty expression");
		}

		if ( flags == null ) {
			throw new NullPointerException("null flags");
		}

		return new Pattern(expression, flags);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String expression;
	private final String flags;


	private Pattern(final String expression, final String flags) {
		this.expression=expression;
		this.flags=flags;
	}


	public String expression() {
		return expression;
	}

	public String flags() {
		return flags;
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
		return this == object || object instanceof Pattern
				&& expression.equals(((Pattern)object).expression)
				&& flags.equals(((Pattern)object).flags);
	}

	@Override public int hashCode() {
		return expression.hashCode()^flags.hashCode();
	}

	@Override public String toString() {
		return "pattern("+expression+(flags.isEmpty() ? "" : ", "+flags)+")";
	}

}
