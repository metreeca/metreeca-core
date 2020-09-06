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

import java.text.Normalizer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Lexical full-text constraint.
 *
 * <p>States that the lexical representation of each term in the focus set matches the given full-text keywords.</p>
 *
 * <p>Sequences of word characters are matched case-insensitively either as whole words or word stems in the order they
 * appear within keywords; non-word characters are ignored.</p>
 */
public final class Like implements Shape {

	private static final Pattern WordPattern=Pattern.compile("\\w+");
	private static final Pattern MarkPattern=Pattern.compile("\\p{M}");


	public static Like like(final String keywords, final boolean stemming) {
		return new Like(keywords, stemming);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String text;

	private final boolean stemming;


	private Like(final String text, final boolean stemming) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text;
		this.stemming=stemming;
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

		final StringBuilder builder=new StringBuilder(text.length()).append("(?i:.*");

		for (final Matcher matcher=WordPattern.matcher(normalize(text)); matcher.find(); ) {
			builder.append("\\b").append(matcher.group()).append(stemming ? "" : "\\b").append(".*");
		}

		return builder.append(")").toString();
	}

	public Predicate<String> toMatcher() {

		final Pattern pattern=Pattern.compile(toExpression());

		return string -> pattern.matcher(normalize(string)).matches();
	}


	private String normalize(final CharSequence string) {
		return MarkPattern.matcher(Normalizer.normalize(string, Normalizer.Form.NFD)).replaceAll("");
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
		return "like("+(stemming ? "stemming" : "")+text+")";
	}

}
