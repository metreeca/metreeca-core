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

import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;


/**
 * Maximum set size constraint.
 *
 * <p>States that the size of the focus set is less than or equal to the given maximum value.</p>
 */
public final class MaxCount extends Shape {

	public static Shape maxCount(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit <"+limit+">");
		}

		return new MaxCount(limit);
	}


	public static int maxCount(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return Optional.ofNullable(shape.map(new MaxCountProbe())).orElse(Integer.MAX_VALUE);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int limit;


	private MaxCount(final int limit) {
		this.limit=limit;
	}


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
		return this == object || object instanceof MaxCount
				&& limit == ((MaxCount)object).limit;
	}

	@Override public int hashCode() {
		return Integer.hashCode(limit);
	}

	@Override public String toString() {
		return "maxCount("+limit+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MaxCountProbe extends Probe<Integer> {

		@Override public Integer probe(final MaxCount maxCount) {
			return maxCount.limit();
		}

		@Override public Integer probe(final And and) {
			return reduce(and.shapes().stream(), Math::min);
		}

		@Override public Integer probe(final Or or) {
			return reduce(or.shapes().stream(), Math::max);
		}

		@Override public Integer probe(final When when) {
			return reduce(Stream.of(when.pass(), when.fail()), Math::max);
		}


		private Integer reduce(final Stream<Shape> shapes, final BinaryOperator<Integer> operator) {
			return shapes
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.reduce(operator)
					.orElse(null);
		}

	}

}
