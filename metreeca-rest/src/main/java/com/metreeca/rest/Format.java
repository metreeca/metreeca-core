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

package com.metreeca.rest;

import com.metreeca.tree.Shape;

import javax.json.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static com.metreeca.rest.Request.status;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.tree.shapes.Field.fields;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * Message body format {thread-safe}.
 *
 * <p>Manages the conversion between message body representations.</p>
 *
 * <p><strong>Warning</strong> / Implementations must be thread-safe.</p>
 *
 * @param <V> the type of the message body representation managed by the body format
 */
public abstract class Format<V> {

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a guarded function.
	 *
	 * @param function the function to be wrapped
	 * @param <V>      the type of the {@code function} input value
	 * @param <R>      the type of the {@code function} return value
	 *
	 * @return a function that returns the value produced by applying {@code function} to its input value, if it is
	 * not null and no exception is thrown in the process, or {@code null}, otherwise
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V, R> Function<V, R> guarded(final Function<? super V, ? extends R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return v -> {
			try {

				return v == null ? null : function.apply(v);

			} catch ( final RuntimeException e ) {

				return null;

			}
		};

	}

	/**
	 * Creates an autoclosing function.
	 *
	 * @param function the function to be wrapped
	 * @param <V>      the type of the {@code function} input value
	 * @param <R>      the type of the {@code function} return value
	 *
	 * @return a function that returns the value produced by applying {@code function} to its input value, closing
	 * it after processing
	 *
	 * @throws NullPointerException if {@code function} is null
	 */
	public static <V extends AutoCloseable, R> Function<V, R> closing(final Function<? super V, ? extends R> function) {

		if ( function == null ) {
			throw new NullPointerException("null function");
		}

		return v -> {

			try ( final V c=v ) {

				return function.apply(c);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} catch ( final Exception e ) {

				throw new RuntimeException(e);

			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a body representation from a message.
	 *
	 * <p>Processing failure should be reported using the following HTTP status codes:</p>
	 *
	 * <ul>
	 * <li>{@link Response#UnsupportedMediaType} for missing bodies;</li>
	 * <li>{@link Response#BadRequest} for malformed bodies, unless a more specific status code is available.</li>
	 * </ul>
	 *
	 * <p>The default implementation returns a failure reporting the {@link Response#UnsupportedMediaType} status,
	 * unless explicitly {@linkplain Message#body(Format, Object) overridden}.</p>
	 *
	 * @param message the message whose body representation managed by this body format is to be retrieved
	 *
	 * @return a value providing access to the body representation managed by this body format, if it was possible to
	 * derive one from {@code message}; an error providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public Result<V, UnaryOperator<Response>> get(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return Error(status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures a message to hold a body representation.
	 *
	 * <p>The default implementation has no effects.</p>
	 *
	 * @param message the message to be configured to hold a body representation managed by this body format
	 * @param value   the body representation being configured for {@code message} by this body format
	 * @param <M>     the type of {@code message}
	 *
	 * @return the configured {@code message}
	 *
	 * @throws NullPointerException if either {@code message} or {@code value} is null
	 */
	public <M extends Message<M>> M set(final M message, final V value) {

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
	 * <p>By all formats in the same class are equal to each other.</p>
	 */
	@Override public boolean equals(final Object object) {
		return this == object || object != null && getClass().equals(object.getClass());
	}

	@Override public int hashCode() {
		return getClass().hashCode();
	}

}
