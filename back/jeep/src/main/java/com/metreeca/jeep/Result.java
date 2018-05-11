/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.jeep;

import java.util.concurrent.Callable;
import java.util.function.Consumer;


public abstract class Result<V, E> {

	public static <T> Result<T, Exception> result(final Callable<T> task) {
		try {
			return value(task == null ? null : task.call());
		} catch ( final Exception e ) {
			return error(e);
		}
	}

	public static <V, E> Result<V, E> value(final V value) {
		return new Result<V, E>() {

			@Override public Result<V, E> value(final Consumer<V> task) {

				if ( task != null ) { task.accept(value); }

				return this;
			}

			@Override public Result<V, E> error(final Consumer<E> task) {
				return this;
			}

		};
	}

	public static <V, E> Result<V, E> error(final E error) {
		return new Result<V, E>() {

			@Override public Result<V, E> value(final Consumer<V> task) {
				return this;
			}

			@Override public Result<V, E> error(final Consumer<E> task) {

				if ( task != null ) {
					task.accept(error);
				}

				return this;
			}

		};
	}


	private Result() {}


	public abstract Result<V, E> value(final Consumer<V> task);

	public abstract Result<V, E> error(final Consumer<E> task);

}
