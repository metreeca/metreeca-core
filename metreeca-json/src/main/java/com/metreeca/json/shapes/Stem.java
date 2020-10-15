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
