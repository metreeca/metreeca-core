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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;


/**
 * Memo utilities.
 */
public final class Memo {

	/**
	 * Creates a memoizing function.
	 *
	 * @param function the function whose results are to be memoized
	 * @param <V>    the type of the input value for {@code function}
	 * @param <R>    the type of the return value for {@code function}
	 *
	 * @return a function memoizing results computed by {@code function}
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V, R> Function<V, R> _memoize(final Function<V, R> function) {

		if ( function == null ) {
			throw new NullPointerException("null mapper");
		}

		return new Function<V, R>() {

			private final Map<V, R> memos=new ConcurrentHashMap<>();

			@Override public R apply(final V value) {
				return memos.computeIfAbsent(value, function);
			}

		};
	}


	public static Shape memo(final Shape shape) {
		return shape;
	}

	/**
	 * Creates a memoizing function.
	 *
	 * @param function the function whose results are to be memoized
	 * @param <V>    the type of the input value for {@code function}
	 * @param <R>    the type of the return value for {@code function}
	 *
	 * @return a function memoizing results computed by {@code function}
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V, R> Function<V, R> memoizing(final Function<V, R> function) {

		if ( function == null ) {
			throw new NullPointerException("null mapper");
		}

		return function;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Memo() {} // utility

}
