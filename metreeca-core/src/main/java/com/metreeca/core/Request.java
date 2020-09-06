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

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.util.Arrays.asList;
import static java.util.Collections.*;


/**
 * HTTP request.
 */
public final class Request extends Message<Request> {

	public static final String GET="GET"; // https://tools.ietf.org/html/rfc7231#section-4.3.1
	public static final String HEAD="HEAD"; // https://tools.ietf.org/html/rfc7231#section-4.3.2
	public static final String POST="POST"; // https://tools.ietf.org/html/rfc7231#section-4.3.3
	public static final String PUT="PUT"; // https://tools.ietf.org/html/rfc7231#section-4.3.4
	public static final String PATCH="PATCH"; // https://tools.ietf.org/html/rfc5789#section-2
	public static final String DELETE="DELETE"; // https://tools.ietf.org/html/rfc7231#section-4.3.5
	public static final String CONNECT="CONNECT"; // https://tools.ietf.org/html/rfc7231#section-4.3.6
	public static final String OPTIONS="OPTIONS"; // https://tools.ietf.org/html/rfc7231#section-4.3.7
	public static final String TRACE="TRACE"; // https://tools.ietf.org/html/rfc7231#section-4.3.8


	private static final Collection<String> Safe=new HashSet<>(asList(
			GET, HEAD, OPTIONS, TRACE // https://tools.ietf.org/html/rfc7231#section-4.2.1
	));

	private static final Pattern SchemePattern=Pattern.compile("[a-zA-Z][-+.a-zA-Z0-9]*:");
	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	public static Map<String, List<String>> search(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		final Map<String, List<String>> parameters=new LinkedHashMap<>();

		final int length=query.length();

		for (int head=0, tail; head < length; head=tail+1) {
			try {

				final int equal=query.indexOf('=', head);
				final int ampersand=query.indexOf('&', head);

				tail=(ampersand >= 0) ? ampersand : length;

				final boolean split=equal >= 0 && equal < tail;

				final String label=URLDecoder.decode(query.substring(head, split ? equal : tail), "UTF-8");
				final String value=URLDecoder.decode(query.substring(split ? equal+1 : tail, tail), "UTF-8");

				parameters.compute(label, (name, values) -> {

					final List<String> strings=(values != null) ? values : new ArrayList<>();

					strings.add(value);

					return strings;

				});

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}
		}

		return parameters;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Object user=null;
	private Set<Object> roles=emptySet();

	private String method="";
	private String base="app:/";
	private String path="/";
	private String query="";

	private final Map<String, List<String>> parameters=new LinkedHashMap<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the focus item IRI of this request.
	 *
	 * @return the absolute IRI obtained by concatenating {@linkplain #base() base} and {@linkplain #path() path} for
	 * this request
	 */
	@Override public String item() {
		return base+path.substring(1);
	}

	/**
	 * Retrieves the originating request for this request.
	 *
	 * @return this request
	 */
	@Override public Request request() {
		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a response for this request.
	 *
	 * @param mapper the mapping function  used to initialize the new response; must return a non-null value
	 *
	 * @return a new lazy response for this request
	 *
	 * @throws NullPointerException if {@code mapper} is null or return a null value
	 */
	public Future<Response> reply(final Function<Response, Response> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return consumer -> consumer.accept(new Response(this).map(mapper));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if this request is safe.
	 *
	 * @return {@code true} if this request is <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">safe</a> ,
	 * that is if it's not expected to cause any state change on the origin server ; {@code false} otherwise
	 */
	public boolean safe() {
		return Safe.contains(method);
	}

	/**
	 * Checks if this request targets a collection.
	 *
	 * @return {@code true} if the {@link #path()} of this request includes a trailing slash; {@code false} otherwise
	 *
	 * @see
	 * <a href="https://www.w3.org/TR/ldp-bp/#include-a-trailing-slash-in-container-uris">Linked Data Platform Best
	 * Practices and Guidelines - § 2.6 Include a trailing slash in container URIs</a>
	 */
	public boolean collection() {
		return path.endsWith("/");
	}

	/**
	 * Checks if request is interactive.
	 *
	 * @return {@code true} if the {@linkplain #method() method} of this request is {@link #GET} and the {@code Accept}
	 * header includes a MIME type usually associated with an interactive browser-managed HTTP request
	 * (e.g. {@code text /html}
	 */
	public boolean interactive() {
		return method.equals(GET) && headers("content-type")
				.stream()
				.anyMatch(value -> HTMLPattern.matcher(value).find());
	}


	/**
	 * Checks if this request if performed by a user in a target set of roles.
	 *
	 * @param roles the target set if roles to be checked
	 *
	 * @return {@code true} if this request is performed by a {@linkplain #user() user} in one of the given {@code
	 * roles}, that is if {@code roles} and  {@linkplain #roles() request roles} are not disjoint
	 */
	public boolean role(final Object... roles) {
		return role(asList(roles));
	}

	/**
	 * Checks if this request if performed by a user in a target set of roles.
	 *
	 * @param roles the target set if roles to be checked
	 *
	 * @return {@code true} if this request is performed by a {@linkplain #user() user} in one of the given {@code
	 * roles}, that is if {@code roles} and  {@linkplain #roles() request roles} are not disjoint
	 */
	public boolean role(final Collection<Object> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		return !disjoint(this.roles, roles);
	}


	//// Actor ////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the identifier of the request user.
	 *
	 * @return an optional identifier for the user performing this request or the empty optional if no user is
	 * authenticated
	 */
	public Optional<Object> user() { return Optional.ofNullable(user); }

	/**
	 * Configures the identifier of the request user.
	 *
	 * @param user an identifier for the user performing this request or {@code null} if no user is authenticated
	 *
	 * @return this request
	 */
	public Request user(final Object user) {

		this.user=user;

		return this;
	}


	/**
	 * Retrieves the roles attributed to the request user.
	 *
	 * @return a set of values uniquely identifying the roles attributed to the request {@linkplain #user() user}
	 */
	public Set<Object> roles() { return unmodifiableSet(roles); }

	/**
	 * Configures the roles attributed to the request user.
	 *
	 * @param roles a collection of values uniquely identifying the roles assigned to the request {@linkplain #user()
	 *              user}
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
	 */
	public Request roles(final Object... roles) {
		return roles(asList(roles));
	}

	/**
	 * Configures the roles attributed to the request user.
	 *
	 * @param roles a collection of IRIs uniquely identifying the roles assigned to the request {@linkplain #user()
	 *              user}
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
	 */
	public Request roles(final Collection<Object> roles) {

		if ( roles == null || roles.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null roles");
		}

		this.roles=new LinkedHashSet<>(roles);

		return this;
	}


	//// Action ///////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the HTTP method of this request.
	 *
	 * @return the HTTP method of this request; in upper case
	 */
	public String method() {
		return method;
	}

	/**
	 * Configures the HTTP method of this request.
	 *
	 * @param method the HTTP method for this request; will be automatically converted to upper case
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code method} is null
	 */
	public Request method(final String method) {

		if ( method == null ) {
			throw new NullPointerException("null method");
		}

		this.method=method.toUpperCase(Locale.ROOT);

		return this;
	}


	/**
	 * Retrieves the base IRI of this request.
	 *
	 * @return the base IRI of this request, that is the base IRI if the linked data server handling the request;
	 * includes a trailing slash
	 */
	public String base() {
		return base;
	}

	/**
	 * Configures the base IRI of this request.
	 *
	 * @param base the base IRI for this request, that is the base IRI if the linked data server handling the request
	 *
	 * @return this request
	 *
	 * @throws NullPointerException     if {@code base} is null
	 * @throws IllegalArgumentException if {@code base} is not an absolute IRI or if it doesn't include a trailing
	 *                                  slash
	 */
	public Request base(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		if ( !SchemePattern.matcher(base).lookingAt() ) {
			throw new IllegalArgumentException("not an absolute base IRI");
		}

		if ( !base.endsWith("/") ) {
			throw new IllegalArgumentException("missing trailing / in base IRI");
		}

		this.base=base;

		return this;
	}


	/**
	 * Retrieves the resource path of this request.
	 *
	 * @return the resource path of this request, that is the absolute server path of the linked data resources this
	 * request refers to; includes a leading slash
	 */
	public String path() {
		return path;
	}

	/**
	 * Configures the resource path of this request.
	 *
	 * @param path the resource path of this request, that is the absolute server path of the linked data resources
	 *             this request refers to
	 *
	 * @return this request
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} doesn't include a leading slash
	 */
	public Request path(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null resource path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("missing leading / in resource path");
		}

		this.path=path;

		return this;
	}


	/**
	 * Retrieves the query of this request.
	 *
	 * @return the query this request
	 */
	public String query() {
		return query;
	}

	/**
	 * Configures the query of this request.
	 *
	 * @param query the query of this request
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code query} is null
	 */
	public Request query(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		this.query=query;

		return this;
	}


	//// Parameters ///////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves request query parameters.
	 *
	 * @return an immutable and possibly empty map from query parameters names to collections of values
	 */
	public Map<String, List<String>> parameters() {
		return unmodifiableMap(parameters);
	}

	/**
	 * Configures request query parameters.
	 *
	 * <p>Existing values are overwritten.</p>
	 *
	 * @param parameters a map from parameter names to lists of values
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code parameters} is null or contains either null keys or null values
	 */
	public Request parameters(final Map<String, ? extends Collection<String>> parameters) {

		if ( parameters == null ) {
			throw new NullPointerException("null parameters");
		}

		parameters.forEach((name, value) -> { // ;( parameters.containsKey()/ContainsValue() can throw NPE

			if ( name == null ) {
				throw new NullPointerException("null parameter name");
			}

			if ( value == null ) {
				throw new NullPointerException("null parameter value");
			}

		});

		this.parameters.clear();

		parameters.forEach(this::parameters);

		return this;
	}


	/**
	 * Retrieves request query parameter value.
	 *
	 * @param name the name of the query parameter whose value is to be retrieved
	 *
	 * @return an optional value containing the first value among those returned by {@link #parameters(String)}, if one
	 * is present; an empty optional otherwise
	 *
	 * @throws NullPointerException if {@code name} is null
	 */
	public Optional<String> parameter(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return parameters(name).stream().findFirst();
	}

	/**
	 * Configures request query parameter value.
	 *
	 * <p>Existing values are overwritten.</p>
	 *
	 * @param name  the name of the query parameter whose value is to be configured
	 * @param value the new value for {@code name}
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code value} is null
	 */
	public Request parameter(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return parameters(name, value);
	}


	/**
	 * Retrieves request query parameter values.
	 *
	 * @param name the name of the query parameter whose values are to be retrieved
	 *
	 * @return an immutable and possibly empty collection of values
	 */
	public List<String> parameters(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return unmodifiableList(parameters.getOrDefault(name, emptyList()));
	}

	/**
	 * Configures request query parameter values.
	 *
	 * <p>Existing values are overwritten.</p>
	 *
	 * @param name   the name of the query parameter whose values are to be configured
	 * @param values a possibly empty collection of values
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
	 *                              {@code null} value
	 */
	public Request parameters(final String name, final String... values) {
		return parameters(name, asList(values));
	}

	/**
	 * Configures request query parameter values.
	 *
	 * <p>Existing values are overwritten.</p>
	 *
	 * @param name   the name of the query parameter whose values are to be configured
	 * @param values a possibly empty collection of values
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code name} or {@code values} is null or if {@code values} contains a
	 *                              {@code null} value
	 */
	public Request parameters(final String name, final Collection<String> values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		if ( values.isEmpty() ) {

			parameters.remove(name);

		} else {

			parameters.put(name, unmodifiableList(new ArrayList<>(values)));

		}

		return this;
	}

}
