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
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.accept(new MaxCountProbe()));
	}


	private final int limit;


	public MaxCount(final int limit) {

		if ( limit < 1 ) {
			throw new IllegalArgumentException("illegal limit ["+limit+"]");
		}

		this.limit=limit;
	}


	public int getLimit() {
		return limit;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
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


	private static final class MaxCountProbe extends Probe<Integer> {

		// ;(jdk) replacing compareTo() with Math.min/max() causes a NullPointerException during Integer unboxing

		private static final BinaryOperator<Integer> min=(x, y) -> x == null ? y : y == null ? x : x.compareTo(y) <= 0 ? x : y;
		private static final BinaryOperator<Integer> max=(x, y) -> x == null ? y : y == null ? x : x.compareTo(y) >= 0 ? x : y;


		@Override public Integer visit(final MaxCount maxCount) {
			return maxCount.getLimit();
		}

		@Override public Integer visit(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.accept(this))
					.reduce(null, min);
		}

		@Override public Integer visit(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.accept(this))
					.reduce(null, max);
		}

		@Override public Integer visit(final Option option) {
			return max.apply(
					option.getPass().accept(this),
					option.getFail().accept(this));
		}

	}

}
