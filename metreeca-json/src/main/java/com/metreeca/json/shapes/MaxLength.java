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
 * Lexical maximum length constraint.
 *
 * <p>States that the length of the lexical representation of each value in the focus set is less than or equal to the
 * given maximum value.</p>
 */
public final class MaxLength extends Shape {

	public static Shape maxLength(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		return new MaxLength(limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int limit;


	private MaxLength(final int limit) {
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
