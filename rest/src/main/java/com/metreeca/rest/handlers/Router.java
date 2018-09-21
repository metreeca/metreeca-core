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

package com.metreeca.rest.handlers;


import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;


/**
 * Path-based request router.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP {@linkplain Request#path()
 * path}.</p>
 *
 * <p>If the index doesn't contain a matching handler, no action is performed giving the system adapter a fall-back
 * opportunity to handle the request.</p>
 */

/*
 * Linked data handlers index {thread-safe}.
 *
 * <p>Maps linked data resource path patterns to delegated resource {@linkplain Handler handlers}.</p>
 *
 * <p>Linked data {@linkplain Server servers} delegate HTTP requests to handlers selected according to the following
 * rules on the basis of the server-relative {@linkplain Request#path() path} of the requested resource:</p>
 *
 * <ul>
 *
 * <li>paths with a trailing wildcard (e.g. {@code /container/*}) match any resource path sharing the same prefix
 * (e.g {@code /container}, {@code /container/resource});</li>
 *
 * <li>paths with no trailing wildcard (e.g. {@code /resource}) match resource path exactly (e.g {@code
 * /resource});</li>
 *
 * <li>lexicographically longer and preceding paths take precedence over shorter and following ones.</li>
 *
 * </ul>
 *
 * <p>Trailing slashes and question marks in resource paths are ignored.</p>
 */
public final class Router implements Handler {

	private final Map<String, Handler> handlers=new TreeMap<>(Comparator
			.comparingInt(String::length).reversed() // longest paths first
			.thenComparing(String::compareTo) // then alphabetically
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a handler to this router.
	 *
	 * @param path    the path pattern the handler to be added will be bound to; the value is normalized before use
	 * @param handler the handler to be added to this router at {@code path}
	 *
	 * @return this router
	 *
	 * @throws NullPointerException     if either {@code path} or {@code handler} is {@code null}
	 * @throws IllegalArgumentException if {@code path} doesn't include a leading slash
	 * @throws IllegalStateException    if {@code path} is already bound to a path
	 */
	public Router path(final String path, final Handler handler) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("missing leading / in path {"+path+"}");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		if ( handlers.putIfAbsent(normalize(path), handler) != null ) {
			throw new IllegalStateException("path is already mapped {"+path+"}");
		}

		return this;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		final String path=request.path();


		final String key=normalize(path);

		return handlers
				.entrySet()
				.stream()
				.filter(entry -> matches(key, entry.getKey()))
				.findFirst()
				.map(entry -> entry.getValue().handle(request.map(rewrite(entry.getKey()))))
				.orElseGet(() -> request.reply(response -> response));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean matches(final String x, final String y) {
		return x.equals(y) || y.endsWith("/") && x.startsWith(y);
	}

	private String normalize(final String path) {
		return path.endsWith("?") || path.endsWith("/") || path.endsWith("/*") ? path.substring(0, path.length()-1) : path;
	}

	private Function<Request, Request> rewrite(final String path) {
		return request -> path.endsWith("/")
				? request.base(request.base()+path.substring(1)).path(request.path().substring(path.length()-1))
				: request;
	}

}
