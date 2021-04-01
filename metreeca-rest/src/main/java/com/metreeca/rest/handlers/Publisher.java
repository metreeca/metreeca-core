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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Request.HEAD;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.handlers.Router.router;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;

/**
 * Static content publisher.
 */
public final class Publisher extends Delegator {

	private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");


	/**
	 * Computes HTML variants of a URL.
	 *
	 * <p>Generate alternative paths appending {@code .html}/{index.html} suffixes as suggested by the path, e.g.:</p>
	 *
	 * <ul>
	 *
	 *     <li>{@code …/path/} ›› {@code …/path/}, {@code …/path/index.html}</li>
	 *     <li>{@code …/path/file} ›› {@code …/path/file}, {@code …/path/file.html}</li>
	 *
	 * </ul>
	 *
	 * <p>URL anchors ({@code #<anchors>} are properly preserved.</p>
	 *
	 * @param url the url whose variants are to be computed
	 *
	 * @return a stream of url variants
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public static Stream<String> variants(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		final Matcher matcher=URLPattern.matcher(url);

		if ( matcher.matches() ) {

			final String head=Optional.ofNullable(matcher.group(1)).orElse("");
			final String file=Optional.ofNullable(matcher.group(2)).orElse("");
			final String hash=Optional.ofNullable(matcher.group(3)).orElse("");

			return file.isEmpty() || file.equals(".") ? Stream.of(head+"index.html"+hash)
					: file.endsWith(".html") ? Stream.of(head+file+hash)
					: Stream.of(head+file+hash, head+file+".html"+hash);

		} else {

			return Stream.of(url);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a static content publisher.
	 *
	 * <p><strong>Warning</strong> / Only {@code file:} and {@code jar:} URLs are currently supported.</p>
	 *
	 * @param root the root URL of the content to be published (e.g. as returned by {@link Class#getResource(String)})
	 *
	 * @return a new static content publisher for the content under {@code root}
	 *
	 * @throws NullPointerException     if {@code root} is null
	 * @throws IllegalArgumentException if {@code root} is malformed
	 */
	public static Publisher publisher(final URL root) {

		if ( root == null ) {
			throw new NullPointerException("null root");
		}

		final String scheme=root.getProtocol();

		if ( scheme.equals("file") ) {

			try {

				return new Publisher(Paths.get(root.toURI()));

			} catch ( final URISyntaxException e ) {

				throw new IllegalArgumentException(e);

			}

		} else if ( scheme.equals("jar") ) {

			final String path=root.toString();
			final int separator=path.indexOf("!/");

			final String jar=path.substring(0, separator);
			final String entry=path.substring(separator+1);

			// load the filesystem from the service toolbox to have it automatically closed
			// !!! won't handle multiple publishers from the same filesystem

			final FileSystem filesystem=service(supplier(() ->
					FileSystems.newFileSystem(URI.create(jar), emptyMap())
			));

			return new Publisher(filesystem.getPath(entry));

		} else {

			throw new UnsupportedOperationException(format("unsupported URL scheme <%s>", root));

		}
	}

	/**
	 * Creates a static content publisher.
	 *
	 * @param root the root path of the content to be published
	 *
	 * @return a new static content publisher for the content under {@code root}
	 *
	 * @throws NullPointerException if {@code root} is null
	 */
	public static Publisher publisher(final Path root) {

		if ( root == null ) {
			throw new NullPointerException("null root");
		}

		return new Publisher(root);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String fallback="";


	private Publisher(final Path root) {
		delegate(router()

				.head(request -> handle(request, root))
				.get(request -> handle(request, root))

		);
	}


	/**
	 * Configures the fallback path.
	 *
	 * @param path the path of the resource to be served for {@linkplain Request#route() route} requests that don't
	 *                match
	 *             any available resource path; ignored if empty
	 *
	 * @return this publisher
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public Publisher fallback(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		this.fallback=path;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> handle(final Request request, final Path root) {
		return reply(request, root, request.path())

				.orElseGet(() -> Optional.of(fallback)

						.filter(path -> !path.isEmpty() && request.route())

						.flatMap(path -> reply(request, root, path))

						.orElseGet(() -> request.reply(status(NotFound)))

				);
	}

	private Optional<Future<Response>> reply(final Request request, final Path root, final String path) {
		return variants(path)

				.map(variant -> root.getRoot().relativize(root.getFileSystem().getPath(variant)))
				.map(root::resolve)
				.map(Path::normalize) // prevent tree walking attacks

				.filter(Files::exists)
				.filter(Files::isRegularFile)

				.findFirst()

				.map(file -> request.reply(function(response -> {

					final String mime=Format.mime(file.getFileName().toString());
					final String length=String.valueOf(Files.size(file));
					final String etag=format("\"%s\"", Files.getLastModifiedTime(file).toMillis());

					return request.headers("If-None-Match").stream().anyMatch(etag::equals)

							? response.status(NotModified)

							: request.method().equals(HEAD)

							? response.status(OK)
							.header("Content-Type", mime)
							.header("ETag", etag)

							: response.status(OK)
							.header("Content-Type", mime)
							.header("Content-Length", length)
							.header("ETag", etag)
							.body(output(), consumer(output -> Files.copy(file, output)));

				})));
	}


	//// Unchecked Lambdas /////////////////////////////////////////////////////////////////////////////////////////////

	private static <V> Supplier<V> supplier(final CheckedSupplier<? extends V> supplier) {
		return () -> {
			try {

				return supplier.get();

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final Exception e ) {

				throw new RuntimeException(e);
			}
		};
	}

	private static <V> Consumer<V> consumer(final CheckedConsumer<? super V> consumer) {
		return v -> {
			try {

				consumer.accept(v);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final Exception e ) {

				throw new RuntimeException(e);
			}
		};
	}

	private static <V, R> Function<V, R> function(final CheckedFunction<? super V, ? extends R> function) {
		return v -> {
			try {

				return function.apply(v);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final Exception e ) {

				throw new RuntimeException(e);
			}
		};
	}


	@FunctionalInterface private static interface CheckedSupplier<V> {

		public V get() throws Exception;

	}

	@FunctionalInterface private static interface CheckedConsumer<V> {

		public void accept(final V value) throws Exception;

	}

	@FunctionalInterface private static interface CheckedFunction<V, R> {

		public R apply(final V value) throws Exception;

	}

}
