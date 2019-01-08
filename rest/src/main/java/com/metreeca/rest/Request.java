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

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.codecs.QueryParser;
import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.function.Function;

import javax.json.JsonException;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Strings.upper;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;

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
	public static final String DELETE="DELETE"; // https://tools.ietf.org/html/rfc7231#section-4.3.5
	public static final String CONNECT="CONNECT"; // https://tools.ietf.org/html/rfc7231#section-4.3.6
	public static final String OPTIONS="OPTIONS"; // https://tools.ietf.org/html/rfc7231#section-4.3.7
	public static final String TRACE="TRACE"; // https://tools.ietf.org/html/rfc7231#section-4.3.8
	public static final String PATCH="PATCH"; // https://tools.ietf.org/html/rfc5789#section-2


	private static final Collection<String> Safe=new HashSet<>(asList(
			GET, HEAD, OPTIONS, TRACE // https://tools.ietf.org/html/rfc7231#section-4.2.1
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private IRI user=Form.none;
	private Set<Value> roles=singleton(Form.none);

	private String method="";
	private String base="app:/";
	private String path="/";
	private String query="";

	private final Map<String, List<String>> parameters=new LinkedHashMap<>();


	/**
	 * Retrieves the focus item IRI of this request.
	 *
	 * @return the absolute IRI obtained by concatenating {@linkplain #base() base} and {@linkplain #path() path} for
	 * this request
	 */
	@Override public IRI item() {
		return iri(base+path.substring(1));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a response for this request.
	 *
	 * @param mapper the mapping function  used to initialize the new response; must return a non-null value
	 *
	 * @return a new response {@linkplain Response#request() associated} to this request
	 *
	 * @throws NullPointerException if {@code mapper} is null or return a null value
	 */
	public Responder reply(final Function<Response, Response> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		return consumer -> consumer.accept(new Response(this).map(mapper));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks if this request is safe.
	 *
	 * @return {@code true} if this request is <a href="https://tools.ietf.org/html/rfc7231#section-4.2.1">safe</a>,
	 * that is if it's not expected to cause any state change on the origin server ; {@code false} otherwise
	 */
	public boolean safe() {
		return Safe.contains(method);
	}


	/**
	 * Checks if this request if performed by a user in a target set of roles.
	 *
	 * @param roles the target set if roles to be checked
	 *
	 * @return {@code true} if this request is performed by a {@linkplain #user() user} in one of the given {@code
	 * roles}, that is if {@code roles} and  {@linkplain #roles() request roles} are not disjoint
	 */
	public boolean role(final IRI... roles) {
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
	public boolean role(final Collection<IRI> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		return !disjoint(this.roles, roles);
	}


	/**
	 * Retrieves the stem IRI of this request.
	 *
	 * @return the absolute IRI obtained by concatenating {@linkplain #base() base} and {@linkplain #path() path} for
	 * this request and appending a trailing slash if one is not already included in {@linkplain #path() path}
	 */
	public String stem() {
		return base+path.substring(1)+(path.endsWith("/") ? "" : "/");
	}


	/**
	 * Retrieves the shape query of this request.
	 *
	 * @param shape the base shape for the query
	 *
	 * @return a value result providing access to the combined query merging constraints from {@code shape} and the
	 * request {@linkplain #query() query} string, as returned by the {@linkplain QueryParser query parser}; an error
	 * result providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Result<Query, Failure> query(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		try {

			return Value(new QueryParser(shape).parse(Codecs.decode(query())));

		} catch ( final JsonException e ) {

			return Error(new Failure()
					.status(Response.BadRequest)
					.error("query-malformed")
					.cause(e));

		} catch ( final NoSuchElementException e ) {

			return Error(new Failure()
					.status(Response.UnprocessableEntity)
					.error("query-illegal")
					.cause(e));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the identifier of the request user.
	 *
	 * @return an absolute IRI identifying the user performing this request or {@link Form#none} if no user is
	 * authenticated
	 */
	public IRI user() { return user; }

	/**
	 * Configures the the identifier of the request user.
	 *
	 * @param user an absolute IRI identifying the user performing this request
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code user} is null
	 */
	public Request user(final IRI user) {

		if ( user == null ) {
			throw new NullPointerException("null user");
		}

		this.user=user;

		return this;
	}


	/**
	 * Retrieves the roles attributed to the request user.
	 *
	 * @return a set of values uniquely identifying the roles attributed to the request {@linkplain #user() user}
	 */
	public Set<Value> roles() { return unmodifiableSet(roles); }

	/**
	 * Configures the roles attributed to the request user.
	 *
	 * @param roles a collection of values uniquely identifying the roles attributed to the request {@linkplain #user()
	 *              user}
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
	 */
	public Request roles(final Value... roles) {
		return roles(asList(roles));
	}

	/**
	 * Configures the roles attributed to the request user.
	 *
	 * @param roles a collection of values uniquely identifying the roles attributed to the request {@linkplain #user()
	 *              user}
	 *
	 * @return this request
	 *
	 * @throws NullPointerException if {@code roles} is null or contains a {@code null} value
	 */
	public Request roles(final Collection<? extends Value> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		this.roles=new LinkedHashSet<>(roles);

		return this;
	}


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

		this.method=upper(method);

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

		if ( !Values.AbsoluteIRIPattern.matcher(base).matches() ) {
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
	 * @param path the resource path of this request, that is the absolute server path of the linked data resources this
	 *             request refers to
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
	 * Retrieves the query component of this request.
	 *
	 * @return the query component this request
	 */
	public String query() {
		return query;
	}

	/**
	 * Configures the query component of this request.
	 *
	 * @param query the query component of this request
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


	/**
	 * Retrieves request query parameters.
	 *
	 * @return an immutable and possibly empty map from query parameters names to collections of values
	 */
	public Map<String, Collection<String>> parameters() {
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

		return unmodifiableList(parameters.getOrDefault(name, list()));
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
