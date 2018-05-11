/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link.services;

import com.metreeca.link.*;
import com.metreeca.link.handlers.Dispatcher;
import com.metreeca.tray.Tool;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.function.BiConsumer;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.spec.Values.literal;

import static java.util.Arrays.asList;


/**
 * SPARQL proxy endpoint.
 */
public class Proxy implements Service {

	private int timeoutConnect; // [s]
	private int timeoutRead; // [s]


	@Override public void load(final Tool.Loader tools) {

		final Setup setup=tools.get(Setup.Tool);

		timeoutConnect=setup.get("proxy.timeout.connect", 30);
		timeoutRead=setup.get("proxy.timeout.read", 60);

		tools.get(Index.Tool).insert("/proxy", new Dispatcher(map(

				entry(Request.GET, this::handle),
				entry(Request.POST, this::handle)

		)), map(

				entry(RDFS.LABEL, literal("SPARQL Proxy Endpoint"))

		));
	}


	private void handle(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {
		try {

			sink.accept(request, out(response, in(request)));

		} catch ( final IOException|RuntimeException e ) {

			tools.get(Trace.Tool).warning(this, "failed proxy request", e);

			sink.accept(request, response

					.setStatus(e instanceof IllegalArgumentException ? Response.BadRequest // !!! review
							: e instanceof IOException ? Response.BadGateway
							: Response.InternalServerError)

					.setHeader("Content-Type", "application/text")
					.setText(e.getMessage()+"\n"));

		}
	}


	private HttpURLConnection in(final Request request) throws IOException {

		final String endpoint=request.getParameter("endpoint").orElse("");
		final String query=request.getParameter("query").orElse("");

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

			request.getHeader(header).ifPresent(value -> connection.setRequestProperty(header, value));

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

		response.setStatus(code/100 == 5 ? Response.BadGateway : code); // !!! relay code/message on BadGateway error

		// transfer server-controlled headers to the browser
		// ;( if switching to connection.getHeaderFields(), be aware that header name are case-sensitive…

		for (final String header : asList(
				"Content-Type",
				"WWW-Authenticate",
				"Server"
		)) {

			final String value=connection.getHeaderField(header);


			if ( value != null ) { response.addHeader(header, value); }

		}

		return response.setBody(out -> {

			try (final InputStream in=connect(connection)) {

				final byte[] buffer=new byte[1024];

				for (int i; (i=in.read(buffer)) >= 0; ) {
					out.write(buffer, 0, i);
				}

				out.flush();

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
