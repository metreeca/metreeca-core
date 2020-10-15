/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
