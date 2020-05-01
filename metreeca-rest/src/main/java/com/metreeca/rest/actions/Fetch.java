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


import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Cache;
import com.metreeca.rest.services.Fetcher;
import com.metreeca.rest.services.Logger;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Fetcher.fetcher;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;


public final class Fetch implements Function<Request, Optional<Response>> {

	private Cache cache=service(Cache.cache());

	private final Fetcher fetcher=service(fetcher());
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
		return fetcher.apply(request);
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
