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

package com.metreeca.rest.bodies;

import com.metreeca.form.Shape;
import com.metreeca.form.codecs.JSONCodec;
import com.metreeca.form.things.Formats;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.OutputBody.output;

import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;


/**
 * RDF body format.
 */
public final class RDFBody implements Body<Iterable<Statement>> {

	private static final RDFBody Instance=new RDFBody();


	/**
	 * Retrieves the RDF body format.
	 *
	 * @return the singleton RDF body format instance
	 */
	public static RDFBody rdf() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RDFBody() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link InputBody}
	 * representation, if present;  a failure reporting RDF processing errors with the {@link Response#BadRequest}
	 * status, otherwise
	 */
	@Override public Result<Iterable<Statement>, Failure> get(final Message<?> message) {
		return message.body(input()).fold(

				supplier -> {

					final IRI focus=message.item();
					final Shape shape=message.shape();

					final String base=focus.stringValue();
					final String type=message.header("Content-Type").orElse("");

					final RDFParser parser=Formats
							.service(RDFParserRegistry.getInstance(), TURTLE, type)
							.getParser();

					parser.set(JSONCodec.Shape, pass(shape) ? null : shape); // !!! handle empty shape directly in JSONParser
					parser.set(JSONCodec.Focus, focus);

					parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
					parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

					parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
					parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

					final ParseErrorCollector errorCollector=new ParseErrorCollector();

					parser.setParseErrorListener(errorCollector);

					final Collection<Statement> model=new LinkedHashSet<>(); // order-preserving and writable

					parser.setRDFHandler(new AbstractRDFHandler() {
						@Override public void handleStatement(final Statement statement) { model.add(statement); }
					});

					try (final InputStream input=supplier.get()) {

						parser.parse(input, base); // resolve relative IRIs wrt the request focus

					} catch ( final RDFParseException e ) {

						if ( errorCollector.getFatalErrors().isEmpty() ) { // exception possibly not reported by parser…
							errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
						}

					} catch ( final IOException e ) {

						throw new UncheckedIOException(e);

					}

					final List<String> fatals=errorCollector.getFatalErrors();
					final List<String> errors=errorCollector.getErrors();
					final List<String> warnings=errorCollector.getWarnings();

					if ( fatals.isEmpty() ) { // return model

						return Value(model);

					} else { // report errors // !!! log warnings/error/fatals?

						final JsonObjectBuilder trace=Json.createObjectBuilder()

								.add("format", parser.getRDFFormat().getDefaultMIMEType());

						if ( !fatals.isEmpty() ) { trace.add("fatals", Json.createArrayBuilder(fatals)); }
						if ( !errors.isEmpty() ) { trace.add("errors", Json.createArrayBuilder(errors)); }
						if ( !warnings.isEmpty() ) { trace.add("warnings", Json.createArrayBuilder(warnings)); }

						return Error(new Failure()
								.status(Response.BadRequest)
								.error(Failure.BodyMalformed)
								.trace(trace.build()));

					}

				},

				error -> error.equals(Body.Missing)
						? Value(new LinkedHashSet<>()) // order-preserving and writable
						: Error(error)

		);
	}

	/**
	 * Configures the {@link OutputBody} representation of {@code message} to write the RDF {@code value} to the
	 * accepted output stream and sets the {@code Content-Type} header to the MIME type of the RDF serialization
	 * selected according to the {@code Accept} header of the request associated to the message, if one is present, or
	 * to {@code "text/turtle"}, otherwise.
	 */
	@Override public <T extends Message<T>> T set(final T message) {

		final List<String> types=Formats.types(message.request().request().headers("Accept"));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, TURTLE, types);

		return message

				// try to set content type to the actual type requested even if it's not the default one

				.header("Content-Type", types.stream()
						.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
						.findFirst()
						.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType())
				)

				.body(output(), rdf().map(rdf -> target -> {

					final IRI focus=message.item();
					final Shape shape=message.shape();

					final String base=focus.stringValue();

					try (final OutputStream output=target.get()) {

						final RDFWriter writer=factory.getWriter(output, base); // relativize IRIs wrt the response focus

						writer.set(JSONCodec.Shape, pass(shape) ? null : shape); // !!! handle empty shape directly in JSONParser
						writer.set(JSONCodec.Focus, focus);

						Rio.write(rdf, writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("unsupported base IRI {" +base+ "}", e);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}

				}));
	}

}
