/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.formats.MultipartFormat;
import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.And;

import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.Result.Value;
import static com.metreeca.tree.shapes.And.and;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP messages and message parts.</p>
 *
 * <p>Messages are associated with possibly multiple {@linkplain #body(Format) body} representations managed by message
 * body {@linkplain Format formats}.</p>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 */
@SuppressWarnings("unchecked")
public abstract class Message<T extends Message<T>> {

	private static final Pattern CharsetPattern=Pattern.compile(";\\s*charset\\s*=\\s*(?<charset>[-\\w]+)\\b");
	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape shape=and();

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();
	private final Map<Format<?>, Object> bodies=new HashMap<>();


	private T self() { return (T)this; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the focus item of this message.
	 *
	 * @return an absolute IRI identifying the focus item of this message
	 */
	public abstract String item();

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


	/**
	 * Lift a message into this message.
	 *
	 * <p>Mainly intended to be used inside wrappers to lift the main message part in {@linkplain MultipartFormat
	 * multipart} requests for further downstream processing, as for instance in:</p>
	 *
	 * <pre>{@code handler -> request -> request.body(multipart(1000, 10_000)).fold(
	 *
	 *     parts -> Optional.ofNullable(parts.get("main"))
	 *
	 *         .map(main -> {
	 *
	 *           ... // process ancillary body parts
	 *
	 *           return handler.handle(request.merge(main));
	 *
	 *         })
	 *
	 *         .orElseGet(() -> request.reply(new Failure()
	 *             .status(BadRequest)
	 *             .cause("missing main body part")
	 *         )),
	 *
	 *     request::reply
	 *
	 * )}</pre>
	 *
	 * @param message the source message to be merged into this message
	 *
	 * @return this message modified as follows:
	 * <ul>
	 * <li>source message headers are copied to this message overriding existing values;</li>
	 * <li>source message body representations are copied to this message replacing all existing values.</li>
	 * </ul>
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public T lift(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		headers.putAll(message.headers); // value lists are read-only

		bodies.clear();
		bodies.putAll(message.bodies);

		return self();
	}

	/**
	 * Creates a linked message.
	 *
	 * @param item the (possibly relative)) IRI identifying the {@linkplain #item() focus item} of the new linked
	 *             message; will be resolved against the message {@linkplain #item() item} IRI
	 *
	 * @return a new linked message with a focus item identified by {@code item} and the same {@linkplain #request()
	 * originating request} as this message
	 *
	 * @throws NullPointerException     if {@code item} is null
	 * @throws IllegalArgumentException if {@code item} is not a legal (possibly relative) IRI
	 */
	public Message<?> link(final String item) {

		if ( item == null ) {
			throw new NullPointerException("null item");
		}

		return new Part(URI.create(item()).resolve(item).toString(), this);
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

	/**
	 * Retrieves the character encoding of this message.
	 *
	 * @return the character encoding set in the {@code Content-Type} header of this message; empty if this message
	 * doesn't include a  {@code Content-Type} header or if no character encoding is explicitly set
	 */
	public Optional<String> charset() {
		return header("Content-Type")
				.map(CharsetPattern::matcher)
				.filter(Matcher::find)
				.map(matcher -> matcher.group("charset"));
	}


	//// Shape /////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the linked data shape.
	 *
	 * @return the linked data shape associated to this message; defaults to an {@linkplain And#and() empty conjunction}
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


	//// Headers ///////////////////////////////////////////////////////////////////////////////////////////////////////

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

		final String _name=normalize(name);

		return unmodifiableCollection(headers.entrySet().stream()
				.filter(entry -> entry.getKey().equalsIgnoreCase(_name))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseGet(Collections::emptySet)
		);
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

			headers.compute(_name, (key, value) -> value == null ?
					_values : Stream.concat(value.stream(), _values.stream()).collect(toList())
			);

		} else if ( _values.isEmpty() ) {

			headers.remove(_name);

		} else {

			headers.put(_name, unmodifiableCollection(_values));

		}

		return self();
	}


	//// Bodies ////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Retrieves a body representation.
	 *
	 * @param format the body format managing the body representation to be retrieved
	 * @param <V>    the type of the body representation managed by {@code body}
	 *
	 * @return a result providing access to the body representation managed by {@code body}, if one was successfully
	 * retrieved from this message; a result providing access to the body processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code body} is null
	 */
	public <V> Result<V, Failure> body(final Format<V> format) {

		if ( format == null ) {
			throw new NullPointerException("null body");
		}

		final V cached=(V)bodies.get(format);

		return cached != null ? Value(cached) : format.get(self()).value(value -> {

			bodies.put(format, value);

			return value;

		});

	}

	/**
	 * Configures a body representation.
	 *
	 * <p>Future calls to {@link #body(Format)} with the same body format will return the specified value, rather than
	 * the value {@linkplain Format#get(Message) retrieved} from this message by the body format.</p>
	 *
	 * @param format the body format managing the body representation to be configured
	 * @param value  the body representation to be associated with {@code body}
	 * @param <V>    the type of the body representation managed by {@code body}
	 *
	 * @return this message
	 *
	 * @throws NullPointerException  if either {@code body} or {@code value} is null
	 * @throws IllegalStateException if a body value was already {@linkplain #body(Format) retrieved} from this message
	 */
	public <V> T body(final Format<V> format, final V value) {

		if ( format == null ) {
			throw new NullPointerException("null body");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		bodies.put(format, value);

		return format.set(self(), value);
	}

	public <V> T body(final Format<V> format, final Function<V, V> mapper) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String normalize(final String name) {
		return (name.startsWith("~") || name.startsWith("+") ? name.substring(1) : name).toLowerCase(Locale.ROOT);
	}

	private Collection<String> normalize(final Collection<String> values) {
		return values
				.stream()
				.filter(value -> !value.isEmpty())
				.distinct()
				.collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Part extends Message<Part> {

		private final String item;
		private final Request request;


		private Part(final String item, final Message<?> message) {
			this.item=item;
			this.request=message.request();
		}


		@Override public String item() {
			return item;
		}

		@Override public Request request() {
			return request;
		}

	}

}
