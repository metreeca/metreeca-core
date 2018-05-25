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

package com.metreeca.link;

import com.metreeca.spec.Spec;
import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.io.*;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.metreeca.spec.things.Transputs.data;
import static com.metreeca.spec.things.Transputs.input;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;


/**
 * HTTP request.
 */
public final class _Request extends _Message<_Request> {

	public static final String ANY="*"; // wildcard method name

	public static final String GET="GET"; // https://tools.ietf.org/html/rfc7231#section-4.3.1
	public static final String HEAD="HEAD"; // https://tools.ietf.org/html/rfc7231#section-4.3.2
	public static final String POST="POST"; // https://tools.ietf.org/html/rfc7231#section-4.3.3
	public static final String PUT="PUT"; // https://tools.ietf.org/html/rfc7231#section-4.3.4
	public static final String DELETE="DELETE"; // https://tools.ietf.org/html/rfc7231#section-4.3.5
	public static final String CONNECT="CONNECT"; // https://tools.ietf.org/html/rfc7231#section-4.3.6
	public static final String OPTIONS="OPTIONS"; // https://tools.ietf.org/html/rfc7231#section-4.3.7
	public static final String TRACE="TRACE"; // https://tools.ietf.org/html/rfc7231#section-4.3.8

	private static final Collection<String> Safe=new HashSet<>(asList(
			GET, HEAD, OPTIONS, TRACE // https://tools.ietf.org/html/rfc7231#section-4.2.1
	));


	private static final String URLEncodedForm="application/x-www-form-urlencoded";


	private IRI user=RDF.NIL;
	private Set<IRI> roles=emptySet();


	private String method="";

	private String base="";
	private String target="";
	private String query="";

	// !!! support multipart/form-data (e.g. https://stackoverflow.com/a/3337115/739773)

	private Supplier<Map<String, List<String>>> parameters=() -> parameters(method.equals(POST)
			&& getHeader("Content-Type").orElse("").startsWith(URLEncodedForm) // ignore charset parameter
			? getText() : query);

	private Supplier<InputStream> body=() -> new InputStream() {
		@Override public int read() { return -1; }
	};


	@Override protected _Request self() { return this; }


	public boolean isSafe() {
		return Safe.contains(method);
	}

	public boolean isInteractive() {
		return method.equals(GET) && getHeaders("Accept").stream().anyMatch(h -> h.contains("text/html"));
	}

	public boolean isSysAdm() {
		return roles.contains(Spec.root);
	}


	/**
	 * Retrieves the identifier of the request user.
	 *
	 * @return an IRI uniquely associated with the user or {@link RDF#NIL} if no user is authenticated
	 */
	public IRI getUser() {
		return user;
	}

	public _Request setUser(final IRI user) {

		if ( user == null ) {
			throw new NullPointerException("null user");
		}

		this.user=user;

		return this;
	}


	/**
	 * Retrieves the roles attributed to the request user.
	 *
	 * @return a set of IRI uniquely identifying the roles attributed to the {@linkplain #getUser() user}
	 */
	public Set<IRI> getRoles() {
		return unmodifiableSet(roles);
	}

	public _Request setRoles(final Collection<IRI> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		this.roles=new LinkedHashSet<>(roles);

		return this;
	}


	public String getMethod() { // !!! § idempotent /	not null / valid HTTP request method  // https://tools.ietf.org/html/rfc7231#section-4
		return method;
	}

	public _Request setMethod(final String method) {

		if ( method == null ) {
			throw new NullPointerException("null method");
		}

		this.method=method.toUpperCase(Locale.ROOT);

		return this;
	}


	/**
	 * Retrieves the absolute base IRI of the LDP server handling this request.
	 *
	 * @return an absolute IRI including a trailing slash
	 */
	public String getBase() { // !!! § idempotent / not null
		return base;
	}

	public _Request setBase(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.base=base.endsWith("/") ? base : base+"/";

		return this;
	}


	/**
	 * Retrieves the absolute IRI of the target resource of this request.
	 *
	 * @return an absolute IRI under the {@code #getRoot() root} IRI
	 */
	public String getTarget() { // !!! § idempotent / not null
		return target;
	}

	public _Request setTarget(final String target) {    // !!! enforce target.startsWith(root)

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		this.target=target;

		return this;
	}


	public String getQuery() { // !!! § idempotent / not {@code null}
		return query;
	}

	public _Request setQuery(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		if ( !query.equals(this.query) ) {
			this.query=query;
			this.parameters=() -> parameters(query); // clear cached value
		}

		return this;
	}


	public Map<String, List<String>> getParameters() {

		final Map<String, List<String>> parameters=this.parameters.get();

		this.parameters=() -> parameters; // cache values

		return parameters;
	}

	public List<String> getParameters(final String name) {
		return getParameters().getOrDefault(name, emptyList());
	}

	public Optional<String> getParameter(final String name) {
		return getParameters(name).stream().findFirst();
	}


	private Map<String, List<String>> parameters(final String query) {

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

		return Collections.unmodifiableMap(parameters);
	}


	public Supplier<InputStream> getBody() { // !!! § not null / once > possible IllegalStateException
		return body;
	}

	public _Request setBody(final Supplier<InputStream> body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=body;

		return this;
	}

	public _Request mapBody(final UnaryOperator<Supplier<InputStream>> mapper) {

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		this.body=mapper.apply(body);

		return this;
	}


	public byte[] getData() {
		return data(body.get());
	}

	public _Request setData(final byte... data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		return setBody(() -> new ByteArrayInputStream(data));
	}


	public String getText() {
		return new String(getData(), Transputs.UTF8); // !!! use request encoding
	}

	public _Request setText(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return setBody(() -> input(new StringReader(text))); // !!! encoding
	}


	public Graph map(final Graph graph) { // !!! generalize

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		return graph.map(base, stem);
	}

	//// !!! canonical base: remove after migrating to context-based tool injection ////////////////////////////////////

	private String stem="";

	public _Request setStem(final String stem) {

		if ( stem == null ) {
			throw new NullPointerException("null stem");
		}

		this.stem=stem;

		return this;
	}

}
