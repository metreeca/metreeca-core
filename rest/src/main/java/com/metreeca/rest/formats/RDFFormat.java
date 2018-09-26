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

package com.metreeca.rest.formats;

import com.metreeca.form.Form;
import com.metreeca.form.Result;
import com.metreeca.form.Shape;
import com.metreeca.form.codecs.JSONAdapter;
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.things.Formats;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import javax.json.Json;

import static com.metreeca.form.Result.error;
import static com.metreeca.form.Result.value;
import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.rest.formats.OutputFormat.asOutput;


/**
 * RDF body format.
 */
public final class RDFFormat implements Format<Collection<Statement>> {

	/**
	 * The singleton RDF body format.
	 */
	public static final Format<Collection<Statement>> asRDF=new RDFFormat();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RDFFormat() {} // singleton


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link InputFormat}
	 * representation, if present; an empty optional, otherwise
	 */
	@Override public Result<Collection<Statement>, Failure> get(final Message<?> message) {
		return message.body(InputFormat.asInput).value(supplier -> {

			final Optional<Request> request=message.as(Request.class);

			final Optional<IRI> focus=request.map(Request::item);
			final Optional<Shape> shape=message.body(ShapeFormat.asShape).value();

			final String type=request.flatMap(r -> r.header("Content-Type")).orElse("");

			final RDFParser parser=Formats
					.service(RDFParserRegistry.getInstance(), org.eclipse.rdf4j.rio.RDFFormat.TURTLE, type)
					.getParser();

			parser.set(JSONAdapter.Shape, shape.orElse(null));
			parser.set(JSONAdapter.Focus, focus.orElse(null));

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

			final ParseErrorCollector errorCollector=new ParseErrorCollector();

			parser.setParseErrorListener(errorCollector);

			final Collection<Statement> model=new ArrayList<>();

			parser.setRDFHandler(new AbstractRDFHandler() {
				@Override public void handleStatement(final Statement statement) { model.add(statement); }
			});

			try (final InputStream input=supplier.get()) {

				parser.parse(input, focus.map(Value::stringValue).orElse("")); // resolve relative IRIs wrt the focus

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

			// !!! log warnings/error/fatals

			if ( fatals.isEmpty() ) {

				focus.ifPresent(f -> shape.ifPresent(s ->
						model.addAll(s.accept(mode(Form.verify)).accept(new Outliner(f))) // shape-implied statements
				));

				return value(model);

			} else {

				return error(new Failure()
						.status(Response.BadRequest)
						.error(Failure.BodyMalformed)
						.trace(Json.createObjectBuilder()

								.add("format", parser.getRDFFormat().getDefaultMIMEType())
								.add("fatal", Json.createArrayBuilder(fatals))
								.add("error", Json.createArrayBuilder(errors))
								.add("warning", Json.createArrayBuilder(warnings))

								.build()));

			}

		});
	}

	/**
	 * Configures the {@link OutputFormat} representation of {@code message} to write the RDF {@code value} to the
	 * accepted output stream and sets the {@code Content-Type} header to the MIME type of the selected RDF format.
	 */
	@Override public <T extends Message<T>> T set(final T message) {

		final Optional<Response> response=message.as(Response.class);

		final List<String> types=Formats.types(response.map(r -> r.request().headers("Accept")).orElse(list()));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, org.eclipse.rdf4j.rio.RDFFormat.TURTLE, types);

		// try to set content type to the actual type requested even if it's not the default one

		message.header("Content-Type", types.stream()
				.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
				.findFirst()
				.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()));

		message.body(asOutput, output -> {

			final RDFWriter writer=factory.getWriter(output);

			writer.set(JSONAdapter.Shape, message.body(ShapeFormat.asShape).value().orElse(null));
			writer.set(JSONAdapter.Focus, response.map(Response::item).orElse(null));

			message.body(asRDF).value().ifPresent(model -> Rio.write(model, writer));

		});

		return message;
	}

}
