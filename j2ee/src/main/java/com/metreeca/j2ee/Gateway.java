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

package com.metreeca.j2ee;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.Loader;
import com.metreeca.tray.sys.Storage;
import com.metreeca.tray.sys.Trace;

import java.io.*;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.OutputBody.output;

import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;


/**
 * J2EE gateway.
 *
 * <p>Provides a gateway between a web application managed by Servlet 3.1 container and resource handlers based on the
 * Metreeca/Link linked data framework:</p>
 *
 * <ul>
 *
 * <li>initializes and destroys the shared tool {@linkplain Tray tray} managing platform components required by
 * resource handlers;</li>
 *
 * <li>intercepts HTTP requests and handles them using a linked data {@linkplain Handler handler} loaded from the
 * shared tool tray;</li>
 *
 * <li>forwards HTTP requests to the enclosing web application if no response is committed by the linked data
 * server.</li>
 *
 * </ul>
 */
public abstract class Gateway implements ServletContextListener {

	private final Tray tray=new Tray();

	private final String pattern;
	private final Function<Tray, Handler> loader;


	/**
	 * Creates a new J2EE gateway.
	 *
	 * @param pattern the URL pattern matching requests paths to be handled by the new gateway
	 * @param loader  the handler loader; the loader is passed a configurable tool tray and is expected to return a
	 *                non-null resource handler
	 *
	 * @throws NullPointerException if either {@code pattern} or {@code loader} is null
	 */
	protected Gateway(final String pattern, final Function<Tray, Handler> loader) {

		if ( pattern == null ) {
			throw new NullPointerException("null pattern");
		}

		if ( loader == null ) {
			throw new NullPointerException("null loader");
		}

		this.pattern=pattern;
		this.loader=loader;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void contextInitialized(final ServletContextEvent event) {

		final ServletContext context=event.getServletContext();

		try {

			final Handler handler=requireNonNull(loader.apply(tray

					.set(Storage.Factory, () -> storage(context))
					.set(Loader.Factory, () -> loader(context))

			), "null resource handler");

			context.addFilter(Gateway.class.getName(), tray.get(() -> new GatewayFilter(handler)))
					.addMappingForUrlPatterns(null, false, pattern);

		} catch ( final Throwable t ) {

			try (final StringWriter message=new StringWriter()) {

				t.printStackTrace(new PrintWriter(message));

				tray.get(Trace.Factory).error(this, "error during initialization: "+message);

				context.log("error during initialization", t);

				throw t;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				tray.clear();

			}

		}
	}

	@Override public void contextDestroyed(final ServletContextEvent event) {

		tray.clear();

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Storage storage(final ServletContext context) {
		return name -> {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			return new File((File)context.getAttribute(ServletContext.TEMPDIR), name);
		};
	}

	private Loader loader(final ServletContext context) {
		return path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return Optional.ofNullable(context.getResourceAsStream(path));

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class GatewayFilter implements Filter {

		private final Handler handler;


		private GatewayFilter(final Handler handler) {
			this.handler=handler;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		@Override public void init(final FilterConfig config) {}

		@Override public void destroy() {}


		@Override public void doFilter(
				final ServletRequest request, final ServletResponse response, final FilterChain chain
		) throws ServletException, IOException {

			handler.handle(request((HttpServletRequest)request))
					.accept(r -> response((HttpServletResponse)response, r));

			if ( !response.isCommitted() ) {
				chain.doFilter(request, response);
			}

		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private Request request(final HttpServletRequest http) {

			final String target=http.getRequestURL().toString();
			final String path=http.getRequestURI().substring(http.getContextPath().length());
			final String base=target.substring(0, target.length()-path.length()+1);
			final String query=http.getQueryString();

			final Request request=new Request()
					.method(http.getMethod())
					.base(base)
					.path(path)
					.query(query != null ? query : "");

			for (final Map.Entry<String, String[]> parameter : http.getParameterMap().entrySet()) {
				request.parameters(parameter.getKey(), asList(parameter.getValue()));
			}

			for (final String name : list(http.getHeaderNames())) {
				request.headers(name, list(http.getHeaders(name)));
			}

			return request.body(input(), () -> {
				try {
					return http.getInputStream();
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});

		}

		private void response(final HttpServletResponse http, final Response response) {
			if ( response.status() > 0 ) { // only if actually processed

				http.setStatus(response.status());

				response.headers().forEach((name, values) ->
						values.forEach(value -> http.addHeader(name, value))
				);

				// ignore missing response bodies // !!! handle other body retrieval errors

				response.body(output()).value().ifPresent(consumer -> consumer.accept(() -> {
					try {
						return http.getOutputStream();
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}));

				if ( !http.isCommitted() ) { // flush if not already committed by bodies
					try {
						http.flushBuffer();
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}

			}
		}

	}

}
