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

import com.metreeca.form.things.Transputs;
import com.metreeca.next.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray._Tray;
import com.metreeca.tray.sys.Trace;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.CanonicalPathHandler;
import io.undertow.server.handlers.GracefulShutdownHandler;
import io.undertow.util.HttpString;

import java.io.*;
import java.util.function.Consumer;
import java.util.function.Function;


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

		this.handler=new GracefulShutdownHandler(new CanonicalPathHandler(exchange -> handler

				.handle(request(exchange))
				.accept(response(exchange))

		));

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request request(final HttpServerExchange exchange) {

		// !!! handle multi-part forms (https://stackoverflow.com/questions/37839418/multipart-form-data-example-using-undertow)

		final Request request=new Request();

		request.method(exchange.getRequestMethod().toString())

				.base(exchange.getRequestURL().substring(0, exchange.getRequestURL().length()-exchange.getRequestPath().length()+1))
				.path(exchange.getRelativePath()) // canonical form

				.query(exchange.getQueryString())
				// !!! NPE .parameters(exchange.getQueryParameters())
		;

		exchange.getRequestHeaders().forEach(header -> request.headers(header.getHeaderName().toString(), header));

		return request

				.body(() -> new Source() {

					@Override public Reader reader() throws IllegalStateException {
						return Transputs.reader(input(), exchange.getRequestCharset());
					}

					@Override public InputStream input() throws IllegalStateException {
						return exchange.getInputStream();
					}

				});
	}

	private Consumer<Response> response(final HttpServerExchange exchange) {
		return response -> {

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
		};
	}

}
