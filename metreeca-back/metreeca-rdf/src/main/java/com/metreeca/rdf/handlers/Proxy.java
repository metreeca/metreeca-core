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

package com.metreeca.rdf.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.rest.services.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.services.Logger.logger;

import static java.util.Arrays.asList;


/**
 * SPARQL proxy endpoint.
 *
 * <p>Forwards SPARQL queries to the remote endpoint defined by the {@code endpoint} query parameter of the incoming
 * request.</p>
 */
public final class Proxy implements Handler {

	// !!! support hardwired remote endpoint

	private static final int timeoutConnect=30; // [s]
	private static final int timeoutRead=60; // [s]


	private final Logger logger=service(logger());

	private final Handler delegate=new Worker()
			.get(this::process)
			.post(this::process);


	@Override public Future<Response> handle(final Request request) {
		return delegate.handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> process(final Request request) {
		return consumer -> request.reply(response -> {
			try {

				return out(response, in(request));

			} catch ( final IOException|RuntimeException e ) {

				logger.warning(this, "failed proxy request", e);

				final int i=e instanceof IllegalArgumentException ? Response.BadRequest // !!! review
						: e instanceof IOException ? Response.BadGateway
						: Response.InternalServerError;

				return response.map(new Failure()
						.status(i)
						.error("request-failed")
						.cause(e));

			}
		}).accept(consumer);
	}


	private HttpURLConnection in(final Request request) throws IOException {

		final String endpoint=request.parameter("endpoint").orElse("");
		final String query=request.parameter("query").orElse("");

		if ( endpoint.isEmpty() ) {
			throw new IllegalArgumentException("missing endpoint parameter");
		}

		if ( query.isEmpty() ) {
			throw new IllegalArgumentException("missing query parameter");
		}

		final URL url=new URL(endpoint);
		final HttpURLConnection connection=(HttpURLConnection)url.openConnection();

		connection.setRequestMethod("POST");
		connection.setConnectTimeout(timeoutConnect*1000);
		connection.setReadTimeout(timeoutRead*1000);
		connection.setDoOutput(true);
		connection.setDoInput(true);

		final byte[] data=("query="+URLEncoder.encode(query, "UTF-8")).getBytes();

		// transfer browser-controlled headers to the endpoint
		// ;( if switching to request.getHeaderNames(), be aware that header name may be case-sensitive…

		for (final String header : asList(
				"Cookie",
				"Accept",
				"Authorization"
		)) {

			request.header(header).ifPresent(value -> connection.setRequestProperty(header, value));

		}

		// ;(sesame) explicitly add charset=UTF-8 to content-type to prevent encoding issues
		// (see https://openrdf.atlassian.net/browse/SES-2301)

		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		connection.setRequestProperty("Content-Length", String.valueOf(data.length));
		connection.setRequestProperty("Cache-Control", "no-cache");
		connection.setRequestProperty("Connection", "keep-alive");

		connection.connect();

		try (final OutputStream stream=connection.getOutputStream()) {
			stream.write(data);
		}

		return connection;
	}

	private Response out(final Response response, final HttpURLConnection connection) throws IOException {

		final int code=connection.getResponseCode();
		final String message=connection.getResponseMessage();

		response.status(code/100 == 5 ? Response.BadGateway : code); // !!! relay code/message on BadGateway error

		// transfer server-controlled headers to the browser
		// ;( if switching to connection.getHeaderFields(), be aware that header name are case-sensitive…

		for (final String header : asList(
				"Content-Type",
				"WWW-Authenticate",
				"Server"
		)) {

			final String value=connection.getHeaderField(header);

			if ( value != null ) { response.header(header, value); }

		}

		return response.body(output(), target -> {
			try (
					final OutputStream output=target.get();
					final InputStream input=connect(connection)
			) {

				final byte[] buffer=new byte[1024];

				for (int i; (i=input.read(buffer)) >= 0; ) {
					output.write(buffer, 0, i);
				}

				output.flush();

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});

	}

	private InputStream connect(final HttpURLConnection connection) {
		try {

			return connection.getInputStream();

		} catch ( final IOException ignored ) {

			return connection.getErrorStream();

		}
	}

}
