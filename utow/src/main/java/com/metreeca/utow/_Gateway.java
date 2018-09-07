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

package com.metreeca.utow;

import com.metreeca.next.Handler;
import com.metreeca.next.Request;
import com.metreeca.next.Target;
import com.metreeca.tray.Tray;
import com.metreeca.tray._Tray;
import com.metreeca.tray.sys.Trace;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.io.*;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * Undertow gateway.
 *
 * <p>Provides a gateway between a web application managed by Servlet 3.1 container and linked data resource handlers
 * based on the Metreeca/Link linked data framework:</p>
 *
 * <ul>
 *
 * <li>initializes and destroys the shared tool {@linkplain _Tray tray} managing platform components required by
 * resource handlers;</li>
 *
 * <li>intercepts HTTP requests and handles them using the {@linkplain Server server} tool provided by the shared tool
 * tray.</li>
 *
 * </ul>
 */
public final class _Gateway implements HttpHandler {

	public static void run(final int port, final String host, final Function<Tray, Handler> loader) {

		if ( port < 0 ) {
			throw new IllegalArgumentException("illegal port ["+port+"]");
		}

		if ( host == null ) {
			throw new NullPointerException("null host");
		}

		if ( loader == null ) {
			throw new NullPointerException("null loader");
		}

		final _Gateway gateway=new _Gateway();

		final Undertow server=Undertow.builder()
				.addHttpListener(port, host, gateway.start(loader))
				.build();

		Runtime.getRuntime().addShutdownHook(new Thread(() -> { // !!! ;( randomly skipped

			gateway.stop();
			server.stop();

		}));

		server.start();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Tray tray=new Tray();


	private GracefulShutdownHandler handler;


	@Override public void handleRequest(final HttpServerExchange exchange) throws Exception {
		handler.handleRequest(exchange);
	}


	public _Gateway start(final Function<Tray, Handler> loader) {

		if ( loader == null ) {
			throw new NullPointerException("null loader");
		}

		if ( handler != null ) {
			throw new IllegalStateException("active gateway");
		}

		tray.get(Trace.Factory).info(this, "starting");

		final Handler handler=loader.apply(tray);

		this.handler=new GracefulShutdownHandler(exchange -> {

			final Request request=new Request();

			request.method(exchange.getRequestMethod().toString());

			exchange.getRequestHeaders().forEach(header -> request.headers(header.getHeaderName().toString(), header));

			handler

					// !!! handle multi-part forms (https://stackoverflow.com/questions/37839418/multipart-form-data-example-using-undertow)

					.handle(request)

					.accept(response -> {

						exchange.setStatusCode(response.status());

						response.headers().forEach((name, values) ->
								exchange.getResponseHeaders().putAll(HttpString.tryFromString(name), values)
						);

						try (final StringWriter writer=new StringWriter(1000)) {

							response.body().accept(new Target() {
								@Override public Writer writer() { return writer; }
							});

							exchange.getResponseSender().send(writer.toString()); // !!! stream

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}

						// !!! handle binary data

					});
		});

		return this;
	}

	public _Gateway stop() {

		if ( handler == null ) {
			throw new IllegalStateException("inactive gateway");
		}

		tray.get(Trace.Factory).info(this, "stopping");

		try {

			handler.shutdown();
			handler.addShutdownListener(success -> tray.clear());
			handler.awaitShutdown();

		} catch ( final InterruptedException ignored ) {

		} finally {

			handler=null;

		}

		return this;
	}

}
