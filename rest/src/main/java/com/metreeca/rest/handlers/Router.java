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

package com.metreeca.rest.handlers;


import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Path-based request router.
 *
 * <p>Delegates request processing to a handler selected on the basis of the request HTTP {@linkplain Request#path()
 * path}, ignoring the leading segment possibly already matched by wrapping routers.</p>
 *
 * <p>Requests are forwarded to a {@linkplain #path(String, Handler) registered} handler if their path is matched by an
 * associated pattern defined by a sequence of path steps according to the following rules:</p>
 *
 * <ul>
 *
 * <li>the empty step ({@code /}) matches only an empty step ({@code /});</li>
 *
 * <li>literal steps {@code /<step>} match path steps verbatim;</li>
 *
 * <li>child wildcard steps {@code /*} match a single path step;</li>
 *
 * <li>descendant wildcard steps {@code /**} match one or more path steps.</li>
 *
 * </ul>
 *
 * <p>Registered path patterns are tested in order of definition.</p>
 *
 * <p>If the index doesn't contain a matching handler, no action is performed giving the system adapter a fall-back
 * opportunity to handle the request.</p>
 */
public final class Router implements Handler {

	private static final Pattern PathPattern=Pattern.compile("(?<prefix>(/[^/*]*|/\\*\\*?)*?)(?<suffix>/\\*\\*?)?");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Function<Request, Optional<Responder>>> routes=new LinkedHashMap<>();


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
	 * @throws IllegalArgumentException if {@code path} is not a well-formed sequence of steps ( {@code
	 *                                  /<step> | /** | /*} )
	 * @throws IllegalStateException    if {@code path} is already bound to a handler
	 */
	public Router path(final String path, final Handler handler) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		final Matcher matcher=PathPattern.matcher(path);

		if ( !matcher.matches() ) {
			throw new IllegalArgumentException("malformed path <"+path+">");
		}

		final String prefix=matcher.group("prefix");
		final String suffix=matcher.group("suffix");

		final Function<Request, Optional<Responder>> route=route(prefix, suffix, handler);

		if ( routes.putIfAbsent(path, route) != null ) {
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

	private Function<Request, Optional<Responder>> route(final String prefix, final String suffix, final Handler handler) {

		final Pattern pattern=Pattern.compile(
				String.format("(?<prefix>%s)(?<suffix>%s)",
						Pattern.quote(prefix ==null? "" : prefix),
						Pattern.quote(suffix ==null? "" : suffix)
				)
				.replaceAll("/\\*\\*", "\\\\E/.*\\\\Q")
				.replaceAll("/\\*", "\\\\E/[^/]*\\\\Q")
		);

		return request -> {

			final String head=request.header("Location").orElse("");
			final String tail=request.path().substring(head.length());

			return Optional.of(pattern.matcher(tail))
					.filter(Matcher::matches)
					.map(matcher -> consumer -> handler
							.handle(request.header("Location", head+matcher.group("prefix")))
							.accept(consumer)
					);

		};
	}

}
