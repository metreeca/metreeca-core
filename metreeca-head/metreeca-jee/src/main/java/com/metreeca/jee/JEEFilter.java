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

package com.metreeca.jee;

import com.metreeca.core.*;
import com.metreeca.core.assets.Loader;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.metreeca.core.assets.Logger.logger;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;
import static java.util.Arrays.asList;
import static java.util.Collections.list;
import static java.util.Objects.requireNonNull;


/**
 * Java EE Servlet adapter.
 *
 * <p>Adapts web applications managed by Servlet 3.1 container and resource handlers based on the Metreeca/Link
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
public abstract class JEEFilter implements Filter {

	private final Context context=new Context();

	private final Supplier<Handler> handler=() -> requireNonNull(load(context), "null resource handler");


	/*
	 * Creates the main handler.
	 *
	 * @param context the shared asset context; may be configured with additional application-specific assets
	 *
	 * @return a non-null resource handler to be used as main entry point for serving requests
	 */
	protected abstract Handler load(final Context context);


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

		context.exec(() -> context.get(handler)
				.handle(request((HttpServletRequest)request))
				.accept(_response -> response((HttpServletResponse)response, _response))
		);

		if ( !response.isCommitted() ) {
			chain.doFilter(request, response);
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
		if ( response.status() > 0 ) { // only if actually processed

			http.setStatus(response.status());

			response.headers().forEach((name, values) ->
					values.forEach(value -> http.addHeader(name, value))
			);

			// ignore missing response bodies

			response.body(output()).accept(e -> {}, target -> {
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
