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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.rest.Codecs;
import com.metreeca.rest.services.Logger;

import java.util.regex.Pattern;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.Request.POST;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Logger.logger;

import static java.lang.String.format;


/**
 * Linked data server.
 *
 * <p>Provides default resource pre/postprocessing and error handling; mainly intended as the outermost wrapper
 * returned by gateway loaders.</p>
 */
public final class Server implements Wrapper {

	private static final Pattern TextualPattern=Pattern.compile("text/[-\\w]+|application/json");
	private static final Pattern URLEncodedPattern=Pattern.compile("application/x-www-form-urlencoded\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Logger logger=service(logger());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> consumer -> {
			try {

				request

						.map(this::query)
						.map(this::form)

						.map(handler::handle)

						.map(this::logging)
						.map(this::charset)

						.accept(consumer);

			} catch ( final RuntimeException e ) {

				request

						.reply(Failure.internal(e))

						.map(this::logging)

						.accept(consumer);

			}
		};
	}


	//// Pre-Processing ////////////////////////////////////////////////////////////////////////////////////////////////

	private Request query(final Request request) { // parse parameters from query string, if not already set
		return request.parameters().isEmpty() && request.method().equals(GET)
				? request.parameters(Codecs.parameters(request.query()))
				: request;
	}

	private Request form(final Request request) { // parse parameters from encoded form body, ignoring charset parameter
		return request.parameters().isEmpty()
				&& request.method().equals(POST)
				&& URLEncodedPattern.matcher(request.header("Content-Type").orElse("")).lookingAt()
				? request.parameters(Codecs.parameters(request.body(text()).value().orElse("")))
				: request;
	}


	//// Post-Processing ///////////////////////////////////////////////////////////////////////////////////////////////

	private Response logging(final Response response) { // log request outcome

		final Request request=response.request();
		final String method=request.method();
		final String item=request.item();

		final int status=response.status();
		final Throwable cause=response.cause().orElse(null);

		logger.entry(status < 400 ? Logger.Level.Info : status < 500 ? Logger.Level.Warning : Logger.Level.Error,
				this, () -> format("%s %s > %d", method, item, status), cause);

		return response;
	}

	private Response charset(final Response response) { // ;( prevent the container from adding its own default charset…

		response.header("Content-Type")
				.filter(type -> TextualPattern.matcher(type).matches()) // textual content with no charset
				.ifPresent(type -> response.header("Content-Type", type+"; charset=UTF-8"));

		return response;
	}

}
