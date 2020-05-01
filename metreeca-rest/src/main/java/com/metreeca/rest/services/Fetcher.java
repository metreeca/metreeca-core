package com.metreeca.rest.services;

import com.metreeca.rest.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * Resource fetcher.
 *
 * <p>Fetches external resources through system-specific facilities.</p>
 */
@FunctionalInterface public interface Fetcher extends Function<Request, Response> {

	/**
	 * Retrieves the default fetcher factory.
	 *
	 * @return the default fetcher factory, which creates {@link URLFetcher} instances
	 */
	public static Supplier<Fetcher> fetcher() {
		return URLFetcher::new;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * URL resource fetcher.
	 *
	 * <p>Fetches external resources through the {@link URL#openConnection()} API.</p>>
	 */
	public static class URLFetcher implements Fetcher {

		private final Logger logger=service(logger());


		@Override public Response apply(final Request request) {
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
					connection.addRequestProperty("Accept", "text/html,"
							+"application/xhtml+xml,"
							+"application/xml;q=0.9;"
							+"*/*;q=0.1"
					);
				}

				if ( !request.header("User-Agent").isPresent() ) {
					connection.addRequestProperty("User-Agent", "metreeca/link HTTP client");
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

								logger.error(this, format("unable to open input stream for <%s>", item));

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

								logger.error(this, format("unable to open input stream for <%s>", item), e);

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
	public static final class CachingFetcher implements Fetcher { // !!! check caching headers

		private final Duration ttl=Duration.ZERO;

		private Cache cache=service(Cache.cache());
		private Fetcher delegate=service(fetcher());


		@Override public Response apply(final Request request) {
			return request.method().equals(GET) ? cache.retrieve(request.item()).fold(

					hit -> {
						try ( final InputStream input=hit.get() ) {

							return decode(input, request);

						} catch ( final IOException e ) {

							throw new UncheckedIOException(e);

						}
					},

					miss -> {

						final Response response=delegate.apply(request);

						if ( response.success() ) {
							try ( final OutputStream output=miss.get() ) {

								encode(output, response);

							} catch ( final IOException e ) {

								throw new UncheckedIOException(e);

							}
						}

						return response;

					}

			) : delegate.apply(request);
		}


		private void encode(final OutputStream output, final Response response) {
			response.body(data()).fold(

					value -> {

						try (
								final ObjectOutputStream serialized=new ObjectOutputStream(output);
						) {

							serialized.writeInt(response.status());
							serialized.writeObject(response.headers());
							serialized.writeObject(value);
							serialized.flush();

							return this;

						} catch ( final IOException e ) {

							throw new UncheckedIOException(e);

						}

					},

					error -> {

						throw new UncheckedIOException(new IOException(error.toString())); // !!! review

					}

			);
		}

		@SuppressWarnings("unchecked") private Response decode(final InputStream input, final Request request) {
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

}
