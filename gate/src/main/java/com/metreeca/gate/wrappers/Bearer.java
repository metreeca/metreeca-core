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

package com.metreeca.gate.wrappers;

import com.metreeca.gate.Roster;
import com.metreeca.gate.Roster.Permit;
import com.metreeca.rest.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.tray.Tray.tool;

import static java.lang.System.currentTimeMillis;


/**
 * Bearer token authenticator.
 *
 * <p>Manages bearer token authentication using tokens issued by the shared {@link Roster#Factory roster} tool.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>
 */
public final class Bearer implements Wrapper {

	private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Roster roster=tool(Roster.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> {

			// !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

			final String authorization=request.header("Authorization").orElse("");
			final Matcher matcher=BearerPattern.matcher(authorization);

			if ( matcher.matches() ) {

				final Permit permit=roster.profile(matcher.group("token"));

				if ( permit.valid(currentTimeMillis()) ) {

					return handler

							.handle(request
									.user(permit.user())
									.roles(permit.roles()))

							.map(response -> authenticate(request, response));

				} else {

					return request.reply(response -> response.status(Response.Unauthorized)
							.header("WWW-Authenticate", authenticate(request, "invalid_token")));

				}

			} else { // no bearer token > fall-through to other authorization schemes

				return handler

						.handle(request)

						.map(response -> authenticate(request, response));

			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Response authenticate(final Request request, final Response response) {

		// add authorization challenge, unless already provided by nested authorization schemes

		return response.status() == Response.Unauthorized && response.headers("WWW-Authenticate").isEmpty() ?
				response.header("WWW-Authenticate", authenticate(request)) : response;
	}


	private String authenticate(final Request request) {
		return "Bearer realm=\""+request.base()+"\"";
	}

	private String authenticate(final Request request, final String error) {
		return "Bearer realm=\""+request.base()+"\", error=\""+error+"\"";
	}

}
