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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.regex.Pattern;

import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.Request.POST;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;


/**
 * Linked data server.
 *
 * <p>Provides default resource pre/post-processing and error handling; mainly intended as the outermost wrapper
 * returned by gateway loaders.</p>
 */
public final class Server implements Wrapper {

	private static final Pattern CharsetPattern=Pattern.compile(";\\s*charset\\s*=.*$");
	private static final Pattern URLEncodedPattern=Pattern.compile("application/x-www-form-urlencoded\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Trace trace=tool(Trace.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> {
			try {

				return consumer -> request

						.map(this::query)
						.map(this::form)

						.map(handler::handle)

						.map(this::logging)
						.map(this::charset)

						.accept(consumer);

			} catch ( final RuntimeException e ) {

				trace.error(this, format("%s %s > internal error", request.method(), request.item()), e);

				return request.reply(new Failure()
						.status(Response.InternalServerError)
						.error("exception-untrapped")
						.cause("unable to process request: see server logs for details")
						.cause(e));

			}
		};
	}


	//// Pre-Processing ////////////////////////////////////////////////////////////////////////////////////////////////

	private Request query(final Request request) { // parse parameters from query string, if not already set
		return request.parameters().isEmpty() && request.method().equals(GET)
				? request.parameters(parse(request.query()))
				: request;
	}

	private Request form(final Request request) { // parse parameters from encoded form body, ignoring charset parameter
		return request.parameters().isEmpty()
				&& request.method().equals(POST)
				&& URLEncodedPattern.matcher(request.header("Content-Type").orElse("")).lookingAt()
				? request.parameters(parse(request.body(text()).value().orElse("")))
				: request;
	}


	//// Post-Processing ///////////////////////////////////////////////////////////////////////////////////////////////

	private Response logging(final Response response) { // log request outcome

		final Request request=response.request();
		final String method=request.method();
		final IRI item=request.item();

		final int status=response.status();
		final Throwable cause=response.cause().orElse(null);

		trace.entry(status < 400 ? Trace.Level.Info : status < 500 ? Trace.Level.Warning : Trace.Level.Error,
				this, () -> format("%s %s > %d", method, item, status), cause);

		return response;
	}

	private Response charset(final Response response) { // prevent the container from adding its default charset…

		response.header("Content-Type")
				.filter(type -> !CharsetPattern.matcher(type).find())
				.filter(type -> type.startsWith("text/") || type.equals("application/json"))
				.ifPresent(type -> response.header("Content-Type", type+";charset=UTF-8"));

		return response;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Map<String, List<String>> parse(final String query) {

		final Map<String, List<String>> parameters=new LinkedHashMap<>();

		final int length=query.length();

		for (int head=0, tail; head < length; head=tail+1) {
			try {

				final int equal=query.indexOf('=', head);
				final int ampersand=query.indexOf('&', head);

				tail=(ampersand >= 0) ? ampersand : length;

				final boolean split=equal >= 0 && equal < tail;

				final String label=URLDecoder.decode(query.substring(head, split ? equal : tail), "UTF-8");
				final String value=URLDecoder.decode(query.substring(split ? equal+1 : tail, tail), "UTF-8");

				parameters.compute(label, (name, values) -> {

					final List<String> strings=(values != null) ? values : new ArrayList<>();

					strings.add(value);

					return strings;

				});

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}
		}

		return unmodifiableMap(parameters);
	}

}
