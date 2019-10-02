/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.tree.shapes;

import com.metreeca.tree.Shape;


/**
 * Lexical pattern constraint.
 *
 * <p>States that the lexical representation of each term in the focus set matches a given {@linkplain
 * java.util.regex.Pattern regular expression}.</p>
 */
public final class Pattern implements Shape {

	public static Pattern pattern(final String pattern) {
		return pattern(pattern, "");
	}

	public static Pattern pattern(final String pattern, final String flags) {
		return new Pattern(pattern, flags);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;
	private final String flags;


	private Pattern(final String text, final String flags) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		if ( text.isEmpty() ) {
			throw new IllegalArgumentException("empty text");
		}

		if ( flags == null ) {
			throw new NullPointerException("null flags");
		}

		// !!! test expression/flags syntax

		this.text=text;
		this.flags=flags;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getText() {
		return text;
	}

	public String getFlags() {
		return flags;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Pattern
				&& text.equals(((Pattern)object).text)
				&& flags.equals(((Pattern)object).flags);
	}

	@Override public int hashCode() {
		return text.hashCode()^flags.hashCode();
	}

	@Override public String toString() {
		return "pattern("+text+(flags.isEmpty() ? "" : ", "+flags)+")";
	}

}
