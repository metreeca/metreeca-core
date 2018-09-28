/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Strings.title;
import static com.metreeca.rest.Result.value;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP messages and message parts.</p>
 */
@SuppressWarnings("unchecked")
public abstract class Message<T extends Message<T>> {

	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();

	private final Map<Format<?>, Object> cache=new HashMap<>();
	private final Map<Format<?>, Function<Message<?>, Result<?>>> pipes=new HashMap<>();


	/**
	 * Retrieves this message.
	 *
	 * <p>This method is required to support the abstract fluent API through the self-bound abstract class pattern.</p>
	 *
	 * @return this message
	 */
	protected abstract T self();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Casts this message to a specific message type.
	 *
	 * @param clazz the target message class
	 * @param <C>   the target message type
	 *
	 * @return an optional containing this message cast to the target type, if the this message is an instance of {@code
	 * clazz}; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code clazz} is null
	 */
	public <C extends Message<?>> Optional<C> as(final Class<C> clazz) {

		if ( clazz == null ) {
			throw new NullPointerException("null clazz");
		}

		return clazz.isInstance(this) ? Optional.of(clazz.cast(this)) : Optional.empty();
	}


	/**
	 * Maps this message.
	 *
	 * @param mapper the message mapping function; must return a non-null value
	 * @param <R>    the type of the value returned by {@code mapper}
	 *
	 * @return a non-null value obtained by applying {@code mapper} to this message
	 *
	 * @throws NullPointerException if {@code mapper} is null or returns a null value
	 */
	public <R> R map(final Function<T, R> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return requireNonNull(mapper.apply(self()), "null mapper return value");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Tests if this message is interactive.
	 *
	 * @return {@code true} if an {@code Accept} or {@code Content-Type} header of this message include a MIME type
	 * usually associated with an interactive browser-managed HTTP exchanges (e.g. {@code text/html}
	 */
	public boolean interactive() {
		return Stream.of(headers("accept"), headers("content-type"))
				.flatMap(Collection::stream)
				.anyMatch(value -> HTMLPattern.matcher(value).find());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves message headers.
	 *
	 * @return an immutable and possibly empty map from header names to collections of headers values
	 */
	public Map<String, Collection<String>> headers() {
		return unmodifiableMap(headers);
	}

	/**
	 * Configures message headers.
	 *
	 * <p>Existing values are overwritten.</p>
	 *
	 * @param headers a map from header names to collections of headers values; empty and duplicate values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code headers} is null or contains either null keys or null values
	 */
	public T headers(final Map<String, ? extends Collection<String>> headers) {

		if ( headers == null ) {
			throw new NullPointerException("null headers");
		}

		headers.forEach((name, value) -> { // ;( parameters.containsKey()/ContainsValue() can throw NPE

			if ( name == null ) {
				throw new NullPointerException("null header name");
			}

			if ( value == null ) {
				throw new NullPointerException("null header value");
			}

		});

		this.headers.clear();

		headers.forEach(this::headers);

		return self();
	}


	/**
	 * Retrieves message header value.
	 *
	 * @param name the name of the header whose value is to be retrieved
	 *
	 * @return an optional value containing the first value among those returned by {@link #headers(String)}, if one is
	 * present; an empty optional otherwise
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public Optional<String> header(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers(name).stream().findFirst();
	}

	/**
	 * Configures message header value.
	 *
	 * <p>If {@code name} is prefixed with a tilde ({@code ~}), header {@code values} are set only if the header is not
	 * already defined; if {@code name} is {@code Set-Cookie} or is prefixed with a plus sign ({@code +}), header {@code
	 * values} are appended to existing values; otherwise, header {@code values} overwrite existing values.</p>
	 *
	 * @param name  the name of the header whose value is to be configured
	 * @param value the new value for {@code name}; empty values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is null
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">
	 * RFC 7230 Hypertext Transfer Protocol (HTTP/1.1): Message Syntax and Routing - § 3.2.2. Field Order</a>
	 */
	public T header(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return headers(name, value);
	}


	/**
	 * Retrieves message header values.
	 *
	 * @param name the name of the header whose values are to be retrieved
	 *
	 * @return an immutable and possibly empty collection of values
	 */
	public Collection<String> headers(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return unmodifiableCollection(headers.getOrDefault(normalize(name), emptyList()));
	}

	/**
	 * Configures message header values.
	 *
	 * <p>Existing values are overwritten, unless the header {@code name} is {@code Set-Cookie} or is prefixed with a
	 * plus sign ({@code +}).</p>
	 *
	 * @param name   the name of the header whose values are to be configured
	 * @param values a possibly empty collection of values; empty and duplicate values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
	 *                              {@code null} value
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.2. Field Order</a>
	 */
	public T headers(final String name, final String... values) {
		return headers(name, asList(values));
	}

	/**
	 * Configures message header values.
	 *
	 * <p>If {@code name} is prefixed with a tilde ({@code ~}), header {@code values} are set only if the header is not
	 * already defined; if {@code name} is {@code Set-Cookie} or is prefixed with a plus sign ({@code +}), header {@code
	 * values} are appended to existing values; otherwise, header {@code values} overwrite existing values.</p>
	 *
	 * @param name   the name of the header whose values are to be configured
	 * @param values a possibly empty collection of values; empty and duplicate values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
	 *                              {@code null} value
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.2. Field Order</a>
	 */
	public T headers(final String name, final Collection<String> values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		final String _name=normalize(name);
		final Collection<String> _values=normalize(values);

		if ( name.startsWith("~") ) {

			headers.computeIfAbsent(_name, key -> _values.isEmpty() ? null : _values);

		} else if ( name.startsWith("+") || _name.equals("Set-Cookie") ) {

			headers.compute(_name, (key, value) -> value == null ? _values : concat(value, _values));

		} else if ( _values.isEmpty() ) {

			headers.remove(_name);

		} else {

			headers.put(_name, unmodifiableCollection(_values));

		}

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a structured of this message body.
	 *
	 * @param format the body format managing the required body representation
	 * @param <V>    the type of the structured message body managed by the format
	 *
	 * @return a structured message body for this message
	 *
	 * @throws NullPointerException if {@code format} is null
	 */
	public <V> Body<V> body(final Format<V> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return new Body<>(format);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String normalize(final String name) {
		return title(name.startsWith("~") || name.startsWith("+") ? name.substring(1) : name);
	}

	private Collection<String> normalize(final Collection<String> values) {
		return values
				.stream()
				.filter(value -> !value.isEmpty())
				.distinct()
				.collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * HTTP message body.
	 *
	 * <p>Provides a type-safe interface for structured message bodies managed by {@linkplain Format formats}.</p>
	 *
	 * <p>A body is associated to a message through a message body {@linkplain Format format}, responsible for
	 * retrieving its structured value and configuring the message as a holder for structured values of the specific
	 * format-managed type.</p>
	 *
	 * @param <V> the type of the data structure exposed by the message body
	 */
	public final class Body<V> implements Result<V> {

		private final Format<V> format;


		private Body(final Format<V> format) {
			this.format=format;
		}


		/**
		 * Configures the structured message body.
		 *
		 * <p>Future calls to {@link Message#body(Format)} with the same format associated to this message body will
		 * return a message body holding the specified structured value, rather than the structured value {@linkplain
		 * Format#get(Message) retrieved} by the format associated to this body.</p>
		 *
		 * <p>The message this body belongs to is {@linkplain Format#set(Message) configured} for holding the
		 * structured value according to the format associated to this body.</p>
		 *
		 * @param value the structured value for this body
		 *
		 * @return the message this body belongs to
		 *
		 * @throws NullPointerException  if {@code value} is null
		 * @throws IllegalStateException if a body value was already retrieved from the message this body belongs to
		 *                               using one the getter {@linkplain Result result} methods on one of its bodies
		 */
		public T set(final V value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			if ( !cache.isEmpty() ) {
				throw new IllegalStateException("message body retrieved");
			}

			pipes.put(format, message -> value(value));

			return format.set(self());
		}


		/**
		 * Filters the structured value of this body.
		 *
		 * <p>Future calls to getter {@linkplain Result result} methods on this body will pipe the structured value
		 * either explicitly {@linkplain #set(Object) set} or {@linkplain Format#get(Message) retrieved} on demand by
		 * the format associated to this body through a filtering function.</p>
		 *
		 * @param mapper the value filtering function
		 *
		 * @return the message this body belongs to
		 *
		 * @throws NullPointerException  if {@code mapper} is null
		 * @throws IllegalStateException if a body value was already retrieved from the message this body belongs to
		 *                               using one the getter {@linkplain Result result} methods on one of its bodies
		 */
		public T pipe(final Function<V, V> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			if ( !cache.isEmpty() ) {
				throw new IllegalStateException("message body already retrieved");
			}

			return flatPipe(v -> value(mapper.apply(v)));
		}

		/**
		 * Filters the structured value of this body.
		 *
		 * <p>Future calls to getter {@linkplain Result result} methods on this body will pipe the structured value
		 * either explicitly {@linkplain #set(Object) set} or {@linkplain Format#get(Message) retrieved} on demand by
		 * the format associated to this body through a result-returning filtering function.</p>
		 *
		 * @param mapper the value filtering function
		 *
		 * @return the message this body belongs to
		 *
		 * @throws NullPointerException  if {@code mapper} is null
		 * @throws IllegalStateException if a body value was already retrieved from the message this body belongs to
		 *                               using one the getter {@linkplain Result result} methods on one of its bodies
		 */
		public T flatPipe(final Function<V, Result<V>> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			if ( !cache.isEmpty() ) {
				throw new IllegalStateException("message body already retrieved");
			}

			pipes.compute(format, (_format, getter) -> message ->
					(getter != null ? getter : (Function<Message<?>, Result<?>>)format::get)
							.apply(message)
							.flatMap(value -> mapper.apply((V)value)));

			return self();
		}


		/**
		 * {@inheritDoc}
		 *
		 * <p>Successfully retrieved structured values handled to the {@code success} mapper are cached for future
		 * reuse.</p>
		 */
		@Override public <R> R map(final Function<V, R> success, final Function<Failure<V>, R> failure) {

			if ( success == null ) {
				throw new NullPointerException("null success mapper");
			}

			if ( failure == null ) {
				throw new NullPointerException("null failure mapper");
			}

			final V cached=(V)cache.get(format);

			return cached != null ? success.apply(cached) : pipes.getOrDefault(format, format::get)
					.apply(self())
					.map(
							v -> {

								cache.put(format, v);

								return success.apply((V)v);

							},
							f -> failure.apply((Failure<V>)f)
					);
		}

	}

}
