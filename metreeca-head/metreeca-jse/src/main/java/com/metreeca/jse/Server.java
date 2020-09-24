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

package com.metreeca.jse;

import com.metreeca.rest.*;
import com.metreeca.rest.assets.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Function;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static java.util.function.Function.identity;

/**
 * Java SE HTTP server adapter.
 *
 * <p>Adapts web applications managed by a native Java SE {@linkplain HttpServer HTTP server} and
 * resource handlers based on the Metreeca/Link framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the {@linkplain Context context} managing shared assets required by resource handlers;
 * </li>
 *
 * <li>handles HTTP requests using a {@linkplain Handler handler} loaded from the context.</li>
 *
 * </ul>
 */
public final class Server {

	private final String root="/"; // must end with slash

	private final int backlog=128;
	private final int delay=0;


	private Function<Context, Handler> handler=context -> request -> request.reply(identity());

	private InetSocketAddress address=new InetSocketAddress("localhost", 8080);

	private final Context context=new Context();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the handler.
	 *
	 * @param factory the handler factory; takes as argument a shared asset context (which may configured with
	 *                additional application-specific assets as a side effect) and must return a non-null handler
	 *                to be used as main entry point for serving requests
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code factory} is null or returns null values
	 */
	public Server handler(final Function<Context, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null handler factory");
		}

		this.handler=factory;

		return this;
	}


	/**
	 * Configures the socket address.
	 *
	 * @param address the socket address to listen to
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code address} is null
	 */
	public Server address(final InetSocketAddress address) {

		if ( address == null ) {
			throw new NullPointerException("null address");
		}

		this.address=address;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void start() {
		try {

			final Handler handler=Objects.requireNonNull(this.handler.apply(context), "null handler");

			final Logger logger=context.get(logger());

			final HttpServer server=HttpServer.create(address, backlog);

			server.setExecutor(Executors.newCachedThreadPool());

			server.createContext(root, exchange -> {
				try {

					context.exec(() -> handler.handle(request(exchange))
							.map(response -> response.status() > 0 ? response : response.status(NotFound))
							.accept(response -> response(exchange, response))
					);

				} catch ( final RuntimeException e ) {

					if ( !e.toString().toLowerCase(Locale.ROOT).contains("broken pipe") ) {
						logger.error(this, "unhandled exception", e);
					}

				}
			});

			Runtime.getRuntime().addShutdownHook(new Thread(() -> {

				logger.info(this, "server stopping");

				try { server.stop(delay); } catch ( final RuntimeException e ) {
					logger.error(this, "unhandled exception while stopping server", e);
				}

				try { context.clear(); } catch ( final RuntimeException e ) {
					logger.error(this, "unhandled exception while releasing resources", e);
				}

				logger.info(this, "server stopped");

			}));

			logger.info(this, "server starting");

			server.start();

			logger.info(this, String.format("server listening at <http://%s:%d/>",
					address.getHostString(), address.getPort()
			));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request request(final HttpExchange exchange) {

		final URI uri=exchange.getRequestURI();

		final String base=String.format("http://%s%s", exchange.getRequestHeaders().getFirst("Host"), root);
		final String path=Optional.ofNullable(uri.getPath()).orElse("/").substring(root.length()-1);

		return new Request()
				.method(exchange.getRequestMethod())
				.base(base)
				.path(path)
				.query(Optional.ofNullable(uri.getRawQuery()).orElse(""))
				.body(input(), exchange::getRequestBody);
	}

	private void response(final HttpExchange exchange, final Response response) {
		try {

			exchange.getResponseHeaders().putAll(response.headers());

			exchange.sendResponseHeaders(response.status(), exchange.getRequestMethod().equals(HEAD) ? -1L : response
					.header("Content-Length")
					.map(s -> {
						try {
							return Long.parseUnsignedLong(s);
						} catch ( final NumberFormatException e ) {
							return null;
						}
					})
					.orElse(0L)
			);

			response.body(output()).accept(e -> {}, target -> {
				try ( final OutputStream output=exchange.getResponseBody() ) {

					target.accept(output);

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		} finally {

			exchange.close();

		}

	}

}
