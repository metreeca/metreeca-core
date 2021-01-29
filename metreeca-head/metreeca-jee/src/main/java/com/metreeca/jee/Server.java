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

package com.metreeca.jee;

import com.metreeca.rest.*;
import com.metreeca.rest.assets.Loader;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;
import static java.util.function.Function.identity;


/**
 * Java EE Servlet connector.
 *
 * <p>Connects web applications managed by Servlet 3.1 container with resource handlers based on the Metreeca/Link
 * framework:</p>
 *
 * <ul>
 *
 * <li>initializes and cleans the {@linkplain Context context} managing shared assets required by resource handlers;
 * </li>
 *
 * <li>intercepts HTTP requests and handles them using a  {@linkplain Handler handler} loaded from the context;</li>
 *
 * <li>forwards HTTP requests to the enclosing web application if no response is committed by REST handlers.</li>
 *
 * </ul>
 */
public abstract class Server implements Filter {

	private final Context context=new Context();

	private final Supplier<Handler> handler=() -> request -> request.reply(identity());


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
	protected Server handler(final Function<Context, Handler> factory) {

		if ( factory == null ) {
			throw new NullPointerException("null factory");
		}

		context.set(handler, () -> requireNonNull(factory.apply(context), "null handler"));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void init(final FilterConfig config) {

		final ServletContext context=config.getServletContext();

		try {

			this.context

					.set(Context.storage(), () -> storage(context))
					.set(Loader.loader(), () -> loader(context))

					.get(handler); // force handler loading during filter initialization

		} catch ( final Throwable t ) {

			try ( final StringWriter message=new StringWriter() ) {

				t.printStackTrace(new PrintWriter(message));

				this.context.get(logger()).error(this, "error during initialization: "+message);

				context.log("error during initialization", t);

				throw t;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				this.context.clear();

			}

		}
	}

	@Override public void destroy() {

		context.clear();

	}


	private Path storage(final ServletContext context) {
		return ((File)context.getAttribute(ServletContext.TEMPDIR)).toPath();
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

	@Override public void doFilter(
			final ServletRequest request, final ServletResponse response, final FilterChain chain
	) throws ServletException, IOException {

		try {

			context.exec(() -> context.get(handler)
					.handle(request((HttpServletRequest)request))
					.accept(_response -> response((HttpServletResponse)response, _response))
			);

			if ( !response.isCommitted() ) {
				chain.doFilter(request, response);
			}

		} catch ( final RuntimeException e ) {

			if ( !e.toString().toLowerCase(Locale.ROOT).contains("broken pipe") ) {
				context.get(logger()).error(this, "unhandled exception", e);
			}

		}

	}


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
		if ( response.status() > 0 ) { // unprocessed requests fall through the host container

			http.setStatus(response.status());

			response.headers().forEach((name, values) ->
					values.forEach(value -> http.addHeader(name, value))
			);

			response.body(output()).accept(e -> {}, target -> { // ignore missing response bodies
				try {

					target.accept(http.getOutputStream());

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});

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
