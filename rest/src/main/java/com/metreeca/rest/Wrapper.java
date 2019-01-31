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

package com.metreeca.rest;


import java.util.function.Predicate;


/**
 * Handler wrapper {thread-safe}.
 *
 * <p>Inspects and possibly alters {@linkplain Request requests} and {@linkplain Response responses} processed and
 * generated by resource {@linkplain Handler handlers}.</p>
 *
 * <p>Implementations must be thread-safe.</p>
 */
@FunctionalInterface public interface Wrapper {

	/**
	 * Creates a dummy wrapper.
	 *
	 * @return a dummy wrapper that performs no action on requests and responses
	 */
	public static Wrapper wrapper() {
		return handler -> handler;
	}

	/**
	 * Creates a conditional wrapper.
	 *
	 * @param test the request predicate used to decide if requests and responses are to be routed through the wrapper
	 * @param pass the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
	 *             true} on the request
	 *
	 * @return a conditional handler that routes requests and responses through the {@code pass} handler if the {@code
	 * test} predicate evaluates to {@code true} on the request or to a {@linkplain #wrapper() dummy wrapper} otherwise
	 *
	 * @throws NullPointerException if either {@code test} or {@code pass} is null
	 */
	public static Wrapper wrapper(final Predicate<Request> test, final Wrapper pass) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass wrapper");
		}

		return wrapper(test, pass, wrapper());
	}

	/**
	 * Creates a conditional wrapper.
	 *
	 * @param test the request predicate used to select the wrapper requests and responses are to be routed through
	 * @param pass the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
	 *             true} on the request
	 * @param fail the wrapper requests and responses are to be routed through when {@code test} evaluates to {@code
	 *             false} on the request
	 *
	 * @return a conditional wrapper that routes requests and responses either through the {@code pass} or the {@code
	 * fail} wrapper according to the results of the {@code test} predicate
	 *
	 * @throws NullPointerException if any of the arguments is null
	 */
	public static Wrapper wrapper(final Predicate<Request> test, final Wrapper pass, final Wrapper fail) {

		if ( test == null ) {
			throw new NullPointerException("null test predicate");
		}

		if ( pass == null ) {
			throw new NullPointerException("null pass wrapper");
		}

		if ( fail == null ) {
			throw new NullPointerException("null fail wrapper");
		}

		return handler -> Handler.handler(test, pass.wrap(handler), fail.wrap(handler));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Wraps a handler.
	 *
	 * @param handler the handler to be wrapped
	 *
	 * @return the combined handler obtained by wrapping this wrapper around {@code handler}
	 */
	public Handler wrap(final Handler handler);

	/**
	 * Wraps a wrapper.
	 *
	 * @param wrapper the handler to be wrapped
	 *
	 * @return the combined wrapper obtained by wrapping this wrapper around {@code wrapper}
	 *
	 * @throws NullPointerException if {@code wrapper} is null
	 */
	public default Wrapper wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		return handler -> wrap(wrapper.wrap(handler));
	}

}
