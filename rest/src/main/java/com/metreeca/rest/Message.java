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

import com.metreeca.form.Result;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.form.Result.error;
import static com.metreeca.form.Result.value;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Strings.title;

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
public abstract class Message<T extends Message<T>> {

	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();

	private final Map<Format<?>, Object> cache=new HashMap<>();
	private final Map<Format<?>, Function<T, Result<?, Failure>>> bodies=new HashMap<>();


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

	/**
	 * Executes a task on this message.
	 *
	 * @param task the task to be executed on this message
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code task} is null
	 */
	public T with(final Consumer<T> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		task.accept(self());

		return self();
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
	 * <p>Existing values are overwritten, unless the header {@code name} is {@code Set-Cookie} or is prefixed with a
	 * plus sign ({@code +}).</p>
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
	 * Maps message header value.
	 *
	 * <p>Existing values are overwritten, unless the header {@code name} is {@code Set-Cookie} or is prefixed with a
	 * plus sign ({@code +}).</p>
	 *
	 * @param name   the name of the header whose value is to be mapped
	 * @param mapper the mapping function for the header value; takes as argument the optional {@linkplain
	 *               #header(String) current header value} and returns the mapped header value or an empty string, if
	 *               mapping produced no values
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code mapper} is null or {@code mapper} returns a null
	 *                              value
	 */
	public T header(final String name, final Function<Optional<String>, String> mapper) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return header(name, requireNonNull(mapper.apply(header(name)), "null mapper return value"));
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

		if ( name.startsWith("+") || _name.equals("Set-Cookie") ) {

			headers.compute(_name, (key, value) -> value == null ? _values : concat(value, _values));

		} else if ( _values.isEmpty() ) {

			headers.remove(_name);

		} else {

			headers.put(_name, unmodifiableCollection(_values));

		}

		return self();
	}

	/**
	 * Maps message header values.
	 *
	 * <p>Existing values are overwritten, unless the header {@code name} is {@code Set-Cookie} or is prefixed with a
	 * plus sign ({@code +}).</p>
	 *
	 * @param name   the name of the header whose values is to be mapped
	 * @param mapper the mapping function for the header values; takes as argument the collection of {@linkplain
	 *               #headers(String) current header values} and returns the mapped header values or an empty
	 *               collection, if mapping produced no values
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code mapper} is null or {@code mapper} returns a null
	 *                              value
	 */
	public T headers(final String name, final Function<Collection<String>, Collection<String>> mapper) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return headers(name, requireNonNull(mapper.apply(headers(name)), "null mapper return value"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a representation of the body of this message.
	 *
	 * @param format the format of the body representation to be retrieved
	 * @param <V>    the type of the body representation to be retrieved
	 *
	 * @return a result providing access to the expected representation of the body of this message, if previously
	 * defined with the same {@code format} or successfully {@linkplain Format#get(Message) derived} by {@code format}
	 * from existing representations; a result providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code format} is null
	 */
	@SuppressWarnings("unchecked")
	public <V> Result<V, Failure> body(final Format<V> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		final V cached=(V)cache.get(format);

		return cached != null ? value(cached) : bodies.getOrDefault(format, format::get).apply(self()).map(
				v -> {
					cache.put(format, v);
					return value((V)v);
				},
				e -> error(e)
		);
	}

	/**
	 * Configures a representation of the body of this message.
	 *
	 * @param format the format of the body representation to be configured
	 * @param value  the body representation of the body to be configured using {@code format}
	 * @param <V>    the type of the body representation to be configured
	 *
	 * @return this message
	 *
	 * @throws NullPointerException  if either {@code format} or {@code body} is null
	 * @throws IllegalStateException if body representations were already retrieved from this message
	 */
	@SuppressWarnings({"unchecked", "ObjectEquality"})
	public <V> T body(final Format<V> format, final V value) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return _body(format, message -> value(value));
	}

	public <V> T _body(final Format<V> format, final Function<T, Result<V, Failure>> body) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		if ( !cache.isEmpty() ) {
			throw new IllegalStateException("message body representations already retrieved");
		}

		bodies.put(format, (Function<T, Result<?, Failure>>)(Object)body); // !!! @@@

		return format.set(self());
	}

	/**
	 * Filters a representation of the body of this message.
	 *
	 * <p>Replaces the current body representation of this message associated to a given format with a new
	 * representation generated by filtering it with a mapping function; if this message has no body representation
	 * associated to the given format, one is retrieved on demand using the format {@linkplain Format#get(Message)
	 * getter}.</p>
	 *
	 * @param format the format of the body representation to be filtered
	 * @param mapper the mapping function for the body representation
	 * @param <V>    the type of the body representation to be filtered
	 *
	 * @return this message
	 *
	 * @throws NullPointerException  if either {@code format} or {@code mapper} is null or if {@code mapper} returns a
	 *                               null value
	 * @throws IllegalStateException if body representations were already retrieved from this message
	 */
	@SuppressWarnings("unchecked")
	public <V> T filter(final Format<V> format, final Function<V, V> mapper) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		if ( !cache.isEmpty() ) {
			throw new IllegalStateException("message body representations already retrieved");
		}

		bodies.compute(format, (_format, getter) -> message -> (

				getter != null ? getter : (Function<T, Result<?, Failure>>)_format::get

		).apply(message).map(

				v -> value(requireNonNull(mapper.apply((V)v), "null mapper return value")),
				e -> error(e)

		));

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String normalize(final String name) {
		return title(name.startsWith("+") ? name.substring(1) : name);
	}

	private Collection<String> normalize(final Collection<String> values) {
		return values
				.stream()
				.filter(value -> !value.isEmpty())
				.distinct()
				.collect(toList());
	}

}
