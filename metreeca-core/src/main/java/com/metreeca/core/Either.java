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

package com.metreeca.core;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/**
 * Alternative values.
 *
 * <p>Wraps a pair of mutually exclusive values.</p>
 *
 * @param <R> the type of the left alternative value
 * @param <L> the type of the right alternative value
 */
@FunctionalInterface public interface Either<L, R> {

	/**
	 * Creates a left alternative value.
	 *
	 * @param value the left value to be wrapped
	 * @param <R>   the type of the left alternative value
	 * @param <L>   the type of the right alternative value
	 *
	 * @return an alternative values pair wrapping the supplied left {@code value}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static <L, R> Either<R, L> Left(final R value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Either<R, L>() {

			@Override public <V> V fold(final Function<R, V> left, final Function<L, V> right) {

				if ( right == null ) {
					throw new NullPointerException("null success mapper");
				}

				if ( left == null ) {
					throw new NullPointerException("null failure mapper");
				}

				return requireNonNull(left.apply(value), "null left mapper return value");
			}

			@Override public String toString() {
				return String.format("Left(%s)", value);
			}

		};
	}

	/**
	 * Creates a right alternative value.
	 *
	 * @param value the right value to be wrapped
	 * @param <R>   the type of the left alternative value
	 * @param <L>   the type of the right alternative value
	 *
	 * @return an alternative values pair wrapping the supplied right {@code value}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static <L, R> Either<R, L> Right(final L value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Either<R, L>() {

			@Override public <V> V fold(final Function<R, V> left, final Function<L, V> right) {

				if ( right == null ) {
					throw new NullPointerException("null success mapper");
				}

				if ( left == null ) {
					throw new NullPointerException("null failure mapper");
				}

				return requireNonNull(right.apply(value), "null right mapper return value");
			}

			@Override public String toString() {
				return String.format("Right(%s)", value);
			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Folds alternative values.
	 *
	 * @param <V>   the type of the folded value
	 * @param left  a function mapping from the left alternative value to the folded value
	 * @param right a function mapping from the right alternative value to the folded value
	 *
	 * @return the folded value, generated as required either by {@code right} or {@code left}
	 *
	 * @throws NullPointerException if either {@code right} or {@code left} is null or returns a null value
	 */
	public <V> V fold(final Function<L, V> left, final Function<R, V> right);

	/**
	 * Consumes alternative values.
	 *
	 * @param left  a consumer for the left alternative value
	 * @param right a consumer for the right alternative value
	 *
	 * @throws NullPointerException if either {@code right} or {@code left} is null
	 */
	public default void accept(final Consumer<L> left, final Consumer<R> right) {

		if ( left == null ) {
			throw new NullPointerException("null left");
		}

		if ( right == null ) {
			throw new NullPointerException("null right");
		}

		fold(
				value -> {
					left.accept(value);
					return this;
				},
				value -> {
					right.accept(value);
					return this;
				}
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the right value.
	 *
	 * @return an optional containing the right value of this pair, if one is present, or an empty optional, otherwise
	 */
	public default Optional<R> get() {
		return fold(l -> Optional.empty(), Optional::of);
	}

	/**
	 * Maps the right value.
	 *
	 * @param mapper the right value mapping function
	 * @param <V>    the type of the mapped returned value
	 *
	 * @return an alternative values pair wrapping the left value of this pair, if one is present, or the right
	 * value generated by applying {@code mapper} to the right value of this pair, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public default <V> Either<L, V> map(final Function<R, V> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				Either::Left, value -> Right(requireNonNull(mapper.apply(value), "null mapper return value"))
		);
	}

	/**
	 * Maps the right value.
	 *
	 * @param mapper the right value mapping function
	 * @param <V>    the type of the mapped returned value
	 *
	 * @return an alternative values pair wrapping the left value of this pair, if one is presente, or the
	 * alternative values pair generated by applying {@code mapper} to the right value of this pair, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public default <V> Either<L, V> flatMap(final Function<R, Either<L, V>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				Either::Left, value -> requireNonNull(mapper.apply(value), "null mapper return value")
		);
	}

}
