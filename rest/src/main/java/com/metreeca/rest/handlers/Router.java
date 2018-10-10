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

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;


/**
 * Path-based request router.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP {@linkplain Request#path()
 * path} according to the following rules, in order of precedence:</p>
 *
 * <ul>
 *
 * <li>paths with no trailing slash or wildcard ({@code /<resource>}) match resource path exactly, ignoring trailing
 * slashes ({@code /<resource>}, {@code /<resource>/});</li>
 *
 * <li>paths with a trailing slash ({@code /<resource>/}) match any resource path sharing the same prefix, ignoring
 * trailing slashes ({@code /<resource>}, {@code /<resource>/}, {@code /<resource>/<item>});</li>
 *
 * <li>paths with a trailing wildcard ({@code /<resource>/*}) match any resource path sharing the same prefix
 * ({@code /<resource>/<item>});</li>
 *
 * </ul>
 *
 * <p>Lexicographically longer and preceding paths take precedence over shorter and following ones.</p>
 *
 * <p>If the index doesn't contain a matching handler, no action is performed giving the system adapter a fall-back
 * opportunity to handle the request.</p>
 */
public final class Router implements Handler {

	private static final Pattern PathPattern=Pattern.compile("^(/[-+_\\w]+)*(/\\*?)?$");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Function<Request, Optional<Responder>>> routes=new TreeMap<>(Comparator
			.comparingInt(String::length).reversed() // longest paths first
			.thenComparing(String::compareTo) // then alphabetically
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Adds a handler to this router.
	 *
	 * @param path    the path pattern the handler to be added will be bound to
	 * @param handler the handler to be added to this router at {@code path}
	 *
	 * @return this router
	 *
	 * @throws NullPointerException     if either {@code path} or {@code handler} is null
	 * @throws IllegalArgumentException if {@code path} doesn't match the {@code ^(/[-+_\w]+)*(/\*?)?$} regular
	 *                                  expression
	 * @throws IllegalStateException    if {@code path} is already bound to a handler
	 */
	public Router path(final String path, final Handler handler) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !PathPattern.matcher(path).matches() ) {
			throw new IllegalArgumentException("malformed path <"+path+">");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		final String sorter=path.endsWith("/") ? path+'1'
				: path.endsWith("/*") ? path.replace('*', '2')
				: path+"/0";

		if ( routes.putIfAbsent(sorter, route(path, handler)) != null ) {
			throw new IllegalStateException("path already mapped <"+path+">");
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		return routes.values().stream()
				.map(route -> route.apply(request))
				.filter(Optional::isPresent)
				.findFirst()
				.map(Optional::get)
				.orElseGet(() -> request.reply(response -> response)); // null response >> managed by the container
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Function<Request, Optional<Responder>> route(final String path, final Handler handler) {
		return path.equals("/") ? root(path, handler)
				: path.endsWith("/") ? prefix(path, handler)
				: path.endsWith("/*") ? subtree(path, handler)
				: exact(path, handler);
	}


	private Function<Request, Optional<Responder>> root(final String path, final Handler handler) {
		return request -> request.path().equals(path) ? Optional.of(consumer -> handler.handle(

				request

		).accept(consumer)) : Optional.empty();
	}

	private Function<Request, Optional<Responder>> prefix(final String path, final Handler handler) {

		final Pattern pattern=Pattern.compile(Pattern.quote(path.substring(0, path.length()-1))+"(/.*)?");

		final String base=path.substring(1);
		final int head=path.length()-1;

		return request -> pattern.matcher(request.path()).matches() ? Optional.of(consumer -> handler.handle(

				request.base(request.base()+base).path(request.path().length() == head ? "/" : request.path().substring(head))

		).accept(consumer)) : Optional.empty();
	}

	private Function<Request, Optional<Responder>> subtree(final String path, final Handler handler) {

		final Pattern pattern=Pattern.compile(Pattern.quote(path.substring(0, path.length()-1))+".+");

		final String base=path.substring(1, path.length()-1);
		final int head=path.length()-2;

		return request -> pattern.matcher(request.path()).matches() ? Optional.of(consumer -> handler.handle(

				request.base(request.base()+base).path(request.path().substring(head))

		).accept(consumer)) : Optional.empty();
	}

	private Function<Request, Optional<Responder>> exact(final String path, final Handler handler) {

		final Pattern pattern=Pattern.compile(Pattern.quote(path)+"/?");

		final String base=path.substring(1)+"/";

		return request -> pattern.matcher(request.path()).matches() ? Optional.of(consumer -> handler.handle(

				request.base(request.base()+base).path("/")

		).accept(consumer)) : Optional.empty();
	}

}
