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

import com.metreeca.rest.formats.MultipartFormat;

import java.net.URI;
import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rest.Either.Right;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;


/**
 * HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP messages and message parts.</p>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 */
@SuppressWarnings("unchecked")
public abstract class Message<T extends Message<T>> {

	private static final Pattern CharsetPattern=Pattern.compile(
			";\\s*charset\\s*=\\s*(?<charset>[-\\w]+)\\b"
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<Supplier<?>, Object> attributes=new LinkedHashMap<>();
	private final Map<String, List<String>> headers=new LinkedHashMap<>();
	private final Map<Format<?>, Either<MessageException, ?>> bodies=new HashMap<>();


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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the charset of this message.
	 *
	 * <p><strong>Warning</strong> / The {@code Accept-Charset} header or the originating request is
	 * <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Charset">ignored</a>.</p>
	 *
	 * @return the charset defined in the {@code Content-Type} header of this message, defaulting to {@code UTF-8} if no
	 * charset is explicitly defined
	 */
	public String charset() {
		return header("Content-Type")

				.map(CharsetPattern::matcher)
				.filter(Matcher::find)
				.map(matcher -> matcher.group("charset"))

				.orElse(UTF_8.name());
	}


	/**
	 * Creates a message part.
	 *
	 * @param item the (possibly relative)) IRI identifying the {@linkplain #item() focus item} of the new message part;
	 *             will be resolved against the message {@linkplain #item() item} IRI
	 *
	 * @return a new message part with a focus item identified by {@code item} and the same {@linkplain #request()
	 * originating request} as this message
	 *
	 * @throws NullPointerException     if {@code item} is null
	 * @throws IllegalArgumentException if {@code item} is not a legal (possibly relative) IRI
	 */
	public Message<?> part(final String item) {

		if ( item == null ) {
			throw new NullPointerException("null item");
		}

		return new Part(URI.create(item()).resolve(item).toString(), this);
	}

	/**
	 * Lift a message part into this message.
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
	 *           return handler.handle(request.lift(main));
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
	 * @param message the message part to be lifted into this message
	 *
	 * @return this message modified as follows:
	 * <ul>
	 * <li>source {@code message} headers are copied to this message overriding existing matching values;</li>
	 * <li>source {@code message} body representations are copied to this message replacing all existing values.</li>
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves message attribute.
	 *
	 * @param name the name for the attribute to be retrieved; must return a non-null default value for the attribute
	 * @param <V>  the type of the attribute to be retrieved
	 *
	 * @return the value of message attribute associated with {@code name}
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public <V> V attribute(final Supplier<V> name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return (V)attributes.computeIfAbsent(name, _key -> requireNonNull(_key.get(), "null name value"));
	}

	/**
	 * Configures message attribute.
	 *
	 * @param name  the name for the attribute to be configured; must return a non-null default value for the attribute
	 * @param value the attribute value to be associated with {@code name}
	 * @param <V>   the type of the attribute to be configured
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is null
	 */
	public <V> T attribute(final Supplier<V> name, final V value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		attributes.put(name, value);

		return self();
	}


	/**
	 * Updates message attribute.
	 *
	 * @param name   the name for the attribute to be updated; must return a non-null default value for the attribute
	 * @param mapper a function mapping from the current to the updated value of the {@code name} attribute; must return
	 *               a non-null value
	 * @param <V>    the type of the attribute to be updated
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is null
	 */
	public <V> T map(final Supplier<V> name, final UnaryOperator<V> mapper) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		attributes.computeIfPresent(name, (key, value) -> mapper.apply((V)value));

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves message headers.
	 *
	 * @return an immutable and possibly empty map from header names to collections of headers values
	 */
	public Map<String, List<String>> headers() {
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
	public List<String> headers(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		final String _name=normalize(name);

		return unmodifiableList(headers.entrySet().stream()
				.filter(entry -> entry.getKey().equalsIgnoreCase(_name))
				.map(Map.Entry::getValue)
				.findFirst()
				.orElseGet(Collections::emptyList)
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
		final List<String> _values=normalize(values);

		if ( name.startsWith("~") ) {

			headers.computeIfAbsent(_name, key -> _values.isEmpty() ? null : _values);

		} else if ( name.startsWith("+") || _name.equals("Set-Cookie") ) {

			headers.compute(_name, (key, value) -> value == null ?
					_values : Stream.concat(value.stream(), _values.stream()).collect(toList())
			);

		} else if ( _values.isEmpty() ) {

			headers.remove(_name);

		} else {

			headers.put(_name, unmodifiableList(_values));

		}

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes message body.
	 *
	 * @param format the expected body format
	 * @param <V>    the type of the body to be decoded
	 *
	 * @return either a message exception reporting a decoding issue or the message body {@linkplain
	 * Format#decode(Message) decoded} by {@code format}
	 *
	 * @throws NullPointerException if {@code format} is null
	 */
	public <V> Either<MessageException, V> body(final Format<V> format) {

		if ( format == null ) {
			throw new NullPointerException("null body");
		}

		return (Either<MessageException, V>)bodies.computeIfAbsent(format, key -> format.decode(this));
	}

	/**
	 * Encodes message body.
	 *
	 * <p>Subsequent calls to {@link #body(Format)} with the same body format will return the specified value.</p>
	 *
	 * @param format the body format
	 * @param value  the body to be encoded
	 * @param <V>    the type of the body to be encoded
	 *
	 * @return this message with the {@code value} {@linkplain Format#encode(Message, Object) encoded} by {@code format}
	 * as body
	 *
	 * @throws NullPointerException if either {@code format} or {@code value} is null
	 */
	public <V> T body(final Format<? super V> format, final V value) {

		if ( format == null ) {
			throw new NullPointerException("null body");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		bodies.put(format, Right(value));

		return format.encode(self(), value);
	}


	/**
	 * Updates message body.
	 *
	 * @param format the body format
	 * @param mapper a function mapping from the current to the updated value of the {@code format} body; must return a
	 *               non-null value
	 * @param <V>    the type of the body to be updated
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is null
	 */
	public <V> T map(final Format<? super V> format, final UnaryOperator<V> mapper) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}


		Optional

				.ofNullable(bodies.computeIfPresent(format, (key, value) -> value.map(body -> requireNonNull(

						mapper.apply((V)body), "null mapper return value"

				))))

				.ifPresent(result -> result.map(body ->

						format.encode(self(), (V)body)

				));

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String normalize(final String name) {
		return (name.startsWith("~") || name.startsWith("+") ? name.substring(1) : name).toLowerCase(Locale.ROOT);
	}

	private List<String> normalize(final Collection<String> values) {
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
