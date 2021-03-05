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
 * Minimum set size constraint.
 *
 * <p>States that the size of the focus set is greater than or equal to the given minimum value.</p>
 */
public final class MinCount extends Shape {

	public static Shape minCount(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		return new MinCount(limit);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int limit;


	private MinCount(final int limit) {
		this.limit=limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int limit() {
		return limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof MinCount
				&& limit == ((MinCount)object).limit;
	}

	@Override public int hashCode() {
		return Integer.hashCode(limit);
	}

	@Override public String toString() {
		return "minCount("+limit+")";
	}

}
