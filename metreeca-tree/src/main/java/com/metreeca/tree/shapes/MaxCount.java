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
import com.metreeca.tree.probes.Traverser;

import java.util.Optional;
import java.util.function.BinaryOperator;


/**
 * Maximum set size constraint.
 *
 * <p>States that the size of the focus set is less than or equal to the given maximum value.</p>
 */
public final class MaxCount implements Shape {

	public static MaxCount maxCount(final int limit) {
		return new MaxCount(limit);
	}


	public static Optional<Integer> maxCount(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new MaxCountProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int limit;


	private MaxCount(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public int getLimit() {
		return limit;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


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

	private static final class MaxCountProbe extends Traverser<Integer> {

		// ;(jdk) replacing compareTo() with Math.min/max() causes a NullPointerException during Integer unboxing

		private static final BinaryOperator<Integer> min=(x, y) -> x == null ? y : y == null ? x : x.compareTo(y) <= 0 ? x : y;
		private static final BinaryOperator<Integer> max=(x, y) -> x == null ? y : y == null ? x : x.compareTo(y) >= 0 ? x : y;


		@Override public Integer probe(final MaxCount maxCount) {
			return maxCount.getLimit();
		}


		@Override public Integer probe(final Field field) { return null; }


		@Override public Integer probe(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, min);
		}

		@Override public Integer probe(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, max);
		}

		@Override public Integer probe(final When when) {
			return max.apply(
					when.getPass().map(this),
					when.getFail().map(this));
		}

	}

}
