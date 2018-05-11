/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next;

import com.metreeca.jeep.IO;
import com.metreeca.jeep.JSON;
import com.metreeca.jeep.rdf.Formats;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.codecs.JSONAdapter;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.json.JsonException;

import static com.metreeca.jeep.Jeep.title;
import static com.metreeca.jeep.Jeep.upper;
import static com.metreeca.jeep.rdf.Values.iri;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


public final class Request {

	/**
	 * The name of the part containing the main body payload in multipart/form-data requests ({@code {@value}}).
	 */
	public static final String BodyPart="body";

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


	private IRI user=Spec.none;
	private Set<Value> roles=singleton(Spec.none);

	private String method="";
	private String base="app:/";
	private String path="/";
	private String query="";

	private Map<String, List<String>> parameters=new LinkedHashMap<>();
	private Map<String, List<String>> headers=new LinkedHashMap<>();
	private Map<String, List<Part>> parts=new LinkedHashMap<>();

	private Supplier<InputStream> input;
	private Supplier<Reader> reader;


	private Request() {}


	public boolean safe() {
		return Safe.contains(method);
	}

	public boolean interactive() {
		return method.equals(GET) && headers("Accept").stream().anyMatch(Link::interactive);
	}


	public boolean role(final IRI... roles) {
		return role(asList(roles));
	}

	public boolean role(final Collection<IRI> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		return !disjoint(this.roles, roles);
	}


	/**
	 * Retrieves the identifier of the request user.
	 *
	 * @return an IRI uniquely associated with the user or {@link Spec#none} if no user is authenticated
	 */
	public IRI user() { return user; }


	/**
	 * Retrieves the roles attributed to the request user.
	 *
	 * @return a set of values uniquely identifying the roles attributed to the {@linkplain #user() user}
	 */
	public Set<Value> roles() { return unmodifiableSet(roles); }


	public IRI focus() {
		return iri(base+path.substring(1));
	}


	public String stem() {
		return base+path.substring(1)+(path.endsWith("/") ? "" : "/");
	}


	public String method() { return method; }

	public String base() { return base; }

	public String path() { return path; }

	public String query() {
		return query;
	}


	public Stream<Entry<String, List<String>>> parameters() {
		return parameters.entrySet().stream();
	}

	public List<String> parameters(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return parameters.getOrDefault(name, emptyList());
	}

	public Optional<String> parameter(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return parameters(name).stream().findFirst();
	}


	public Stream<Entry<String, List<String>>> headers() {
		return headers.entrySet().stream();
	}

	public List<String> headers(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers.getOrDefault(title(name), emptyList());
	}

	public Optional<String> header(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return headers(name).stream().findFirst();
	}


	public List<Part> parts(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return parts.getOrDefault(name, emptyList());
	}

	public Optional<Part> part(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return parts(name).stream().findFirst();
	}


	private String content() {
		return part(BodyPart)
				.map(p -> p.header("Content-Type").orElse(""))
				.orElseGet(() -> header("Content-Type").orElse(""));
	}


	public InputStream input() {
		return part(BodyPart).map(Part::input).orElseGet(()
				-> input != null ? input.get()
				: reader != null ? IO.input(reader.get())
				: IO.input());
	}

	public Reader reader() {
		return part(BodyPart).map(Part::reader).orElseGet(()
				-> reader != null ? reader.get()
				: input != null ? IO.reader(input.get())
				: IO.reader());
	}


	public byte[] data() {
		try (final InputStream input=input()) {
			return IO.data(input);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	public String text() {
		try (final Reader reader=reader()) {
			return IO.text(reader);
		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	public Object json() throws JsonException {
		return JSON.decode(text());
	}


	public Collection<Statement> rdf() throws RDFParseException {
		return rdf(null);
	}

	public Collection<Statement> rdf(final Shape shape) throws RDFParseException {
		return rdf(shape, focus());
	}

	public Collection<Statement> rdf(final Shape shape, final Resource focus) throws RDFParseException {

		final String content=content();

		final RDFParserFactory factory=Formats.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);
		final RDFParser parser=factory.getParser();

		parser.set(JSONAdapter.Shape, shape);
		parser.set(JSONAdapter.Focus, focus);

		final ParseErrorCollector errorCollector=new ParseErrorCollector();

		parser.setParseErrorListener(errorCollector);

		final Collection<Statement> model=new ArrayList<>();

		parser.setRDFHandler(new AbstractRDFHandler() {
			@Override public void handleStatement(final Statement statement) {
				model.add(statement);
			}
		});

		try (final Reader reader=reader()) { // use reader to activate IRI rewriting

			parser.parse(reader, focus.stringValue()); // resolve relative IRIs wrt the focus

		} catch ( final RDFParseException e ) {

			if ( errorCollector.getFatalErrors().isEmpty() ) { // exception possibly not reported by parser…
				errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
			}

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}

		// !!! log warnings/error/fatals

		final List<String> fatals=errorCollector.getFatalErrors();
		final List<String> errors=errorCollector.getErrors();
		final List<String> warnings=errorCollector.getWarnings();

		if ( fatals.isEmpty() ) {

			return model;

		} else {

			throw new RDFParseException("errors parsing content as "+parser.getRDFFormat().getDefaultMIMEType()+":\n\n"
					+fatals.stream().collect(joining("\n"))
					+errors.stream().collect(joining("\n"))
					+warnings.stream().collect(joining("\n")));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Writer {

		private final Request request=new Request();

		private Consumer<Request> source;


		Writer(final Consumer<Request> source) {
			this.source=source;
		}


		public Writer copy(final Request request) {

			if ( request == null ) {
				throw new NullPointerException("null request");
			}

			final Request target=this.request;

			target.user=request.user;
			target.roles=new LinkedHashSet<>(request.roles);

			target.method=request.method;
			target.base=request.base;
			target.path=request.path;
			target.query=request.query;

			target.parameters=new LinkedHashMap<>(request.parameters);
			target.headers=new LinkedHashMap<>(request.headers);
			target.parts=new LinkedHashMap<>(request.parts);

			target.input=request.input;
			target.reader=request.reader;

			return this;
		}


		public Writer user(final IRI user) {

			if ( user == null ) {
				throw new NullPointerException("null user");
			}

			request.user=user;

			return this;
		}

		public Writer roles(final Value... roles) {
			return roles(asList(roles));
		}

		public Writer roles(final Collection<? extends Value> roles) {

			if ( roles == null ) {
				throw new NullPointerException("null roles");
			}

			if ( roles.contains(null) ) {
				throw new NullPointerException("null role");
			}

			request.roles=new LinkedHashSet<>(roles);

			return this;
		}


		public Writer method(final String method) {

			if ( method == null ) {
				throw new NullPointerException("null method");
			}

			request.method=upper(method);

			return this;
		}

		public Writer base(final String base) {

			if ( base == null ) {
				throw new NullPointerException("null base");
			}

			if ( !base.matches("^\\w+:.*") ) {
				throw new IllegalArgumentException("not an absolute IRI base");
			}

			if ( !base.endsWith("/") ) {
				throw new IllegalArgumentException("missing trailing / in base");
			}

			request.base=base;

			return this;
		}

		public Writer path(final String path) {

			if ( path == null ) {
				throw new NullPointerException("null path");
			}

			if ( !path.startsWith("/") ) {
				throw new IllegalArgumentException("missing leading / in path");
			}

			request.path=path;

			return this;
		}

		public Writer query(final String query) {

			if ( query == null ) {
				throw new NullPointerException("null query");
			}

			request.query=query;

			return this;
		}


		public Writer parameters(final Stream<Entry<String, List<String>>> parameters) {

			if ( parameters == null ) {
				throw new NullPointerException("null parameters");
			}

			parameters.forEachOrdered(parameter -> parameter(parameter.getKey(), parameter.getValue()));

			return this;
		}

		public Writer parameter(final String name, final String... values) {
			return parameter(name, asList(values));
		}

		public Writer parameter(final String name, final Collection<String> values) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			if ( values.contains(null) ) {
				throw new NullPointerException("null value");
			}

			request.parameters.compute(name, (key, current) -> unmodifiableList(values.stream().flatMap(
					value -> value.isEmpty() ? current == null ? Stream.empty() : current.stream() : Stream.of(value)
			).collect(toList())));

			return this;
		}


		public Writer headers(final Stream<Entry<String, Collection<String>>> headers) {

			if ( headers == null ) {
				throw new NullPointerException("null headers");
			}

			headers.forEachOrdered(header -> header(header.getKey(), header.getValue()));

			return this;
		}

		public Writer header(final String name, final String... values) {
			return header(name, asList(values));
		}

		public Writer header(final String name, final Collection<String> values) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			if ( values.contains(null) ) {
				throw new NullPointerException("null value");
			}

			request.headers.compute(title(name), (key, current) -> unmodifiableList(values.stream().flatMap(
					value -> value.isEmpty() ? current == null ? Stream.empty() : current.stream() : Stream.of(value)
			).distinct().collect(toList())));

			return this;
		}


		public Writer part(final String name, final Part... parts) {
			return part(name, asList(parts));
		}

		public Writer part(final String name, final Collection<Part> parts) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			if ( parts == null ) {
				throw new NullPointerException("null parts");
			}

			if ( parts.contains(null) ) {
				throw new NullPointerException("null part");
			}

			request.parts.put(name, unmodifiableList(new ArrayList<>(parts)));

			return this;
		}


		public Writer input(final Supplier<InputStream> input) {

			if ( input == null ) {
				throw new NullPointerException("null input");
			}

			request.input=input;
			request.reader=null;

			return this;
		}

		public Writer reader(final Supplier<Reader> reader) {

			if ( reader == null ) {
				throw new NullPointerException("null reader");
			}

			request.input=null;
			request.reader=reader;

			return done();
		}


		public Writer body() {
			return body(IO::input, IO::reader); // empty body
		}

		public Writer body(final Supplier<InputStream> data, final Supplier<Reader> text) {

			if ( data == null ) {
				throw new NullPointerException("null data");
			}

			if ( text == null ) {
				throw new NullPointerException("null text");
			}

			request.input=data;
			request.reader=text;

			return done();
		}


		public Writer data(final byte... data) {

			if ( data == null ) {
				throw new NullPointerException("null data");
			}

			return input(() -> new ByteArrayInputStream(data));
		}

		public Writer text(final String text) {

			if ( text == null ) {
				throw new NullPointerException("null text");
			}

			return reader(() -> new StringReader(text));
		}


		public Writer json(final Object json) throws JsonException {

			if ( json == null ) {
				throw new NullPointerException("null json");
			}

			return header("Content-Type", "application/json").text(JSON.encode(json));
		}


		public Writer done() {

			if ( source == null ) {
				throw new IllegalStateException("already committed");
			}

			if ( request.method.isEmpty() ) {
				throw new IllegalStateException("undefined method");
			}

			try {
				source.accept(request);
			} finally {
				source=null;
			}

			return this;
		}

	}

}
