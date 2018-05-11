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

package com.metreeca.link;

import com.metreeca.spec.Formats;
import com.metreeca.spec.Shape;
import com.metreeca.spec.codecs.JSONAdapter;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.List;

import static com.metreeca.spec.Values.iri;

import static java.util.stream.Collectors.joining;


public final class Transfer {

	private final Request request;
	private final Response response;


	public Transfer(final Request request, final Response response) {

		if ( request == null ) {
			throw new NullPointerException("null request");
		}

		if ( response == null ) {
			throw new NullPointerException("null response");
		}

		this.request=request;
		this.response=response;
	}


	public Collection<Statement> model() {
		return model((Shape)null);
	}

	public Collection<Statement> model(final Shape shape) {
		return model(shape, iri(request.getTarget()));
	}

	public Collection<Statement> model(final Shape shape, final Resource focus) {

		// !!! limit input size? e.g. to prevent DoS attacks in cloud

		final Iterable<String> content=request.getHeaders("Content-Type");

		final StatementCollector statementCollector=new StatementCollector();
		final ParseErrorCollector errorCollector=new ParseErrorCollector();

		final RDFParserFactory factory=Formats.service(RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);
		final RDFParser parser=factory.getParser();

		parser.set(JSONAdapter.Shape, shape);
		parser.set(JSONAdapter.Focus, focus);

		parser.setRDFHandler(statementCollector);
		parser.setParseErrorListener(errorCollector);

		try {

			parser.parse(request.getBody().get(), focus.stringValue());

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		} catch ( final RDFParseException e ) {

			if ( errorCollector.getFatalErrors().isEmpty() ) { // exception possibly not reported by parser…
				errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
			}

		} catch ( final RuntimeException e ) {

			throw e; // !!! log/handle

		}

		// !!! log warnings/error/fatals

		final List<String> fatals=errorCollector.getFatalErrors();
		final List<String> errors=errorCollector.getErrors();
		final List<String> warnings=errorCollector.getWarnings();

		if ( fatals.isEmpty() ) {

			return statementCollector.getStatements();

		} else {

			throw new LinkException(Response.BadRequest,
					"errors parsing content as "+parser.getRDFFormat().getDefaultMIMEType()+":\n\n"
							+fatals.stream().collect(joining("\n"))
							+errors.stream().collect(joining("\n"))
							+warnings.stream().collect(joining("\n")));

		}
	}


	public Transfer model(final Iterable<Statement> model) {
		return model(model, null);
	}

	public Transfer model(final Iterable<Statement> model, final Shape shape) {
		return model(model, shape, iri(request.getTarget()));
	}

	public Transfer model(final Iterable<Statement> model, final Shape shape, final Resource focus) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final List<String> types=Formats.types(request.getHeaders("Accept"));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=Formats.service(registry, RDFFormat.TURTLE, types);

		response.setStatus(Response.OK)

				// try to set content type to the actual type requested even if it's not the default one

				.addHeader("Content-Type", types
						.stream().filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
						.findFirst().orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType()))

				.setBody(output -> {

					final RDFWriter writer=factory.getWriter(output);

					writer.set(JSONAdapter.Shape, shape);
					writer.set(JSONAdapter.Focus, focus);

					Rio.write(model, writer);

				});

		return this;
	}

}
