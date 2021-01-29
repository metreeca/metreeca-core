/*
 * Copyright © 2013-2021 Metreeca srl
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


/**
 * Lexical pattern constraint.
 *
 * <p>States that the lexical representation of each value in the focus set matches a given {@linkplain
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
