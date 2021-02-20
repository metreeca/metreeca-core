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

import com.metreeca.rest.Handler;
import com.metreeca.rest.assets.Loader;
import com.metreeca.rest.formats.DataFormat;

import java.io.*;
import java.net.*;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Optional;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.assets.Loader.loader;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.handlers.Router.router;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toMap;

/**
 * Static content publisher.
 */
public final class Publisher extends Delegator {

	private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");


	/**
	 * MIME types by file extension.
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">
	 * Common MIME types @ MDN</a>
	 */
	private static final Map<String, String> MIMETypes=unmodifiableMap(Stream

			.of(Publisher.class.getSimpleName()+".tsv")

			.map(Publisher.class::getResourceAsStream)
			.flatMap(stream -> new BufferedReader(new InputStreamReader(stream, UTF_8)).lines())

			.filter(line -> !line.isEmpty())

			.map(line -> {

				final int tab=line.indexOf('\t');

				return new SimpleImmutableEntry<>(line.substring(0, tab), line.substring(tab+1));

			})

			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))

	);


	private static String mime(final String extension) {
		return MIMETypes.getOrDefault(extension, DataFormat.MIME);
	}


	private static String filename(final String path) {

		final int slash=path.lastIndexOf('/');

		return slash >= 0 ? path.substring(slash+1) : path;
	}

	private static String extension(final String name) {

		final int dot=name.lastIndexOf('.');

		return dot >= 0 ? name.substring(dot).toLowerCase(ROOT) : "";
	}


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

			// load the filesystem from the asset manager to have it automatically closed
			// !!! won't handle multiple publishers from the same filesystem

			final FileSystem filesystem=asset(supplier(() ->
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


	/**
	 * Creates a static fallback content publisher.
	 *
	 * @param path the path of the {@linkplain Loader shared resource} to be published as fallback content
	 *
	 * @return a new static content publisher unconditionally serving the content of the shared read from {@code path}
	 * using the default {@linkplain Loader loader}.
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static Handler publisher(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final byte[] data=asset(loader())
				.load(path)
				.map(DataFormat::data)
				.orElseThrow(() -> new RuntimeException(format("missing <%s> path", path)));

		final String mime=mime(extension(filename(path)));
		final String length=String.valueOf(data.length);

		return request -> request.reply(response -> response
				.status(OK)
				.header("Content-Type", mime)
				.header("Content-Length", length)
				.body(data(), data)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Publisher(final Path root) {
		delegate(router().get(request -> variants(request.path())

				.map(path -> root.getRoot().relativize(root.getFileSystem().getPath(path)))
				.map(root::resolve)
				.map(Path::normalize) // prevent tree walking attacks

				.filter(Files::exists)
				.filter(Files::isRegularFile)

				.findFirst()

				.map(file -> request.reply(function(response -> response.status(OK)

						.header("Content-Type", mime(extension(file.getFileName().toString())))
						.header("Content-Length", String.valueOf(Files.size(file)))
						.header("ETag", format("\"%s\"", Files.getLastModifiedTime(file).toMillis()))

						.body(output(), consumer(output -> Files.copy(file, output)))

				)))

				.orElseGet(() -> request.reply(status(NotFound)))

		));
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
