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
import java.text.Normalizer.Form;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Lexical full-text constraint.
 *
 * <p>States that the lexical representation of each value in the focus set matches the given full-text keywords.</p>
 *
 * <p>Sequences of word characters are matched case-insensitively either as whole words or word stems in the order they
 * appear within keywords; non-word characters are ignored.</p>
 */
public final class Like extends Shape {

	private static final Pattern WordPattern=Pattern.compile("\\w+");
	private static final Pattern MarkPattern=Pattern.compile("\\p{M}");


	public static String keywords(final CharSequence keywords, final boolean stemming) {

		if ( keywords == null ) {
			throw new NullPointerException("null keywords");
		}

		final StringBuilder builder=new StringBuilder(keywords.length()).append("(?i:.*");

		final String normal=MarkPattern.matcher(Normalizer.normalize(keywords, Form.NFD)).replaceAll("");

		for (final Matcher matcher=WordPattern.matcher(normal); matcher.find(); ) {
			builder.append("\\b").append(matcher.group()).append(stemming ? "" : "\\b").append(".*");
		}

		return builder.append(")").toString();
	}


	public static Shape like(final String keywords, final boolean stemming) {

		if ( keywords == null ) {
			throw new NullPointerException("null keywords");
		}

		return new Like(keywords, stemming);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String keywords;

	private final boolean stemming;


	private Like(final String keywords, final boolean stemming) {
		this.keywords=keywords;
		this.stemming=stemming;
	}


	public String keywords() {
		return keywords;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Converts this constraint to a regular expression.
	 *
	 * @return a regular expression matching strings matched by this like constraint
	 */
	public String toExpression() {
		return keywords(keywords, stemming);
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
		return this == object || object instanceof Like
				&& keywords.equals(((Like)object).keywords);
	}

	@Override public int hashCode() {
		return keywords.hashCode();
	}

	@Override public String toString() {
		return "like("+keywords+(stemming ? ", stemming" : "")+")";
	}

}
