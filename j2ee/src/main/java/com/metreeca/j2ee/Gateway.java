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

package com.metreeca.j2ee;

import com.metreeca.form.things.Transputs;
import com.metreeca.next.*;
import com.metreeca.next.formats.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.Loader;
import com.metreeca.tray.sys.Trace;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;

import java.io.*;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.metreeca.tray.Tray.tool;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;


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

	private static final Supplier<ServletFileUpload> Upload=ServletFileUpload::new; // shared file upload tool


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Tray tray=new Tray();

	private final String pattern;
	private final Function<Tray, Handler> loader;


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

			context.addFilter(Gateway.class.getName(), new GatewayFilter(loader.apply(tray // !!! @@@ add server handlers

					// !!! file storage location
					.set(Loader.Factory, () -> loader(context))
					.set(Upload, () -> upload(context))))

			).addMappingForUrlPatterns(null, false, pattern);

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


	private Loader loader(final ServletContext context) {
		return path -> {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			return Optional.ofNullable(context.getResourceAsStream(path));

		};
	}

	private ServletFileUpload upload(final ServletContext context) {

		final DiskFileItemFactory factory=new DiskFileItemFactory();
		final FileCleaningTracker tracker=new FileCleaningTracker();

		factory.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);

		factory.setRepository((File)context.getAttribute("javax.servlet.context.tempdir"));
		factory.setFileCleaningTracker(tracker);

		final ServletFileUpload upload=new ServletFileUpload(factory);

		upload.setSizeMax(10_000_000L);
		upload.setFileSizeMax(10_000_000L);

		tool(() -> (AutoCloseable)tracker::exitWhenFinished); // register cleanup hook

		return upload;
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

			if ( !filter((HttpServletRequest)request, (HttpServletResponse)response) ) {
				chain.doFilter(request, response);
			}

		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private boolean filter(final HttpServletRequest request, final HttpServletResponse response) {
			try {

				try { // ;( request.getParts() is not available to filters…

					final List<FileItem> items=isMultipartContent(request) ? tool(Upload).parseRequest(request) : emptyList();

					handler.handle(request(request, items)).accept(r -> response(response, r));

				} catch ( final IOFileUploadException e ) {

					final Throwable cause=e.getCause();

					if ( cause instanceof SocketTimeoutException ) {
						response.sendError(HttpServletResponse.SC_REQUEST_TIMEOUT);
					} else {
						throw new UncheckedIOException((IOException)cause);
					}

				} catch ( final FileUploadException e ) {

					request.getServletContext().log(e.getMessage(), e);

					response.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());

				}

				return response.isCommitted();

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		}


		private Request request(final HttpServletRequest http, final Collection<FileItem> items) {

			final String target=http.getRequestURL().toString();
			final String path=http.getRequestURI().substring(http.getContextPath().length());
			final String base=target.substring(0, target.length()-path.length()+1);
			final String query=http.getQueryString();

			final Request request=new Request()
					.method(http.getMethod())
					.base(base)
					.path(path)
					.query(query != null ? Transputs.decode(query) : ""); // !!! review decoding

			for (final Map.Entry<String, String[]> parameter : http.getParameterMap().entrySet()) {
				request.parameters(parameter.getKey(), asList(parameter.getValue()));
			}

			for (final String name : list(http.getHeaderNames())) {
				request.headers(name, list(http.getHeaders(name)));
			}

			if ( items.isEmpty() ) {

				return request

						.body(_Input.Format, () -> {
							try {
								return http.getInputStream();
							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						})

						.body(_Reader.Format, () -> {
							try {
								return http.getReader();
							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						});

			} else {

				final Map<String, List<String>> parameters=new LinkedHashMap<>();
				final Map<String, List<Message<?>>> parts=new LinkedHashMap<>();

				for (final FileItem item : items) {

					if ( item.isFormField() ) { // accumulate parameters

						parameters.compute(item.getFieldName(), (key, current) -> {

							List<String> updated=current;

							if ( updated == null ) {
								updated=new ArrayList<>();
							}

							try {
								updated.add(item.getString(Transputs.UTF8.name()));
							} catch ( final UnsupportedEncodingException unexpected ) {
								throw new UncheckedIOException(unexpected);
							}

							return updated;

						});

					} else { // accumulate items

						parts.compute(item.getFieldName(), (key, current) -> {

							List<Message<?>> updated=current;

							if ( updated == null ) {
								updated=new ArrayList<>();
							}

							final Message<?> part=new Message<Message>() {
								@Override protected Message self() { return this; }
							}; // !!! .filename(item.getName());

							final FileItemHeaders headers=item.getHeaders();

							headers.getHeaderNames().forEachRemaining(name -> {

								final List<String> values=new ArrayList<>();

								headers.getHeaders(name).forEachRemaining(values::add);

								part.headers(name, values);

							});

							updated.add(part

									.body(_Input.Format, () -> {
										try {
											return item.getInputStream();
										} catch ( final IOException e ) {
											throw new UncheckedIOException(e);
										}
									})

									.body(_Reader.Format, () -> { // !!! from input using part/request encoding
										throw new UnsupportedOperationException("to be implemented"); // !!! tbi
									}));

							return updated;

						});

					}

				}

				for (final Map.Entry<String, List<String>> parameter : parameters.entrySet()) {
					request.parameters(parameter.getKey(), parameter.getValue());
				}

				for (final Map.Entry<String, List<Message<?>>> part : parts.entrySet()) {
					// !!! request.body(_Parts,Format, ___)// !!! request.part(part.getKey(), part.getValue());
				}

				// !!! set input/reader format from main body part

				return request;

			}
		}

		private void response(final HttpServletResponse http, final Response response) {
			if ( response.status() != 0 ) {

				http.setStatus(response.status());

				response.headers().forEach((name, values) -> values.forEach(value -> http.addHeader(name, value)));

				response.body(_Output.Format).value().ifPresent(consumer -> {
					try (final OutputStream output=http.getOutputStream()) {
						consumer.accept(output);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});

				response.body(_Writer.Format).value().ifPresent(consumer -> {
					try (final Writer writer=http.getWriter()) {
						consumer.accept(writer);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				});

				// !!! @@@ no body: commit???

				//try (final ServletOutputStream output=http.getOutputStream()) {
				//	output.flush();
				//} catch ( final IOException e ) {
				//	throw new UncheckedIOException(e);
				//}
			}
		}

	}

}
