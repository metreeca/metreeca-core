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

package com.metreeca.next;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Strings.title;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;


/**
 * Abstract HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP messages and message parts.</p>
 */
public abstract class Message<T extends Message<T>> {

	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Map<String, Collection<String>> headers=new LinkedHashMap<>();


	/**
	 * Retrieves this message.
	 *
	 * @return this message
	 *
	 * @apiNote required to support abstract fluent API through the self-bound abstract class pattern.
	 */
	protected abstract T self();


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
	 * @throws NullPointerException if {@code map} is {@code null} or contains either null keys or null values
	 */
	public T headers(final Map<String, Collection<String>> headers) {

		if ( headers == null ) {
			throw new NullPointerException("null headers");
		}

		if ( headers.containsKey(null) ) {
			throw new NullPointerException("null part name");
		}

		if ( headers.containsValue(null) ) {
			throw new NullPointerException("null part content");
		}

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
	 * @throws NullPointerException if {@code name} is {@code null}
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
	 * <p>Existing values are overwritten, except when defining a new cookie with {@code Set-Cookie}.</p>
	 *
	 * @param name  the name of the header whose value is to be configured
	 * @param value the new value for {@code name}; empty values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is {@code null}
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.2. Field Order</a>)
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
	 * <p>Existing values are overwritten, except when defining a new cookie with {@code Set-Cookie}.</p>
	 *
	 * @param name   the name of the header whose values are to be configured
	 * @param values a possibly empty collection of values; empty and duplicate values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is {@code null} or if {@code values}
	 *                              contains a {@code null} value
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.2. Field Order</a>)
	 */
	public T headers(final String name, final String... values) {
		return headers(name, asList(values));
	}

	/**
	 * Configures message header values.
	 *
	 * <p>Existing values are overwritten, except when defining a new cookie with {@code Set-Cookie}.</p>
	 *
	 * @param name   the name of the header whose values are to be configured
	 * @param values a possibly empty collection of values; empty and duplicate values are ignored
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is {@code null} or if {@code values}
	 *                              contains a {@code null} value
	 * @see <a href="https://tools.ietf.org/html/rfc7230#section-3.2.2">RFC 7230 Hypertext Transfer Protocol (HTTP/1.1):
	 * Message Syntax and Routing - § 3.2.2. Field Order</a>)
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

		if ( _name.equals("Set-Cookie") ) {

			headers.compute(_name, (key, value) -> concat(value, _values));

		} else if ( _values.isEmpty() ) {

			headers.remove(_name);

		} else {

			headers.put(_name, unmodifiableCollection(_values));

		}

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String normalize(final String name) {
		return title(name);
	}

	private Collection<String> normalize(final Collection<String> values) {
		return values
				.stream()
				.filter(value -> !value.isEmpty())
				.distinct()
				.collect(toList());
	}

}
