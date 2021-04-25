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
import com.metreeca.rest.formats.JSONFormat;
import com.metreeca.rest.formats.TextFormat;
import com.metreeca.rest.services.Logger;

import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Request.*;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Logger.Level.*;
import static com.metreeca.rest.services.Logger.logger;

import static java.lang.String.format;
import static java.util.function.Function.identity;


/**
 * API server.
 *
 * <p>Provides default resource pre/postprocessing and error handling; mainly intended as the outermost wrapper
 * returned by loaders.</p>
 */
public final class Server implements Wrapper {

	private static final Pattern TextualPattern=Pattern.compile(TextFormat.MIMEPattern+"|"+JSONFormat.MIMEPattern);
	private static final Pattern URLEncodedPattern=Pattern.compile("application/x-www-form-urlencoded\\b");


	/**
	 * Creates an API server.
	 *
	 * @return a new API server
	 */
	public static Server server() {
		return new Server();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Logger logger=service(logger());

	private Server() {}


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

			} catch ( final RuntimeException e ) { // try to send a new response

				request

						.reply(status(InternalServerError, e))

						.map(this::logging)

						.accept(consumer);

			}
		};
	}


	//// Pre-Processing ///////////////////////////////////////////////////////////////////////////////////////////////

	private Request query(final Request request) { // parse parameters from query string, if not already set
		return request.parameters().isEmpty() && request.method().equals(GET)
				? request.parameters(search(request.query()))
				: request;
	}

	private Request form(final Request request) { // parse parameters from encoded form body, ignoring charset
		return request.parameters().isEmpty()
				&& request.method().equals(POST)
				&& URLEncodedPattern.matcher(request.header("Content-Type").orElse("")).lookingAt()
				? request.parameters(search(request.body(text()).fold(e -> "", identity()))) // !!! error handling?
				: request;
	}


	//// Post-Processing //////////////////////////////////////////////////////////////////////////////////////////////

	private Response logging(final Response response) { // log request outcome

		final Request request=response.request();
		final String method=request.method();
		final String item=request.item();

		final int status=response.status();
		final Throwable cause=response.cause().orElse(null);

		final Logger.Level level=(status < 400) ? info
				: (status < 500) ? warning
				: error;

		logger.entry(level, this, () -> format("%s %s > %d", method, item, status), cause);

		return response;
	}

	private Response charset(final Response response) { // ;( prevent the container from adding its default charset…

		response.header("Content-Type")
				.filter(type -> TextualPattern.matcher(type).matches()) // textual content with no charset
				.ifPresent(type -> response.header("Content-Type", type+"; charset=UTF-8"));

		return response;
	}

}
