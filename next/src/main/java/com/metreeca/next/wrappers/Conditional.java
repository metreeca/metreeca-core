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

package com.metreeca.next.wrappers;

import com.metreeca.next.Handler;
import com.metreeca.next.Request;
import com.metreeca.next.Wrapper;

import java.util.function.Predicate;


/**
 * Conditional wrapper.
 *
 * <p>Conditionally routes incoming {@linkplain Request requests} through a delegate {@linkplain Wrapper wrapper}
 * according to the outcome of a request {@linkplain #test(Predicate)} predicate}.</p>
 */
public final class Conditional implements Wrapper {

	private Predicate<Request> predicate=request -> true;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the request predicate for this conditional wrapper.
	 *
	 * <p>Incoming {@linkplain Request requests} are routed through the {@linkplain #wrap(Wrapper) delegate} wrapper if
	 * {@code predicate} evaluates to {@code true} on them; otherwise, they are directly forwarded to the {@linkplain
	 * #wrap(Handler) wrapped} handler.</p>
	 *
	 * @param predicate the request predicate for this conditional wrapper
	 *
	 * @return this conditional wrapper
	 *
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public Conditional test(final Predicate<Request> predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null request predicate");
		}

		this.predicate=predicate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return handler;
	}

	@Override public Wrapper wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return handler -> {

			final Handler wrapped=wrapper.wrap(handler);

			return request -> (predicate.test(request) ? wrapped : handler).handle(request);

		};
	}

}
