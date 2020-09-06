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

import com.metreeca.core.*;
import com.metreeca.core.assets.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.NotFound;
import static com.metreeca.core.assets.Logger.logger;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;

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
public final class JSEServer {

	private final String root="/"; // must end with slash

	private final int backlog=128;
	private final int delay=0;

	private Function<Context, Handler> factory=context -> request -> request.reply(status(NotFound));

	private final Context context=new Context();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/*
	 * Configures the main handler.
	 *
	 * @param context the shared asset context; may be configured with additional application-specific assets
	 *
	 * @return a non-null resource handler to be used as main entry point for serving requests
	 */
	public JSEServer handler(final Function<Context, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null handler factory");
		}

		this.factory=factory;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void start() {
		start(new InetSocketAddress(8080));
	}

	public void start(final InetSocketAddress address) {

		if ( address == null ) {
			throw new NullPointerException("null address");
		}

		try {

			final Handler handler=Objects.requireNonNull(factory.apply(context), "null handler");
			final Logger logger=context.get(logger());

			final HttpServer server=HttpServer.create(address, backlog);

			server.createContext(root, exchange -> {
				try {

					context.exec(() ->
							handler.handle(request(exchange)).accept(response -> response(exchange, response))
					);

				} catch ( final RuntimeException e ) {
					logger.error(this, "unhandled exception", e);
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

			exchange.sendResponseHeaders(response.status(), response
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

			exchange.close();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}

	}

}
