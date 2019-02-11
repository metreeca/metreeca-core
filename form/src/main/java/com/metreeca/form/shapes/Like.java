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

import java.util.regex.Pattern;


/**
 * Lexical full-text constraint.
 *
 * <p>States that the lexical representation of each term in the focus set matches the given full-text keywords.</p>
 *
 * <p>sequences of word characters are matched case-insensitively as word stemsin the order the appear within keywords;
 * non-word characters are ignored.</p>
 */
public final class Like implements Shape {

	private static final Pattern WordsPattern =Pattern.compile("^|\\W+");


	public static Like like(final String keywords) {
		return new Like(keywords);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;


	private Like(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		if ( text.isEmpty() ) { // !!! test after normalization
			throw new IllegalArgumentException("empty pattern");
		}

		this.text=text;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getText() {
		return text;
	}


	/**
	 * Converts this constraint to a regular expression.
	 *
	 * @return a regular expression matching strings matched by this like constraint
	 */
	public String toExpression() {
		return "(?i:"+WordsPattern.matcher(text).replaceAll(".*\\\\b")+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Like
				&& text.equals(((Like)object).text);
	}

	@Override public int hashCode() {
		return text.hashCode();
	}

	@Override public String toString() {
		return "like("+text+")";
	}

}
