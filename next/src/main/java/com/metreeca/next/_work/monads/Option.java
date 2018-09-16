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
import java.util.function.Supplier;


public abstract class Option<V> {

	public static <V> Option<V> option(final V value) {
		return value == null ? none() : some(value);
	}


	@SuppressWarnings("unchecked")
	public static <V> Option<V> none() {
		return (None<V>)None.Instance;
	}

	public static <V> Option<V> some(final V value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Some<>(value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Option() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public abstract <R> Option<R> map(final Function<V, R> mapper);

	public abstract Option<V> accept(final Consumer<V> consumer);

	public abstract <R> R apply(final Function<V, R> value, final Supplier<R> empty);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class None<V> extends Option<V> {

		private static final None<?> Instance=new None<>();


		@Override public <R> Option<R> map(final Function<V, R> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return none();
		}

		@Override public Option<V> accept(final Consumer<V> consumer) {

			if ( consumer == null ) {
				throw new NullPointerException("null consumer");
			}

			return this;
		}

		@Override public <R> R apply(final Function<V, R> value, final Supplier<R> empty) {

			if ( value == null ) {
				throw new NullPointerException("null value producer");
			}

			if ( empty == null ) {
				throw new NullPointerException("null producer producer");
			}

			return empty.get();
		}

	}

	private static final class Some<V> extends Option<V> {

		private final V value;


		private Some(final V value) { this.value=value; }


		@Override public <R> Option<R> map(final Function<V, R> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return some(mapper.apply(value));
		}

		@Override public Option<V> accept(final Consumer<V> consumer) {

			if ( consumer == null ) {
				throw new NullPointerException("null consumer");
			}

			consumer.accept(value);

			return this;
		}

		@Override public <R> R apply(final Function<V, R> value, final Supplier<R> empty) {

			if ( value == null ) {
				throw new NullPointerException("null value producer");
			}

			if ( empty == null ) {
				throw new NullPointerException("null producer producer");
			}

			return value.apply(this.value);
		}
	}

}
