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

package com.metreeca.link.wrappers;

import com.metreeca.link.*;
import com.metreeca.spec.Spec;
import com.metreeca.tray.iam.Roster;
import com.metreeca.tray.iam.Roster.Permit;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.tray.Tray.tool;

import static java.lang.System.currentTimeMillis;
import static java.util.Collections.singleton;


/**
 * Bearer token authenticator.
 *
 * <p>Manages bearer token authentication using tokens issued by the shared {@link Roster} tool.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc6750">The OAuth 2.0 Authorization Framework: Bearer Token Usage</a>
 */
public final class Bearer implements Wrapper {

	private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


	public static Bearer bearer() { return new Bearer(); }


	private final Roster roster=tool(Roster.Tool);


	private Bearer() {}


	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {

			// !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

			final String authorization=request.header("Authorization").orElse("");
			final Matcher matcher=BearerPattern.matcher(authorization);

			if ( matcher.matches() ) {

				final Permit permit=roster.profile(matcher.group("token"));

				if ( permit.valid(currentTimeMillis()) ) {

					handler.wrap(execute(permit.user(), permit.roles()))
							.handle(request, response);

				} else {

					response.status(Response.Unauthorized)
							.header("WWW-Authenticate", authenticate(request, "invalid_token"))
							.done();

				}

			} else { // no bearer token > fall-through to other authorization schemes

				handler.wrap(execute(Spec.none, singleton(Spec.none)))
						.handle(request, response);

			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper execute(final IRI user, final Collection<Value> roles) {
		return handler -> (request, response) -> handler.exec(

				writer -> writer
						.copy(request)
						.user(user)
						.roles(roles)
						.done(),

				reader -> {

					response.copy(reader);

					final int status=reader.status();

					// add authorization challenge, unless already provided by nested authorization schemes

					if ( status == Response.Unauthorized && reader.headers("WWW-Authenticate").isEmpty() ) {
						response.header("WWW-Authenticate", authenticate(request));
					}

					response.done();

				}

		);

	}


	private String authenticate(final Request request) {
		return "Bearer realm=\""+request.base()+"\"";
	}

	private String authenticate(final Request request, final String error) {
		return "Bearer realm=\""+request.base()+"\", error=\""+error+"\"";
	}

}
