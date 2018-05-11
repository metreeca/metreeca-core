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

import com.metreeca.jeep.JSON;
import com.metreeca.jeep.rdf.Formats;
import com.metreeca.jeep.rdf.Values;
import com.metreeca.spec.Shape;
import com.metreeca.spec.codecs.JSONAdapter;
import com.metreeca.tray.IO;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.TreeModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.json.JsonException;

import static com.metreeca.jeep.Jeep.title;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;


public final class Response {

	public static final int OK=200; // https://tools.ietf.org/html/rfc7231#section-6.3.1
	public static final int Created=201; // https://tools.ietf.org/html/rfc7231#section-6.3.2
	public static final int Accepted=202; // https://tools.ietf.org/html/rfc7231#section-6.3.3
	public static final int NoContent=204; // https://tools.ietf.org/html/rfc7231#section-6.3.5

	public static final int MovedPermanently=301; // https://tools.ietf.org/html/rfc7231#section-6.4.2
	public static final int SeeOther=303; // https://tools.ietf.org/html/rfc7231#section-6.4.4

	public static final int BadRequest=400; // https://tools.ietf.org/html/rfc7231#section-6.5.1
	public static final int Unauthorized=401; // https://tools.ietf.org/html/rfc7235#section-3.1
	public static final int Forbidden=403; // https://tools.ietf.org/html/rfc7231#section-6.5.3
	public static final int NotFound=404; // https://tools.ietf.org/html/rfc7231#section-6.5.4
	public static final int MethodNotAllowed=405; // https://tools.ietf.org/html/rfc7231#section-6.5.5
	public static final int Conflict=409; // https://tools.ietf.org/html/rfc7231#section-6.5.8
	public static final int UnprocessableEntity=422; // https://tools.ietf.org/html/rfc4918#section-11.2

	public static final int InternalServerError=500; // https://tools.ietf.org/html/rfc7231#section-6.6.1
	public static final int NotImplemented=501; // https://tools.ietf.org/html/rfc7231#section-6.6.2
	public static final int BadGateway=502; // https://tools.ietf.org/html/rfc7231#section-6.6.3
	public static final int ServiceUnavailable=503; // https://tools.ietf.org/html/rfc7231#section-6.6.4
	public static final int GatewayTimeout=504; // https://tools.ietf.org/html/rfc7231#section-6.6.5


	private final Request request;

	private Consumer<Reader> target;

	private int status;
	private Throwable cause;

	private Map<String, List<String>> headers=new LinkedHashMap<>();

	private Consumer<OutputStream> output;
	private Consumer<Writer> writer;


	Response(final Request request, final Consumer<Reader> target) {
		this.request=request;
		this.target=target;
	}


	public Response copy(final Reader reader) {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		final Response source=reader.response;

		status=source.status;
		cause=source.cause;

		headers=new LinkedHashMap<>(source.headers);

		output=source.output;
		writer=source.writer;

		return this;
	}


	public boolean committed() {
		return target == null;
	}


	public Response status(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status ["+status+"]");
		}

		this.status=status;

		return this;
	}

	public Response cause(final Throwable cause) {

		this.cause=cause;

		return this;
	}


	public Response headers(final Stream<Entry<String, Collection<String>>> headers) {

		if ( headers == null ) {
			throw new NullPointerException("null headers");
		}

		headers.forEachOrdered(header -> header(header.getKey(), header.getValue()));

		return this;
	}

	public Response header(final String name, final String... values) {return header(name, asList(values));}

	public Response header(final String name, final Collection<String> values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		headers.compute(title(name), (key, current) -> unmodifiableList(values.stream().flatMap(
				value -> value.isEmpty() ? current == null ? Stream.empty() : current.stream() : Stream.of(value)
		).distinct().collect(toList())));

		return this;
	}


	public Response output(final Consumer<OutputStream> output) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		this.output=output;
		this.writer=null;

		return done();
	}

	public Response writer(final Consumer<Writer> writer) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		this.output=null;
		this.writer=writer;

		return done();
	}


	public Response data(final byte... data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		return output(output -> {
			try (final OutputStream o=output) {
				o.write(data);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	public Response text(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return writer(writer -> {
			try (final Writer w=writer) {
				w.write(text);
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}


	public Response json(final Object json) throws JsonException {

		if ( json == null ) {
			throw new NullPointerException("null json");
		}

		return header("Content-Type", "application/json;charset=UTF-8").text(JSON.encode(json));
	}


	public Response rdf(final Iterable<Statement> model) {
		return rdf(model, null);
	}

	public Response rdf(final Iterable<Statement> model, final Shape shape) {
		return rdf(model, shape, request.focus());
	}

	public Response rdf(final Iterable<Statement> model, final Shape shape, final Resource focus) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final List<String> types=Formats.types(request.headers("Accept"));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);

		// try to set content type to the actual type requested even if it's not the default one

		return header("Content-Type", types
				.stream().filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
				.findFirst().orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()))

				.writer(writer -> {
					try (final Writer w=writer) {

						final RDFWriter rdf=factory.getWriter(w);

						rdf.set(JSONAdapter.Shape, shape);
						rdf.set(JSONAdapter.Focus, focus);

						Rio.write(model, rdf);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				});
	}


	public Response done() {

		if ( target == null ) {
			throw new IllegalStateException("already committed");
		}

		if ( status == 0 ) {
			throw new IllegalStateException("undefined status code");
		}

		try {
			target.accept(new Reader(this));
		} finally {
			target=null;
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Reader {

		private final Response response;


		private Reader(final Response response) {
			this.response=response;
		}


		public boolean success() {
			return response.status/100 == 2;
		}

		public boolean error() {
			return response.status/100 >= 4;
		}

		public boolean interactive() {
			return status() == OK && headers("Content-Type").stream().anyMatch(Link::interactive);
		}

		public boolean binary() {
			return response.output != null;
		}

		public boolean textual() {
			return response.writer != null;
		}


		public Request request() {
			return response.request;
		}


		public IRI focus() {
			return header("location")
					.filter(location -> !location.isEmpty())
					.map(Values::iri)
					.orElseGet(() -> request().focus());
		}


		public int status() {
			return response.status;
		}

		public Throwable cause() { return response.cause; }


		public Stream<Entry<String, List<String>>> headers() {
			return response.headers.entrySet().stream();
		}

		public List<String> headers(final String name) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			return response.headers.getOrDefault(title(name), emptyList());
		}

		public Optional<String> header(final String name) {

			if ( name == null ) {
				throw new NullPointerException("null name");
			}

			return headers(name).stream().findFirst();
		}


		public Reader output(final OutputStream output) {

			if ( output == null ) {
				throw new NullPointerException("null output");
			}

			if ( response.output != null ) {

				response.output.accept(output);

			} else if ( response.writer != null ) {

				// !!! user request accept encoding with quality values
				// !!! response.request.header("Accept-Charset");

				response.writer.accept(IO.writer(output, IO.UTF8Enc));

			}

			return this;
		}

		public Reader writer(final Writer writer) {

			if ( writer == null ) {
				throw new NullPointerException("null writer");
			}

			if ( response.writer != null ) {

				response.writer.accept(writer);

			} else if ( response.output != null ) {

				// !!! user request accept encoding with quality values
				// !!! response.request.header("Accept-Charset");

				response.output.accept(IO.output(writer, IO.UTF8Enc)); // !!! user request accept encoding

			}

			return this;
		}


		public byte[] data() {

			final ByteArrayOutputStream output=new ByteArrayOutputStream();

			output(output);

			return output.toByteArray();
		}

		public String text() {

			final StringWriter writer=new StringWriter();

			writer(writer);

			return writer.toString();
		}


		public Object json() {
			return header("content-type").orElse("").startsWith("application/json") ? JSON.decode(text()) : null;
		}


		public Model rdf() {
			return rdf(null);
		}

		public Model rdf(final Shape shape) {
			return rdf(shape, focus());
		}

		public Model rdf(final Shape shape, final Resource focus) {

			final String content=header("Content-Type").orElse("");

			final RDFParserFactory factory=Formats.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);
			final RDFParser parser=factory.getParser();

			parser.set(JSONAdapter.Shape, shape);
			parser.set(JSONAdapter.Focus, focus);

			final Model model=new TreeModel();

			parser.setRDFHandler(new AbstractRDFHandler() {
				@Override public void handleStatement(final Statement statement) { model.add(statement); }
			});

			try {

				parser.parse(new StringReader(text()), focus.stringValue()); // use reader to activate IRI rewriting

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

			return model;
		}

	}

}
