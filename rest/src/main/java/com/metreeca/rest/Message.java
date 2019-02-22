/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Evaluator;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Strings.title;
import static com.metreeca.rest.Result.Value;

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
 *
 * <p>Messages are associated with possibly multiple {@linkplain #body(Body) body} representations managed by message
 * body {@linkplain Body formats}.</p>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 */
@SuppressWarnings("unchecked")
public abstract class Message<T extends Message<T>> {

	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	/**
	 * Creates a {@code Link} header value.
	 *
	 * @param resource the target resource to be linked through the header
	 * @param relation the relation with the target {@code resource}
	 *
	 * @return the header value linking the target {@code resource} with the given {@code relation}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code relation} is null
	 */
	public static String link(final IRI resource, final String relation) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( relation == null ) {
			throw new NullPointerException("null relation");
		}

		return String.format("<%s>; rel=\"%s\"", resource, relation);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape=pass();

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();

	private final Map<Body<?>, Object> cache=new HashMap<>();
	private final Map<Body<?>, Function<Message<?>, Result<?, Failure>>> pipes=new HashMap<>();


	private T self() { return (T)this; }


	/**
	 * Retrieves the focus item IRI of this message.
	 *
	 * @return an absolute IRI identifying the focus item of this message
	 */
	public abstract IRI item();

	/**
	 * Retrieves the originating request for this message.
	 *
	 * @return the originating request for this message
	 */
	public abstract Request request();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
	 * Retrieves the linked data shape.
	 *
	 * @return the linked data shape associated to this message; defaults to the {@linkplain Evaluator#pass() wildcard}
	 * shape
	 */
	public Shape shape() {
		return shape;
	}

	/**
	 * Configures the linked data shape.
	 *
	 * @param shape the linked data shape to be associated to this message
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public T shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;

		return self();
	}


	/**
	 * Retrieves a body representation.
	 *
	 * @param body the body format managing the body representation to be retrieved
	 * @param <V>  the type of the body representation managed by {@code body}
	 *
	 * @return a result providing access to the body representation managed by {@code body}, if one was successfully
	 * retrieved from this message; a result providing access to the body processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code body} is null
	 */
	public <V> Result<V, Failure> body(final Body<V> body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		final V cached=(V)cache.get(body);

		return cached != null ? Value(cached) : pipes.getOrDefault(body, body::get).apply(self()).value(

				value -> {

					cache.put(body, value);

					return (V)value;

				}

		);

	}

	/**
	 * Configures a body representation.
	 *
	 * <p>Future calls to {@link #body(Body)} with the same body format will return the specified value, rather than
	 * the value {@linkplain Body#get(Message) retrieved} from this message by body.</p>
	 *
	 * @param body  the body format managing the body representation to be configured
	 * @param value the body representation to be associated with {@code body}
	 * @param <V>   the type of the body representation managed by {@code body}
	 *
	 * @return this message
	 *
	 * @throws NullPointerException  if either {@code body} or {@code value} is null
	 * @throws IllegalStateException if a body value was already {@linkplain #body(Body) retrieved} from this message
	 */
	public <V> T body(final Body<V> body, final V value) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		if ( !cache.isEmpty() ) {
			throw new IllegalStateException("message body already retrieved");
		}

		pipes.put(body, message -> Value(value));

		return body.set(self());
	}


	/**
	 * Process a body representation.
	 *
	 * <p>Future calls to {@link #body(Body)} with the same body format will pipe the value either explicitly
	 * {@linkplain #body(Body, Object) set} or {@linkplain Body#get(Message) retrieved} on demand by the body through a
	 * result-returning processing function.</p>
	 *
	 * <p><strong>Warning</strong> / Processing is performed on demand, as final consumer eventually retrieves the
	 * processed message body: if {@code mapper} relies on information retrieved from the message, its current state
	 * must be memoized, before it's possibly altered by downstream wrappers.</p>
	 *
	 * @param body   the body format managing the body representation to be processed
	 * @param mapper the value processing function
	 * @param <V>    the type of the body representation managed by {@code body}
	 *
	 * @return this message
	 *
	 * @throws NullPointerException  if either {@code body} or {@code mapper} is null
	 * @throws IllegalStateException if a body value was already {@linkplain #body(Body) retrieved} from this message
	 */
	public <V> T pipe(final Body<V> body, final Function<V, Result<V, Failure>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		if ( !cache.isEmpty() ) {
			throw new IllegalStateException("message body already retrieved");
		}

		pipes.compute(body, (_format, getter) -> message ->
				(getter != null ? getter : (Function<Message<?>, Result<?, Failure>>)body::get)
						.apply(message).fold(value -> mapper.apply((V)value), Result::Error)
		);

		return self();
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

}
