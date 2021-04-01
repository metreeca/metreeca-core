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

package com.metreeca.rest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Toolbox.input;
import static com.metreeca.rest.Xtream.entry;

import static java.lang.Float.parseFloat;
import static java.lang.Math.max;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.unmodifiableMap;
import static java.util.Locale.ROOT;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Message format {thread-safe}.
 *
 * <p>Decodes and encodes message bodies.</p>
 *
 * <p><strong>Warning</strong> / Concrete subclasses must be thread-safe.</p>
 *
 * @param <V> the type of the message body managed by the format
 */
public abstract class Format<V> {

	private static final String MIMEDefault="application/octet-stream";


	private static final Pattern QualityPattern=Pattern.compile("(?:\\s*;\\s*q\\s*=\\s*(\\d*(?:\\.\\d+)?))?");

	private static final Pattern MIMEPattern=Pattern.compile("((?:[-+\\w]+|\\*)/(?:[-+\\w]+|\\*))"+QualityPattern);
	private static final Pattern LangPattern=
			Pattern.compile("([a-zA-Z]{1,8}(?:-[a-zA-Z0-9]{1,8})*|\\*)"+QualityPattern);


	/**
	 * MIME types by file extension (including dot).
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types/Common_types">
	 * Common MIME types @ MDN</a>
	 */
	private static final Map<String, String> MIMETypes=unmodifiableMap(Stream

			.of(input(Format.class, ".tsv"))

			.flatMap(stream -> new BufferedReader(new InputStreamReader(stream, UTF_8)).lines())

			.filter(line -> !line.isEmpty())

			.map(line -> {

				final int tab=line.indexOf('\t');

				return entry(line.substring(0, tab), line.substring(tab+1));

			})

			.collect(toMap(Map.Entry::getKey, Map.Entry::getValue))

	);


	/**
	 * Guess the MIME type of a resource path.
	 *
	 * @param path the path of the resource whose MIME type is to be guessed
	 *
	 * @return the well-known MIME type associated with the extension of the {@code path} filename or
	 * {@value MIMEDefault}, if {@code path} doesn't include an extension or no well-known MIME type is defined
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public static String mime(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final int slash=max(0, path.lastIndexOf('/'));
		final int dot=(slash >= 0 ? path.substring(slash) : path).lastIndexOf('.');

		final String extension=dot >= 0 ? path.substring(slash+dot).toLowerCase(ROOT) : "";

		return MIMETypes.getOrDefault(extension, MIMEDefault);
	}


	/**
	 * Parses a MIME type list.
	 *
	 * @param types the MIME type list to be parsed
	 *
	 * @return a list of MIME types parsed from {@code types}, sorted by descending
	 * <a href="https://developer.mozilla.org/en-US/docs/Glossary/quality_values">quality value</a>
	 *
	 * @throws NullPointerException if {@code types} is null
	 */
	public static List<String> mimes(final CharSequence types) {

		if ( types == null ) {
			throw new NullPointerException("null types");
		}

		return values(types, MIMEPattern);
	}

	/**
	 * Parses a language tag list.
	 *
	 * @param langs the language tag list to be parsed
	 *
	 * @return a list of language tags parsed from {@code langs}, sorted by descending
	 * <a href="https://developer.mozilla.org/en-US/docs/Glossary/quality_values">quality value</a>
	 *
	 * @throws NullPointerException if {@code langs} is null
	 */
	public static List<String> langs(final CharSequence langs) {

		if ( langs == null ) {
			throw new NullPointerException("null langs");
		}

		return values(langs, LangPattern);
	}


	private static List<String> values(final CharSequence types, final Pattern pattern) {

		if ( types == null ) {
			throw new NullPointerException("null mime types");
		}

		final List<Map.Entry<String, Float>> entries=new ArrayList<>();

		final Matcher matcher=pattern.matcher(types);

		while ( matcher.find() ) {

			final String media=matcher.group(1).toLowerCase(ROOT);
			final String quality=matcher.group(2);

			try {
				entries.add(new SimpleImmutableEntry<>(media, quality == null ? 1 : parseFloat(quality)));
			} catch ( final NumberFormatException ignored ) {
				entries.add(new SimpleImmutableEntry<>(media, 0.0f));
			}
		}

		entries.sort((x, y) -> -Float.compare(x.getValue(), y.getValue()));

		return entries.stream().map(Map.Entry::getKey).collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes a message body.
	 *
	 * <p>The default implementation returns a {@linkplain MessageException#status() no-op message exception}.</p>
	 *
	 * <p>Concrete subclasses should report decoding issues using the following HTTP status codes:</p>
	 *
	 * <ul>
	 * <li>{@link Response#UnsupportedMediaType} for missing bodies;</li>
	 * <li>{@link Response#BadRequest} for malformed bodies, unless a more specific status code is available.</li>
	 * </ul>
	 *
	 * @param message the message whose body is to be decoded
	 *
	 * @return either a message exception reporting a decoding issue or the decoded {@code message} body
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public Either<MessageException, V> decode(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return Left(status());
	}

	/**
	 * Encodes a message body.
	 *
	 * <p>The default implementation has no effects.</p>
	 *
	 * @param message the message whose body is to be encoded
	 * @param value   the body being encoded into {@code message}
	 * @param <M>     the type of {@code message}
	 *
	 * @return the target {@code message} with the encoded {@code value} as body
	 *
	 * @throws NullPointerException if either {@code message} or {@code value} is null
	 */
	public <M extends Message<M>> M encode(final M message, final V value) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return message;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 *
	 * <p>All format objects in the same class are equal to each other.</p>
	 */
	@Override public final boolean equals(final Object object) {
		return this == object || object != null && getClass().equals(object.getClass());
	}

	@Override public final int hashCode() {
		return getClass().hashCode();
	}

}
