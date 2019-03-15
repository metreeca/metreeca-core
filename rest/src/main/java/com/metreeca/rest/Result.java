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

package com.metreeca.rest;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/**
 * Operation result.
 *
 * <p>Describes the result of an operation that could either successfully return a value or report an error.</p>
 *
 * @param <V> the type of the value possibly returned by the operation
 * @param <E> the type of the error possibly reported by the operation
 */
public abstract class Result<V, E> {

	/**
	 * Creates an operation result with a returned value.
	 *
	 * @param value the value returned by the operation
	 * @param <V>   the type of the value returned by the operation
	 * @param <E>   the type of the error possibly reported by the operation
	 *
	 * @return a result providing access to the value returned by the operation
	 */
	public static <V, E> Result<V, E> Value(final V value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Result<V, E>() {

			@Override public <R> R fold(final Function<V, R> success, final Function<E, R> failure) {

				if ( success == null ) {
					throw new NullPointerException("null success mapper");
				}

				if ( failure == null ) {
					throw new NullPointerException("null failure mapper");
				}

				return requireNonNull(success.apply(value), "null success mapper return value");
			}

			@Override public String toString() {
				return String.format("Value(%s)", value);
			}

		};
	}

	/**
	 * Creates an operation result with a reported error.
	 *
	 * @param error the error reported by the operation
	 * @param <V>   the type of the value possibly returned by the operation
	 * @param <E>   the type of the error reported by the operation
	 *
	 * @return a result providing access to the error reported by the operation
	 */
	public static <V, E> Result<V, E> Error(final E error) {

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		return new Result<V, E>() {

			@Override public <R> R fold(final Function<V, R> success, final Function<E, R> failure) {

				if ( success == null ) {
					throw new NullPointerException("null success mapper");
				}

				if ( failure == null ) {
					throw new NullPointerException("null failure mapper");
				}

				return requireNonNull(failure.apply(error), "null failure mapper return value");
			}

			@Override public String toString() {
				return String.format("Error(%s)", error);
			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private Result() {} // ADT


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the operation returned value.
	 *
	 * @return an optional operation returned value, if one is present; an empty optional, otherwise
	 */
	public Optional<V> value() {
		return fold(Optional::of, f -> Optional.empty());
	}

	/**
	 * Retrieves the operation reported error.
	 *
	 * @return an optional operation reported error, if one is present; an empty optional, otherwise
	 */
	public Optional<E> error() {
		return fold(f -> Optional.empty(), Optional::of);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Maps the operation returned value.
	 *
	 * @param mapper the value mapping function
	 * @param <R>    the type of the mapped returned value
	 *
	 * @return a result providing access to the value generated by applying {@code mapper} to the returned value of this
	 * result, if one was present; a result providing access to the reported error of this result, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public <R> Result<R, E> value(final Function<V, R> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				value -> Value(requireNonNull(mapper.apply(value), "null mapper return value")),
				Result::Error
		);
	}

	/**
	 * Maps the operation reported error.
	 *
	 * @param mapper the error mapping function
	 * @param <R>    the type of the mapped reported error
	 *
	 * @return a result providing access to the error generated by applying {@code mapper} to the reported error of this
	 * result, if one was present; a result providing access to the returned value of this result, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public <R> Result<V, R> error(final Function<E, R> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				Result::Value,
				error -> Error(requireNonNull(mapper.apply(error), "null mapper return value"))
		);
	}


	/**
	 * Process the operation returned value.
	 *
	 * @param mapper the value processing function
	 * @param <R>    the type of the mapped returned value
	 *
	 * @return a result generated by applying {@code mapper} to the returned value of this result, if one was present; a
	 * result providing access to the reported error of this result, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public <R> Result<R, E> process(final Function<V, Result<R, E>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				value -> requireNonNull(mapper.apply(value), "null mapper return value"),
				Result::Error
		);
	}

	/**
	 * Recovers from the operation reported error.
	 *
	 * @param mapper the error recovering function
	 * @param <R>    the type of the mapped reported error
	 *
	 * @return a result generated by applying {@code mapper} to the reported error of this result, if one was present; a
	 * result providing access to the returned value of this result, otherwise
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public <R> Result<V, R> recover(final Function<E, Result<V, R>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return fold(
				Result::Value,
				error -> requireNonNull(mapper.apply(error), "null mapper return value")
		);
	}


	/**
	 * Handles operations results.
	 *
	 * @param success a consumer accepting the operation returned value
	 * @param failure a consumer accepting the operation reported error
	 *
	 * @return this result
	 *
	 * @throws NullPointerException if either {@code success} or {@code failure} is null
	 */
	public Result<V, E> use(final Consumer<V> success, final Consumer<E> failure) {

		if ( success == null ) {
			throw new NullPointerException("null success consumer");
		}

		if ( failure == null ) {
			throw new NullPointerException("null failure consumer");
		}

		return fold(
				v -> {
					success.accept(v);
					return this;
				},
				e -> {
					failure.accept(e);
					return this;
				}
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Folds operation results.
	 *
	 * @param success a function mapping from the returned value to the final operation outcome
	 * @param failure a function mapping from the reported error to the final operation outcome
	 * @param <R>     the type of the final operation outcome
	 *
	 * @return the final operation outcome, as generated by either {@code success} or {@code failure} according to the
	 * result state
	 *
	 * @throws NullPointerException if either {@code success} or {@code failure} is null or returns a null value
	 */
	public abstract <R> R fold(final Function<V, R> success, final Function<E, R> failure);

}
