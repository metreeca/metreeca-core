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

package com.metreeca.next._work;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.codecs.JSONAdapter;
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.things.Formats;
import com.metreeca.next.*;
import com.metreeca.next.formats.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.json.JsonObject;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.next.Rest.error;

import static java.util.Collections.emptySet;


/**
 * Model-driven RDF body manager.
 */
public final class Driver implements Wrapper {

	private Shape shape=and();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Driver shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> before(request).apply(
				value -> handler.handle(value).map(this::after),
				error -> request.response().status(Response.BadRequest).body(JSON.Format, error)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Request, JsonObject> before(final Request request) {
		return request.body(_Reader.Format).map(supplier -> {

			final IRI focus=request.item();
			final String type=request.header("content-type").orElse("");

			final RDFParser parser=Formats
					.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, type)
					.getParser();

			parser.set(JSONAdapter.Shape, shape);
			parser.set(JSONAdapter.Focus, focus);

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

			try (final Reader reader=supplier.get()) { // use reader to activate IRI rewriting

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

				model.addAll(shape.accept(mode(Form.verify)).accept(new Outliner(focus))); // shape-implied statements

				return new Result<Request, JsonObject>() {
					@Override public <R> R apply(final Function<Request, R> value, final Function<JsonObject, R> error) {
						return value.apply(request.body(_RDF.Format, new Crate(focus, shape, model)));
					}
				};

			} else { // !!! structured json error report

				return new Result<Request, JsonObject>() {
					@Override public <R> R apply(final Function<Request, R> value, final Function<JsonObject, R> error) {
						return error.apply(error("data-malformed", new RDFParseException("errors parsing content as "
								+parser.getRDFFormat().getDefaultMIMEType()+":\n\n"
								+String.join("\n", fatals)
								+String.join("\n", errors)
								+String.join("\n", warnings))));
					}
				};

			}

		}).orElseGet(() -> new Result<Request, JsonObject>() {
			@Override public <R> R apply(final Function<Request, R> value, final Function<JsonObject, R> error) {
				return value.apply(request.body(_RDF.Format, new Crate(request.item(), shape, emptySet())));
			}
		});

	}

	private Response after(final Response response) {
		return response.body(_RDF.Format).map(crate -> {

			final List<String> types=Formats.types(response.request().headers("Accept"));

			final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
			final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);

			// try to set content type to the actual type requested even if it's not the default one

			return response

					.header("Content-Type", types.stream()
							.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
							.findFirst()
							.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()))

					.body(_Writer.Format, writer -> { // use writer to activate IRI rewriting

						final RDFWriter rdf=factory.getWriter(writer);

						rdf.set(JSONAdapter.Shape, crate.shape());
						rdf.set(JSONAdapter.Focus, crate.focus());

						Rio.write(crate.model(), rdf);

					});

		}).orElse(response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static interface Result<V, E> {

		public <R> R apply(final Function<V, R> value, final Function<E, R> error);

	}

}
