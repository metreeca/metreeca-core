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

package com.metreeca.jse;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Xtream.guarded;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.services.Logger.logger;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

/**
 * Java SE HTTP server connector.
 *
 * <p>Connects web applications managed by a native Java SE {@linkplain HttpServer HTTP server} with
 * resource handlers based on the Metreeca/Link framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the {@linkplain Toolbox toolbox} managing shared services required by resource handlers;
 * </li>
 *
 * <li>handles HTTP requests using a {@linkplain Handler handler} loaded from the toolbox.</li>
 *
 * </ul>
 */
public final class JSEServer {

	private static final String DefaultHost="localhost";
	private static final int DefaultPort=8080;

	private static final Pattern ContextPattern=Pattern.compile(
			"(?<base>(?:\\w+://[^/?#]*)?)(?<path>.*)"
	);

	private static final Pattern AddressPattern=Pattern.compile(
			"(?<host>^|[-+._a-zA-Z0-9]*[-+._a-zA-Z][-+._a-zA-Z0-9]*)(?:^:?|:)(?<port>\\d{1,4}|$)"
	);


	private static Supplier<Handler> delegate() { return () -> request -> request.reply(identity()); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private InetSocketAddress address=new InetSocketAddress(DefaultHost, DefaultPort);

	private String base="";
	private String path="/";

	private final int backlog=128;
	private final int delay=0;

	private final Toolbox toolbox=new Toolbox();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the socket address.
	 *
	 * @param address the socket address to listen to; must match one of the following formats: {@code <host>:<port>},
	 *                {@code <host>}, {@code <port>}
	 *
	 * @return this server
	 *
	 * @throws NullPointerException     if {@code address} is null
	 * @throws IllegalArgumentException if {@code address} is malformed
	 */
	public JSEServer address(final String address) {

		if ( address == null ) {
			throw new NullPointerException("null address");
		}

		final Matcher matcher=AddressPattern.matcher(address);

		if ( !matcher.matches() ) {
			throw new IllegalArgumentException(format("malformed address <%s>", address));
		}

		this.address=new InetSocketAddress(

				Optional
						.of(matcher.group("host"))
						.filter(host -> !host.isEmpty())
						.orElse(DefaultHost),

				Optional
						.of(matcher.group("port"))
						.filter(port -> !port.isEmpty())
						.map(Integer::valueOf)
						.orElse(DefaultPort)

		);

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
	public JSEServer address(final InetSocketAddress address) {

		if ( address == null ) {
			throw new NullPointerException("null address");
		}

		this.address=address;

		return this;
	}


	/**
	 * Configures the context.
	 *
	 * @param context the context IRI for the root resource of this server; accepts root-relative paths
	 *
	 * @return this server
	 *
	 * @throws NullPointerException     if {@code context} is null
	 * @throws IllegalArgumentException if {@code context} is malformed
	 */
	public JSEServer context(final String context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		final Matcher matcher=ContextPattern.matcher(context);

		if ( !matcher.matches() ) {
			throw new IllegalArgumentException(format("malformed context IRI <%s>", context));
		}

		this.base=matcher.group("base");

		this.path=Optional
				.of(matcher.group("path"))
				.map(p -> p.startsWith("/") ? p : "/"+p)
				.map(p -> p.endsWith("/") ? p : p+"/")
				.get();

		return this;
	}


	/**
	 * Configures the delegate handler factory.
	 *
	 * @param factory the delegate handler factory; takes as argument a shared service manager (which may configured
	 *                   with
	 *                additional application-specific services as a side effect) and must return a non-null handler
	 *                to be
	 *                used as entry point for serving requests
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code factory} is null or returns a null value
	 */
	public JSEServer delegate(final Function<Toolbox, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null handler factory");
		}

		toolbox.set(delegate(), () -> requireNonNull(factory.apply(toolbox), "null handler"));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void start() {
		try {

			final Handler handler=toolbox.get(delegate());
			final Logger logger=toolbox.get(logger());

			final HttpServer server=HttpServer.create(address, backlog);

			server.setExecutor(Executors.newCachedThreadPool());

			server.createContext(path, exchange -> {
				try {

					toolbox.exec(() -> handler.handle(request(exchange))
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

				try { toolbox.clear(); } catch ( final RuntimeException e ) {
					logger.error(this, "unhandled exception while releasing resources", e);
				}

				logger.info(this, "server stopped");

			}));

			logger.info(this, "server starting");

			server.start();

			logger.info(this, format("server listening at <http://%s:%d%s>",
					address.getHostString(), address.getPort(), path
			));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request request(final HttpExchange exchange) {

		final URI uri=exchange.getRequestURI();

		return new Request()

				.method(exchange.getRequestMethod())

				.base((base.isEmpty() ? format("http://%s",

						Optional.ofNullable(exchange.getRequestHeaders().getFirst("Host")).orElse(DefaultHost)

				) : base)+path)

				.path(Optional.ofNullable(uri.getPath()).orElse("/").substring(path.length()-1))

				.query(Optional.ofNullable(uri.getRawQuery()).orElse(""))

				.headers(exchange.getRequestHeaders().entrySet().stream() // ;( possibly null header names…
						.filter(entry -> nonNull(entry.getKey()) && nonNull(entry.getValue()))
						.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
				)

				.body(input(), exchange::getRequestBody);
	}

	private void response(final HttpExchange exchange, final Response response) {
		try {

			response.headers().entrySet().stream() // Content-Length is generated by server
					.filter(entry -> !entry.getKey().equalsIgnoreCase("Content-Length"))
					.forEachOrdered(entry -> exchange.getResponseHeaders().put(entry.getKey(), entry.getValue()));

			response.body(output()).accept(

					error -> {
						try {

							final int status=error.getStatus() == 0  // undefined output body
									? response.status()  // return response status
									: error.getStatus(); // return error status

							exchange.sendResponseHeaders(status, -1L); // no output

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					},

					value -> {
						try ( final OutputStream output=exchange.getResponseBody() ) {

							final long length=exchange.getRequestMethod().equals(HEAD) ? -1L : response
									.header("Content-Length")
									.map(guarded(Long::parseUnsignedLong))
									.orElse(0L); // chunked transfer

							exchange.sendResponseHeaders(response.status(), length);

							value.accept(output);

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}

			);

		} finally {

			exchange.close();

		}

	}

}
