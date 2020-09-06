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

package com.metreeca.core;

import com.metreeca.json.Shape;

import javax.json.*;
import java.util.*;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.json.shapes.Field.fields;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Message format {thread-safe}.
 *
 * <p>Decodes and encodes message bodies.</p>
 *
 * <p><strong>Warning</strong> / Concrete subclasses must be thread-safe.</p>
 *
 * @param <V> the type of the message body managed by the body format
 */
public abstract class Format<V> {

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
	 * <p>All formats in the same class are equal to each other.</p>
	 */
	@Override public boolean equals(final Object object) {
		return this == object || object != null && getClass().equals(object.getClass());
	}

	@Override public int hashCode() {
		return getClass().hashCode();
	}


	//// !!! //////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a JSON {@linkplain Request#query(Format, Shape) query} field path.
	 *
	 * <p>The default implementation splits paths at dots without further well-formedness checks.</p>
	 *
	 * @param base  the base IRI for resolving relative references
	 * @param shape the base shape for the query
	 * @param path  the query field path to be parsed; may be empty
	 *
	 * @return a list of format-specific path steps parsed from {@code path}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public List<?> path(final String base, final Shape shape, final String path) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return asList((Object[])path.split("\\."));
	}

	/**
	 * Parses a JSON {@linkplain Request#query(Format, Shape) query} field value.
	 *
	 * <p>The default implementation converts JSON values to their native Java representation, using lists of objects
	 * for arrays and maps from strings to objects for objects.</p>
	 *
	 * @param base  the base IRI for resolving relative references
	 * @param shape the base shape for the query
	 * @param value the query value to be parsed
	 *
	 * @return a format-specific value parsed from {@code value}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public Object value(final String base, final Shape shape, final JsonValue value) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return value.equals(JsonValue.TRUE) ? true
				: value.equals(JsonValue.FALSE) ? false
				: value instanceof JsonNumber && ((JsonNumber)value).isIntegral() ? ((JsonNumber)value).longValue()
				: value instanceof JsonNumber ? ((JsonNumber)value).doubleValue()
				: value instanceof JsonString ? ((JsonString)value).getString()
				: value instanceof JsonArray ?
				((JsonArray)value).stream().map(v -> value(base, shape, v)).collect(toList())
				: value instanceof JsonObject ? ((JsonObject)value).entrySet().stream().collect(toMap(Map.Entry::getKey
				, e -> Optional
						.ofNullable(fields(shape).get(e.getKey()))
						.map(nested -> value(base, nested, e.getValue()))
						.orElseThrow(() -> new JsonException("unknown field {"+e.getKey()+"}"))
		))
				: null;
	}

}
