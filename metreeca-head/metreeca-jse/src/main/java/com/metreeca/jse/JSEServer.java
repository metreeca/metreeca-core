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
import com.metreeca.rest.assets.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.rdf4j.model.IRI;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.json.Values.AbsoluteIRIPattern;
import static com.metreeca.json.Values.format;
import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Xtream.guarded;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;

/**
 * Java SE HTTP server connector.
 *
 * <p>Connects web applications managed by a native Java SE {@linkplain HttpServer HTTP server} with
 * resource handlers based on the Metreeca/Link framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the {@linkplain Context context} managing shared assets required by resource handlers;</li>
 *
 * <li>handles HTTP requests using a {@linkplain Handler handler} loaded from the context.</li>
 *
 * </ul>
 */
public final class JSEServer {

	private static final String DefaultHost="localhost";
	private static final int DefaultPort=8080;

	private static final Pattern AddressPattern=Pattern.compile(
			"(?<host>^|[-+._a-zA-Z0-9]*[-+._a-zA-Z][-+._a-zA-Z0-9]*)(?:^:?|:)(?<port>\\d{1,4}|$)"
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private InetSocketAddress address=new InetSocketAddress(DefaultHost, DefaultPort);

	private String base="";
	private String root="/";

	private final int backlog=128;
	private final int delay=0;

	private final Context context=new Context();


	private static Supplier<Handler> handler() { return () -> request -> request.reply(identity()); }


	private static String normalize(final String path) {
		return Optional.of(path)
				.map(p -> p.startsWith("/") ? p : "/"+p)
				.map(p -> p.endsWith("/") ? p : p+"/")
				.orElse(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String base(final String host) {
		return base.isEmpty() ? format("http://%s%s", Optional.ofNullable(host).orElse(DefaultHost), root) : base;
	}

	private String path(final String path) {
		return Optional.ofNullable(path).orElse("/").substring(root.length()-1);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the delegate handler.
	 *
	 * @param factory a handler factory; takes as argument a shared asset context (which may configured with additional
	 *                application-specific assets as a side effect) and must return a non-null handler to be used as
	 *                entry point for serving requests
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code factory} is null or returns null values
	 */
	public JSEServer delegate(final Function<Context, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null handler factory");
		}

		context.set(handler(), () -> requireNonNull(factory.apply(context), "null handler"));

		return this;
	}


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
			throw new IllegalArgumentException(String.format("malformed address <%s>", address));
		}

		final String host=Optional
				.of(matcher.group("host"))
				.filter(s -> !s.isEmpty())
				.orElse(DefaultHost);

		final int port=Optional
				.of(matcher.group("port"))
				.filter(s -> !s.isEmpty())
				.map(Integer::valueOf)
				.orElse(8080);

		this.address=new InetSocketAddress(host, port);

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
	 * Configures the context path.
	 *
	 * @param context the context path for the root resource of this server; if missing, leading and trailing slashes
	 *                will be automatically added
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code context} is null
	 */
	public JSEServer context(final String context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		this.base="";
		this.root=normalize(context);

		return this;
	}

	/**
	 * Configures the context path.
	 *
	 * @param context the context path for the root resource of this server; if missing, leading and trailing slashes
	 *                will be automatically added
	 *
	 * @return this server
	 *
	 * @throws NullPointerException if {@code context} is null
	 */
	public JSEServer context(final IRI context) {

		if ( context == null ) {
			throw new NullPointerException("null context");
		}

		final Matcher matcher=AbsoluteIRIPattern.matcher(context.stringValue());

		if ( !matcher.matches() ) {
			throw new IllegalArgumentException(format("illegal context IRI %s", format(context)));
		}

		final String scheme=matcher.group("schemeall");
		final String host=matcher.group("hostall");
		final String path=normalize(matcher.group("path"));

		this.base=scheme+host+path;
		this.root=path;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void start() {
		try {

			final Handler handler=context.get(handler());
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

			logger.info(this, format("server listening at <http://%s:%d/>",
					address.getHostString(), address.getPort()
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
				.base(base(exchange.getRequestHeaders().getFirst("Host")))
				.path(path(uri.getPath()))
				.query(Optional.ofNullable(uri.getRawQuery()).orElse(""))
				.headers(exchange.getRequestHeaders())
				.body(input(), exchange::getRequestBody);
	}

	private void response(final HttpExchange exchange, final Response response) {
		try {

			response.headers().entrySet().stream() // Content-Length is generated by server
					.filter(entry -> !entry.getKey().equalsIgnoreCase("Content-Length"))
					.forEachOrdered(entry -> exchange.getResponseHeaders().put(entry.getKey(), entry.getValue()));

			final long length=exchange.getRequestMethod().equals(HEAD) ? -1L : response
					.header("Content-Length")
					.map(guarded(Long::parseUnsignedLong))
					.orElse(0L); // chunked transfer

			exchange.sendResponseHeaders(response.status(), length);

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
