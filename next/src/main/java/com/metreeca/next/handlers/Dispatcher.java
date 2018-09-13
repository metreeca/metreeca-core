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

package com.metreeca.next.handlers;


import com.metreeca.next.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


/**
 * Method-based request dispatcher.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP {@linkplain Request#method()
 * method}.</p>
 */
public final class Dispatcher implements Handler {

	private final Map<String, Handler> mappings=new LinkedHashMap<>();


	public Dispatcher() {
		mappings.put(Request.OPTIONS, this::options);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the handler for the GET HTTP method.
	 *
	 * @param handler the handler to be delegated for HTTP GET method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is {@code null}
	 */
	public Dispatcher get(final Handler handler) {
		return method(Request.GET, handler);
	}

	/**
	 * Configures the handler for the POST HTTP method.
	 *
	 * @param handler the handler to be delegated for HTTP POST method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is {@code null}
	 */
	public Dispatcher post(final Handler handler) {
		return method(Request.POST, handler);
	}

	/**
	 * Configures the handler for the PUT HTTP method.
	 *
	 * @param handler the handler to be delegated for HTTP PUT method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is {@code null}
	 */
	public Dispatcher put(final Handler handler) {
		return method(Request.PUT, handler);
	}

	/**
	 * Configures the handler for the DELETE HTTP method.
	 *
	 * @param handler the handler to be delegated for HTTP DELETE method
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if {@code handler} is {@code null}
	 */
	public Dispatcher delete(final Handler handler) {
		return method(Request.DELETE, handler);
	}


	/**
	 * Configures the handler for a HTTP method.
	 *
	 * @param method  the HTTP method whose handler is to be configured
	 * @param handler the handler to be delegated for {@code method}
	 *
	 * @return this dispatcher
	 *
	 * @throws NullPointerException if either {@code method} or {@code handler} is {@code null}
	 */
	public Dispatcher method(final String method, final Handler handler) {

		if ( method == null ) {
			throw new NullPointerException("null method");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		mappings.put(method, handler);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Lazy<Response> handle(final Request request) {
		return Optional.ofNullable(mappings.get(request.method()))
				.orElse(this::unsupported)
				.handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Response options(final Request request) {
		return request.response()
				.status(Response.OK)
				.headers("Allow", mappings.keySet());
	}

	private Response unsupported(final Request request) {
		return request.response()
				.status(Response.MethodNotAllowed)
				.headers("Allow", mappings.keySet());
	}

}
