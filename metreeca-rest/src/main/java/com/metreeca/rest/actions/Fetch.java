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

package com.metreeca.rest.actions;


import com.metreeca.rest.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Cache;
import com.metreeca.rest.services.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;


public final class Fetch implements Function<Request, Optional<Response>> {

	private Cache cache=service(Cache.cache());

	private final Logger logger=service(logger());


	public Fetch cache(final Cache cache) {

		if ( cache == null ) {
			throw new NullPointerException("null cache");
		}

		this.cache=cache;

		return this;
	}

	public Fetch limit(final Limit<Request> limit) {

		if ( limit == null ) {
			throw new NullPointerException("null limit");
		}

		return this;
	}


	@Override public Optional<Response> apply(final Request request) {
		return Optional.ofNullable(request)

				.map(_request -> _request.method().equals(GET) ? cache(_request) : fetch(_request))

				.filter(response -> {

					final boolean success=response.success();

					if ( !success ) {

						logger.error(this, format("unable to retrieve data from <%s> : status %d (%s)",
								response.item(), response.status(), response.body(text()).value().orElse("")
						));

					}

					return success;

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Response cache(final Request request) {
		if ( cache instanceof Cache.None ) { return fetch(request); } else {

			try ( final InputStream input=cache.retrieve(request.item(), () -> fetch(request).map(this::encode)) ) {

				return decode(request, input);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}

		}
	}

	private Response fetch(final Request request) {
		try {

			final String method=request.method();
			final String item=request.item();

			logger.info(this, format("fetching <%s>", item));

			final HttpURLConnection connection=(HttpURLConnection)new URL(item).openConnection();

			connection.setRequestMethod(method);
			connection.setDoOutput(method.equals(Request.POST) || method.equals(Request.PUT));

			connection.setInstanceFollowRedirects(true);

			// !!! connection.setConnectTimeout();
			// !!! connection.setReadTimeout();
			// !!! connection.setIfModifiedSince();

			if ( !request.header("Accept").isPresent() ) {
				connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9;*/*;q=0"
						+".1");
			}

			if ( !request.header("User-Agent").isPresent() ) {
				connection.addRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_2)"
						+" AppleWebKit/537.36 (KHTML, like Gecko)"
						+" Chrome/79.0.3945.130"
						+" Safari/537.36"
				);
			}

			request.headers().forEach((name, values) -> values.forEach(value ->
					connection.addRequestProperty(name, value)
			));

			connection.connect();

			if ( connection.getDoOutput() ) {

				request.body(input()).fold(

						target -> {

							try ( final InputStream input=target.get() ) {

								return Codecs.data(connection.getOutputStream(), input);

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}

						},

						error -> {

							logger.error(this, format("unable to open input stream for <%s>",
									item
							));

							throw new RuntimeException(error.toString()); // !!!

						}

				);

			}

			final int code=connection.getResponseCode(); // !!! handle http > https redirection

			return new Response(request)

					.status(code)

					.headers(connection.getHeaderFields().entrySet().stream()
							.filter(entry -> entry.getKey() != null) // ;( may use null to hold status line
							.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
					)

					.body(input(), () -> {
						try {

							return Optional
									.ofNullable(code/100 == 2 ? connection.getInputStream() :
											connection.getErrorStream())
									.orElseGet(Codecs::input);

						} catch ( final IOException e ) {

							logger.error(this, format("unable to open input stream for <%s>",
									item
							), e);

							throw new UncheckedIOException(e);

						}
					});

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private InputStream encode(final Response response) {
		return response.body(data()).fold(

				value -> {

					try (
							final ByteArrayOutputStream output=new ByteArrayOutputStream(1000);
							final ObjectOutputStream serialized=new ObjectOutputStream(output);
					) {

						serialized.writeInt(response.status());
						serialized.writeObject(response.headers());
						serialized.writeObject(value);
						serialized.flush();

						final InputStream input=new ByteArrayInputStream(output.toByteArray());

						return response.success() ? input : new Cache.TransientInputStream(input);

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

				},

				error -> {

					throw new UncheckedIOException(new IOException(error.toString())); // !!! review

				}

		);

	}

	private Response decode(final Request request, final InputStream input) {

		try ( final ObjectInputStream serialized=new ObjectInputStream(input) ) {

			final int status=serialized.readInt();
			final Map<String, ? extends Collection<String>> headers=
					(Map<String, ? extends Collection<String>>)serialized.readObject();
			final byte[] body=(byte[])serialized.readObject();

			return new Response(request)
					.status(status)
					.headers(headers)
					.body(input(), () -> new ByteArrayInputStream(body));

		} catch ( final ClassNotFoundException unexpected ) {

			throw new RuntimeException(unexpected);

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}

	}

}
