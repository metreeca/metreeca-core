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

package com.metreeca.link.services;

import com.metreeca.link.*;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import static com.metreeca.link.Handler.error;
import static com.metreeca.link.handlers.Dispatcher.dispatcher;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;


/**
 * SPARQL proxy endpoint.
 */
public class Proxy implements Service {

	private static final String Path="/proxy";


	private final Setup setup=tool(Setup.Tool);
	private final Index index=tool(Index.Tool);
	private final Trace trace=tool(Trace.Tool);


	private final int timeoutConnect=setup.get("proxy.timeout.connect", 30); // [s]
	private final int timeoutRead=setup.get("proxy.timeout.read", 60); // [s]


	@Override public void load() {
		index.insert(Path, dispatcher()

				.get(this::handle)
				.post(this::handle));


		// !!! port metadata
		//map(
		//
		//		entry(RDFS.LABEL, literal("SPARQL Proxy Endpoint"))
		//
		//)
	}


	private void handle(final Request request, final Response response) {
		try {

			out(response, in(request));

		} catch ( final IOException|RuntimeException e ) {

			trace.warning(this, "failed proxy request", e);

			response.status(e instanceof IllegalArgumentException ? Response.BadRequest // !!! review
							: e instanceof IOException ? Response.BadGateway
							: Response.InternalServerError)

					.cause(e)
					.json(error("request-failed", e));

		}
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

		response.status(code/100 == 5 ? _Response.BadGateway : code); // !!! relay code/message on BadGateway error

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

		return response.output(output -> {

			try (final InputStream in=connect(connection)) {

				final byte[] buffer=new byte[1024];

				for (int i; (i=in.read(buffer)) >= 0; ) {
					output.write(buffer, 0, i);
				}

				output.flush();

			} catch ( IOException e ) {
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
