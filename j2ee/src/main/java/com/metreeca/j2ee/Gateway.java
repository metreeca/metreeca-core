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

import com.metreeca.link.*;
import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.Loader;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;
import org.apache.commons.fileupload.FileUploadBase.IOFileUploadException;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileCleaningTracker;

import java.io.*;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.function.Supplier;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.metreeca.link.Part.part;
import static com.metreeca.spec.things.Strings.upper;
import static com.metreeca.tray.Tray.tool;

import static org.apache.commons.fileupload.servlet.ServletFileUpload.isMultipartContent;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.list;


/**
 * J2EE/Metreeca gateway.
 *
 * <p>Provides a gateway between a web application managed by Servlet 3.1 container and linked data resource handlers
 * based on the Metreeca {@linkplain com.metreeca.link linked data framework}:</p>
 *
 * <ul>
 *
 * <li>initializes and destroys the shared tool {@linkplain Tray tray} managing platform components required by
 * resource handlers;</li>
 *
 * <li>intercepts HTTP requests and handles them using the {@linkplain Server server} tool provided by the shared tool
 * tray;</li>
 *
 * <li>forwards HTTP requests to the enclosing web application if no response is {@linkplain Response#committed()
 * committed} by the linked data server.</li>
 *
 * </ul>
 */
@WebListener public final class Gateway implements ServletContextListener, Filter {

	private static final String TrayAttribute=Tray.class.getName();


	private static final Supplier<ServletFileUpload> Upload=ServletFileUpload::new; // shared file upload tool


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void contextInitialized(final ServletContextEvent event) {

		final ServletContext context=event.getServletContext();

		final Tray tray=new Tray();

		try {

			tray
					.set(Setup.Factory, () -> setup(context))
					.set(Loader.Factory, () -> loader(context))
					.set(Upload, () -> upload(context));

			for (final Toolkit toolkit : ServiceLoader.load(Toolkit.class)) { toolkit.load(tray); }

			tray.exec(() -> ServiceLoader.load(Service.class).forEach(Service::load));

			context.setAttribute(TrayAttribute, tray);

		} catch ( final Throwable t ) {

			try (final StringWriter message=new StringWriter()) {

				t.printStackTrace(new PrintWriter(message));

				tray.get(Trace.Factory).error(this, "error during data hub initialization: "+message);

				context.log("error during data hub initialization", t);

				throw t;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				tray.clear();

			}

		}
	}

	@Override public void contextDestroyed(final ServletContextEvent event) {

		final ServletContext context=event.getServletContext();

		try {

			final Tray tray=(Tray)context.getAttribute(TrayAttribute);

			if ( tray != null ) { tray.clear(); }

		} finally {
			context.removeAttribute(TrayAttribute);
		}
	}


	private Setup setup(final ServletContext context) {
		return new Setup(Setup::system, Setup::custom, setup -> {

			try { // defaults from WEB-INF directory, if found

				final Properties properties=new Properties();

				Optional.ofNullable(context.getResource("/WEB-INF/metreeca.properties")).ifPresent(url -> {

					try (final InputStream input=url.openStream()) {
						properties.load(input);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(format("unable to load default setup file [%s]", url), e);
					}

				});

				return properties;

			} catch ( final MalformedURLException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}

		});
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

	@Override public void init(final FilterConfig config) {}

	@Override public void destroy() {}


	@Override public void doFilter(
			final ServletRequest request, final ServletResponse response, final FilterChain chain
	) throws ServletException, IOException {

		final Tray tray=(Tray)request.getServletContext().getAttribute(TrayAttribute);

		if ( tray == null ) {
			throw new IllegalStateException("no tray in context");
		}

		tray.exec(() -> filter((HttpServletRequest)request, (HttpServletResponse)response));

		if ( !response.isCommitted() ) {
			chain.doFilter(request, response);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void filter(final HttpServletRequest request, final HttpServletResponse response) {
		try {

			try { // ;( request.getParts() is not available to filters…


				final List<FileItem> items=isMultipartContent(request) ? tool(Upload).parseRequest(request) : emptyList();

				tool(Server.Factory).handle(

						writer -> request(writer, request, items), reader -> response(reader, response)

				);

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

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	private void request(final Request.Writer writer, final HttpServletRequest request, final Collection<FileItem> items) {

		writer.method(upper(request.getMethod()));

		final String target=request.getRequestURL().toString();
		final String path=request.getRequestURI().substring(request.getContextPath().length());
		final String base=target.substring(0, target.length()-path.length()+1);
		final String query=request.getQueryString();

		writer.base(base);
		writer.path(path);
		writer.query(query != null ? Transputs.decode(query) : "");

		for (final Map.Entry<String, String[]> parameter : request.getParameterMap().entrySet()) {
			writer.parameter(parameter.getKey(), asList(parameter.getValue()));
		}

		for (final String name : list(request.getHeaderNames())) {
			writer.header(name, list(request.getHeaders(name)));
		}

		if ( items.isEmpty() ) {

			writer.body(

					() -> {
						try {
							return request.getInputStream();
						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					},

					() -> {
						try {
							return request.getReader();
						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					}

			);

		} else {

			final Map<String, List<String>> parameters=new LinkedHashMap<>();
			final Map<String, List<Part>> parts=new LinkedHashMap<>();

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

				} else { // accumulate parts

					parts.compute(item.getFieldName(), (key, current) -> {

						List<Part> updated=current;

						if ( updated == null ) {
							updated=new ArrayList<>();
						}

						final Part.Writer part=part().filename(item.getName());

						final FileItemHeaders headers=item.getHeaders();

						headers.getHeaderNames().forEachRemaining(name -> {

							final List<String> values=new ArrayList<>();

							headers.getHeaders(name).forEachRemaining(values::add);

							part.header(name, values);

						});

						updated.add(part.input(() -> {
							try {
								return item.getInputStream();
							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						}));

						return updated;

					});

				}

			}

			for (final Map.Entry<String, List<String>> parameter : parameters.entrySet()) {
				writer.parameter(parameter.getKey(), parameter.getValue());
			}

			for (final Map.Entry<String, List<Part>> part : parts.entrySet()) {
				writer.part(part.getKey(), part.getValue());
			}

			writer.body(); // empty body

		}
	}

	private void response(final Response.Reader reader, final HttpServletResponse response) {

		response.setStatus(reader.status());

		reader.headers().forEachOrdered(header ->
				header.getValue().forEach(value -> response.addHeader(header.getKey(), value)));

		if ( reader.binary() ) {

			try (final OutputStream output=response.getOutputStream()) {
				reader.output(output);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		} else if ( reader.textual() ) {

			try (final Writer writer=response.getWriter()) {
				reader.writer(writer);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		} else {

			try (final ServletOutputStream output=response.getOutputStream()) {
				output.flush();
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}

	}

}
