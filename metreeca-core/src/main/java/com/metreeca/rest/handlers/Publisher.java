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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.formats.DataFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.OutputFormat.output;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toMap;

/**
 * Static content publisher.
 */
public final class Publisher implements Handler {

	private static final Pattern URLPattern=Pattern.compile("(.*/)?(\\.|[^/#]*)?(#[^/#]*)?$");


	/**
	 * MIME types by file extension.
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">
	 * Common MIME types @ MDN</a>
	 */
	private static final Map<String, String> MIMETypes=unmodifiableMap(Stream

			.of(Publisher.class.getSimpleName()+".tsv")

			.map(Publisher.class::getResource)
			.map(function(URL::toURI))
			.map(Paths::get)
			.flatMap(function(path -> Files.readAllLines(path, UTF_8).stream()))

			.filter(line -> !line.isEmpty())

			.map(line -> {

				final int tab=line.indexOf('\t');

				return new AbstractMap.SimpleImmutableEntry<>(line.substring(0, tab), line.substring(tab+1));

			})

			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))

	);


	/**
	 * Extracts the basename of a path.
	 *
	 * @param path the path whose basename is to be extracted
	 *
	 * @return the portion of the {@code path} filename before the last dot character or the whole {@code  path}
	 * filename if it doesn't include a dot character
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static String basename(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final String name=path.getFileName().toString();
		final int dot=name.lastIndexOf('.');

		return dot >= 0 ? name.substring(0, dot) : name;
	}

	/**
	 * Extracts the extension of a path.
	 *
	 * @param path the path whose extension is to be extracted
	 *
	 * @return the portion of the {@code path} filename after the last dot character or an empty string if the filename
	 * doesn't include a dot character
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static String extension(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final String name=path.getFileName().toString();
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
	 * @param root the root URL of the content to be published (e.g. as returned by {@link Class#getResource(String)})
	 *
	 * @return a new static content publisher for the content under {@code root}
	 *
	 * @throws NullPointerException     if {@code root} is null
	 * @throws IllegalArgumentException if {@code root} is malformed
	 */
	public static Publisher publisher(final URL root) {
		try {

			return new Publisher(Paths.get(root.toURI()));

		} catch ( final URISyntaxException e ) {

			throw new IllegalArgumentException(e);

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
		return new Publisher(root);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Path root;


	private Publisher(final Path root) {this.root=root;}


	@Override public Future<Response> handle(final Request request) {
		return variants(request.path())

				.map(Paths::get)
				.map(Paths.get("/")::relativize)
				.map(root::resolve)
				.map(Path::normalize) // prevent tree walking attacks

				.filter(Files::exists)
				.filter(Files::isRegularFile)

				.findFirst()

				.map(file -> request.reply(function(response -> response.status(OK)

						.header("Content-Type", MIMETypes.getOrDefault(extension(file), DataFormat.MIME))
						.header("Content-Length", String.valueOf(Files.size(file)))
						.header("ETag", format("\"%s\"", Files.getLastModifiedTime(file).toMillis()))

						.body(output(), consumer(output -> Files.copy(file, output)))

				)))

				.orElseGet(() -> request.reply(status(NotFound)));
	}


	//// Unchecked Lambdas /////////////////////////////////////////////////////////////////////////////////////////////

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


	@FunctionalInterface private static interface CheckedConsumer<V> {

		public void accept(final V value) throws Exception;

	}

	@FunctionalInterface private static interface CheckedFunction<V, R> {

		public R apply(final V value) throws Exception;

	}

}
