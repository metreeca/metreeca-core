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
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.*;

import javax.json.Json;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.Result.error;
import static com.metreeca.form.Result.value;


/**
 * RDF body format.
 */
public final class _RDF implements Format<Collection<Statement>> {

	/**
	 * The singleton RDF body format.
	 */
	public static final Format<Collection<Statement>> Format=new _RDF();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _RDF() {} // singleton


	/**
	 * @return the optional RDF body representation of {@code message}, as retrieved from its {@link _Input#Format}
	 * representation, if present; an empty optional, otherwise
	 */
	@Override public Result<Collection<Statement>, Failure> get(final Message<?> message) {
		return message.body(ReaderFormat.asReader).value(supplier -> { // use reader to activate IRI rewriting

			final Optional<Request> request=message.as(Request.class);

			final Optional<IRI> focus=request.map(Request::item);
			final Optional<Shape> shape=message.body(_Shape.Format).value();

			final String type=request.flatMap(r -> r.header("content-type")).orElse("");

			final RDFParser parser=Formats
					.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, type)
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

			try (final Reader reader=supplier.get()) {

				parser.parse(reader, focus.map(Value::stringValue).orElse("")); // resolve relative IRIs wrt the focus

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
	 * Configures the {@link _Output#Format} representation of {@code message} to write the RDF {@code value} to the
	 * accepted output stream.
	 */
	@Override public <T extends Message<T>> T set(final T message, final Collection<Statement> value) {

		final Optional<Response> response=message.as(Response.class);

		final List<String> types=Formats.types(response.map(r -> r.request().headers("Accept")).orElse(list()));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);

		// try to set content type to the actual type requested even if it's not the default one

		message.header("Content-Type", types.stream()
				.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
				.findFirst()
				.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()));

		message.body(_Writer.Format, writer -> { // use writer to activate IRI rewriting

			final RDFWriter rdf=factory.getWriter(writer);

			rdf.set(JSONAdapter.Shape, message.body(_Shape.Format).value().orElse(null));
			rdf.set(JSONAdapter.Focus, response.map(Response::item).orElse(null));

			Rio.write(value, rdf);

		});

		return message;
	}

}
