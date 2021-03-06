/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

	/**
	 * Creates a key-based bearer token authenticator.
	 *
	 * @param key   the fixed key to be presented as bearer token
	 * @param roles a collection of values uniquely identifying the roles to be {@linkplain Request#role(Object...)
	 *              assigned} to the request user on successful {@code key} validation
	 *
	 * @return a new key-based bearer token authenticator
	 *
	 * @throws NullPointerException     if {@code roles} is null or contains a {@code null} value
	 * @throws IllegalArgumentException if {@code key} is empty
	 */
	public static Bearer bearer(final String key, final Object... roles) {

		if ( key == null ) {
			throw new NullPointerException("null key");
		}

		if ( key.isEmpty() ) {
			throw new IllegalArgumentException("empty key");
		}

		if ( roles == null || Stream.of(roles).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		return new Bearer((token, request) -> token.equals(key)
				? Optional.of(request.roles(roles))
				: Optional.empty()
		);
	}

	/**
	 * Creates a bearer token authenticator.
	 *
	 * @param authenticator the delegated authentication service; takes as argument the bearer token presented with the
	 *                      request and the request itself; returns an optional configured request on successful token
	 *                      validation or an empty optional otherwise
	 *
	 * @return a new a bearer token authenticator
	 *
	 * @throws NullPointerException if {@code authenticator} is null
	 */
	public static Bearer bearer(final BiFunction<String, Request, Optional<Request>> authenticator) {

		if ( authenticator == null ) {
			throw new NullPointerException("null authenticator");
		}

		return new Bearer(authenticator);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BiFunction<? super String, Request, Optional<Request>> authenticator;


	private Bearer(final BiFunction<? super String, Request, Optional<Request>> authenticator) {
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
