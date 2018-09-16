/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next._work.monads;

import java.util.function.Consumer;
import java.util.function.Function;


public abstract class Result<V, E> {

	public static <V, E> Result<V, E> value(final V value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Value<>(value);
	}

	public static <V, E> Result<V, E> error(final E error) {

		if ( error == null ) {
			throw new NullPointerException("null error");
		}

		return new Error<V, E>(error);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Result() {}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract <MV, ME> Result<MV, ME> map(final Function<V, MV> value, final Function<E, ME> error);

	public abstract Result<V, E> accept(final Consumer<V> value, final Consumer<E> error);

	public abstract <R> R apply(final Function<V, R> value, final Function<E, R> error);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Value<V, E> extends Result<V, E> {

		private final V value;


		private Value(final V value) { this.value=value; }


		@Override public <MV, ME> Result<MV, ME> map(final Function<V, MV> value, final Function<E, ME> error) {

			if ( value == null ) {
				throw new NullPointerException("null value mapper");
			}

			if ( error == null ) {
				throw new NullPointerException("null error mapper");
			}

			return value(value.apply(this.value));
		}

		@Override public Result<V, E> accept(final Consumer<V> value, final Consumer<E> error) {

			if ( value == null ) {
				throw new NullPointerException("null value consumer");
			}

			if ( error == null ) {
				throw new NullPointerException("null error consumer");
			}

			value.accept(this.value);

			return this;
		}

		@Override public <R> R apply(final Function<V, R> value, final Function<E, R> error) {

			if ( value == null ) {
				throw new NullPointerException("null value producer");
			}

			if ( error == null ) {
				throw new NullPointerException("null error producer");
			}

			return value.apply(this.value);
		}

	}

	private static final class Error<V, E> extends Result<V, E> {

		private final E error;


		private Error(final E error) { this.error=error; }


		@Override public <MV, ME> Result<MV, ME> map(final Function<V, MV> value, final Function<E, ME> error) {

			if ( value == null ) {
				throw new NullPointerException("null value mapper");
			}

			if ( error == null ) {
				throw new NullPointerException("null error mapper");
			}

			return error(error.apply(this.error));
		}

		@Override public Result<V, E> accept(final Consumer<V> value, final Consumer<E> error) {

			if ( value == null ) {
				throw new NullPointerException("null value consumer");
			}

			if ( error == null ) {
				throw new NullPointerException("null error consumer");
			}

			error.accept(this.error);

			return this;
		}

		@Override public <R> R apply(final Function<V, R> value, final Function<E, R> error) {

			if ( value == null ) {
				throw new NullPointerException("null value producer");
			}

			if ( error == null ) {
				throw new NullPointerException("null error producer");
			}

			return error.apply(this.error);
		}

	}

}
