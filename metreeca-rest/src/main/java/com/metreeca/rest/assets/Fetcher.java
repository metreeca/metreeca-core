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

package com.metreeca.rest.assets;

import com.metreeca.rest.*;
import com.metreeca.rest.formats.DataFormat;
import com.metreeca.rest.formats.InputFormat;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;

import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Logger.logger;
import static java.lang.Integer.max;
import static java.lang.Integer.min;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * Resource fetcher.
 *
 * <p>Fetches external resources through system-specific facilities.</p>
 */
@FunctionalInterface public interface Fetcher extends Function<Request, Response> {

	/**
	 * Retrieves the default resource fetcher factory.
	 *
	 * @return the default resource fetcher factory, which creates {@link URLFetcher} instances
	 */
	public static Supplier<Fetcher> fetcher() {
		return URLFetcher::new;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * URL resource fetcher.
	 *
	 * <p>Fetches external resources through the {@link URL#openConnection()} API.</p>
	 */
	public static class URLFetcher implements Fetcher {

		private final Logger logger=asset(logger());


		@Override public Response apply(final Request request) {
			try {

				final String method=request.method();
				final String item=request.item();

				logger.info(this, format("%s %s", method, item));

				final HttpURLConnection connection=(HttpURLConnection)new URL(item).openConnection();

				connection.setRequestMethod(method);
				connection.setDoOutput(method.equals(Request.POST) || method.equals(Request.PUT));

				connection.setInstanceFollowRedirects(true);

				// !!! connection.setConnectTimeout();
				// !!! connection.setReadTimeout();
				// !!! connection.setIfModifiedSince();

				if ( !request.header("User-Agent").isPresent() ) {
					connection.addRequestProperty("User-Agent", "Metreeca/Link (https://github.com/metreeca/link)");
				}

				if ( !request.header("Accept-Encoding").isPresent() ) {
					connection.addRequestProperty("Accept-Encoding", "gzip");
				}

				if ( !request.header("Accept").isPresent() ) {
					connection.addRequestProperty("Accept", "text/html,"
							+"application/xhtml+xml,"
							+"application/xml;q=0.9;"
							+"*/*;q=0.1"
					);
				}

				request.headers().forEach((name, values) -> values.forEach(value ->
						connection.addRequestProperty(name, value)
				));

				connection.connect();

				if ( connection.getDoOutput() ) {

					request.body(InputFormat.input()).fold(

							error -> {

								logger.error(this, format("unable to open input stream for <%s>", item));

								throw new RuntimeException(error.toString()); // !!!

							}, target -> {

								try (
										final InputStream input=target.get();
										final OutputStream output=connection.getOutputStream()
								) {

									return Xtream.copy(output, input);

								} catch ( final IOException e ) {

									throw new UncheckedIOException(e);

								}

							}

					);

				}

				final boolean head=connection.getRequestMethod().equalsIgnoreCase(Request.HEAD);
				final int code=connection.getResponseCode(); // !!! handle http > https redirection
				final String encoding=connection.getContentEncoding();

				return new Response(request)

						.status(min(max(100, code), 599)) // harden against illegal codes

						.headers(connection.getHeaderFields().entrySet().stream()
								.filter(entry -> entry.getKey() != null) // ;( may use null to hold status line
								.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
						)

						.body(InputFormat.input(), () -> {
							try {

								return Optional

										.ofNullable(head ? null
												: code/100 == 2 ? connection.getInputStream()
												: connection.getErrorStream()
										)

										.map(input -> {
											try {

												return "gzip".equals(encoding)
														? new GZIPInputStream(input)
														: input;

											} catch ( final IOException e ) {
												throw new UncheckedIOException(e);
											}
										})

										.orElseGet(Xtream::input);

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						});

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}

	}

	/**
	 * Caching resource fetcher.
	 *
	 * <p>Caches resources fetched by a delegate fetcher.</p>
	 */
	public static final class CacheFetcher implements Fetcher { // !!! check caching headers

		private Fetcher delegate=asset(fetcher(), fetcher());
		private Cache cache=asset(Cache.cache());


		/**
		 * Configures the delegate for this fetcher (defaults to the {@linkplain #fetcher() shared fetcher service}).
		 *
		 * @param delegate the delegate for this fetcher
		 *
		 * @return this fetcher
		 *
		 * @throws NullPointerException if {@code delegate} is null
		 */
		public CacheFetcher delegate(final Fetcher delegate) {

			if ( delegate == null ) {
				throw new NullPointerException("null delegate");
			}

			this.delegate=delegate;

			return this;
		}

		/**
		 * Configures the cache for this fetcher (defaults to the {@link Cache#cache() shared cache}).
		 *
		 * @param cache the cache for this fetcher
		 *
		 * @return this fetcher
		 *
		 * @throws NullPointerException if {@code cache} is null
		 */
		public CacheFetcher cache(final Cache cache) {

			if ( cache == null ) {
				throw new NullPointerException("null cache");
			}

			this.cache=cache;

			return this;
		}


		@Override public Response apply(final Request request) {
			return request.safe() ? cache.retrieve(

					format("%s %s", request.method(), request.item()),

					input -> decode(request, input),
					output -> encode(delegate.apply(request), output)

			) : delegate.apply(request);
		}


		private Response encode(final Response response, final OutputStream output) {
			return response.success() ? response.body(DataFormat.data()).fold(

					error -> {

						throw new UncheckedIOException(new IOException(error.toString())); // !!! review

					}, value -> {

						try (
								final ObjectOutputStream serialized=new ObjectOutputStream(output);
						) {

							serialized.writeInt(response.status());
							serialized.writeObject(response.headers());
							serialized.writeObject(value);
							serialized.flush();

							return response.body(InputFormat.input(), () -> new ByteArrayInputStream(value));

						} catch ( final IOException e ) {

							throw new UncheckedIOException(e);

						}

					}

			) : response;
		}

		@SuppressWarnings("unchecked") private Response decode(final Request request, final InputStream input) {
			try ( final ObjectInputStream serialized=new ObjectInputStream(input) ) {

				final int status=serialized.readInt();
				final Map<String, ? extends Collection<String>> headers=
						(Map<String, ? extends Collection<String>>)serialized.readObject();
				final byte[] body=(byte[])serialized.readObject();

				return new Response(request)
						.status(status)
						.headers(headers)
						.body(InputFormat.input(), () -> new ByteArrayInputStream(body));

			} catch ( final ClassNotFoundException unexpected ) {

				throw new RuntimeException(unexpected);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		}

	}

}
