package com.metreeca.rest.services;

import com.metreeca.rest.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;
import static java.util.stream.Collectors.toMap;

/**
 * System fetcher.
 *
 * <p>Fetches external resources through system-specific facilities.</p>
 */
@FunctionalInterface public interface Fetcher extends Function<Request, Response> {

	/**
	 * Retrieves the default fetcher factory.
	 *
	 * @return the default fetcher factory, which fetches external resources through {@link URL#openConnection()}
	 */
	public static Supplier<Fetcher> fetcher() {
		return () -> new Fetcher() {

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
						connection.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0"
								+ ".9;"
								+"*/*;q=0"
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
		};

	}

}
