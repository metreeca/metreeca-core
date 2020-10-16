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

package com.metreeca.rest;

import java.util.Optional;
import java.util.function.*;

import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;


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
	 * @param <L>   the type of the right alternative value
	 * @param <R>   the type of the left alternative value
	 *
	 * @return an alternative values pair wrapping the supplied left {@code value}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static <L, R> Either<L, R> Left(final L value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Either<L, R>() {

			@Override public <V> V fold(final Function<L, V> left, final Function<R, V> right) {

				if ( left == null ) {
					throw new NullPointerException("null failure mapper");
				}

				if ( right == null ) {
					throw new NullPointerException("null success mapper");
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
	 * @param <L>   the type of the right alternative value
	 * @param <R>   the type of the left alternative value
	 *
	 * @return an alternative values pair wrapping the supplied right {@code value}
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static <L, R> Either<L, R> Right(final R value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Either<L, R>() {

			@Override public <V> V fold(final Function<L, V> left, final Function<R, V> right) {

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


	/**
	 * Creates an alternative value pair.
	 *
	 * @param supplier the supplier of the right alternative value
	 * @param <V>      the type of the right alternative value
	 *
	 * @return a right alternative value, if a value was successfully provided by {@code supplier}; a left alternative
	 * value with an exception thrown in the process, otherwise
	 *
	 * @throws NullPointerException if {@code supplier} is null or returns a null value
	 */
	public static <V> Either<RuntimeException, V> from(final Supplier<V> supplier) {

		if ( supplier == null ) {
			throw new NullPointerException("null supplier");
		}

		try {

			return Right(requireNonNull(supplier.get(), "null supplier return value"));

		} catch ( final RuntimeException e ) {

			return Left(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

		return fold(Either::Left, value ->
				Right(requireNonNull(mapper.apply(value), "null mapper return value"))
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

		return fold(Either::Left, value ->
				requireNonNull(mapper.apply(value), "null mapper return value")
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
	 * Folds the left value.
	 *
	 * @param left a function mapping from the left alternative value to a value of the same type as the right value
	 *
	 * @return the folded value, generated as required either by {@code left} or the identity function
	 *
	 * @throws NullPointerException if {@code left} is null or returns a null value
	 */
	public default R fold(final Function<L, R> left) {

		if ( left == null ) {
			throw new NullPointerException("null left");
		}

		return fold(left, identity());
	}

	/**
	 * Folds alternative values.
	 *
	 * @param <V>   the type of the folded value
	 * @param left  a function mapping from the left alternative value to the folded value
	 * @param right a function mapping from the right alternative value to the folded value
	 *
	 * @return the folded value, generated as required either by {@code left} or {@code right}
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

}
