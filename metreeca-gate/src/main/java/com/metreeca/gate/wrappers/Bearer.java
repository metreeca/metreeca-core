/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.gate.wrappers;

import com.metreeca.rest.*;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;


/**
 * Bearer token authenticator.
 *
 * <p>Manages bearer token authentication protocol delegating token validation to an authentication service.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>
 */
public final class Bearer implements Wrapper {

	private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BiFunction<String, Request, Optional<Request>> authenticator;


	/**
	 * Creates a bearer token authenticator
	 *
	 * @param authenticator the delegated authentication service; takes as argument the bearer token presented with the
	 *                      request and the request itself; returns an optional configured request on successful token
	 *                      validation or an empty optional otherwise
	 *
	 * @throws NullPointerException if {@code authenticator} is null
	 */
	public Bearer(final BiFunction<String, Request, Optional<Request>> authenticator) {

		if ( authenticator == null ) {
			throw new NullPointerException("null authenticator");
		}

		this.authenticator=authenticator;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(challenger())
				.with(authenticator());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a wrapper adding authentication challenge to unauthorized responses, unless already provided by nested
	 * authorization schemes
	 */
	private Wrapper challenger() {
		return handler -> request -> handler.handle(request).map(response ->
				response.status() == Response.Unauthorized && response.headers("WWW-Authenticate").isEmpty()
						? response.header("WWW-Authenticate", format("Bearer realm=\"%s\"", request.base()))
						: response
		);
	}

	/**
	 * @return a wrapper managing token-based authentication
	 */
	private Wrapper authenticator() {
		return handler -> request -> {

			// !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

			final String authorization=request.header("Authorization").orElse("");

			return Optional

					.of(BearerPattern.matcher(authorization))
					.filter(Matcher::matches)
					.map(matcher -> matcher.group("token"))

					// bearer token > authenticate

					.map(token -> authenticator.apply(token, request)

							// authenticated > handle request

							.map(handler::handle)

							// not authenticated > report error

							.orElseGet(() -> request.reply(response -> response
									.status(Response.Unauthorized)
									.header("WWW-Authenticate", format(
											"Bearer realm=\"%s\", error=\"invalid_token\"", response.request().base()
									))
							))

					)

					// no bearer token > fall-through to other authorization schemes

					.orElseGet(() -> handler.handle(request));
		};
	}

}
