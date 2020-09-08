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

package com.metreeca.rdf.formats;

import com.metreeca.core.*;
import com.metreeca.core.formats.InputFormat;
import com.metreeca.core.formats.OutputFormat;

import org.eclipse.rdf4j.common.lang.FileFormat;
import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;
import org.eclipse.rdf4j.rio.helpers.ParseErrorCollector;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.Message.types;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;
import static com.metreeca.rdf.Values.iri;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;


/**
 * RDF message format.
 */
public final class RDFFormat extends Format<Collection<Statement>> {

	/**
	 * Locates a file format service in a registry.
	 *
	 * @param registry the registry the file format service is to be located from
	 * @param fallback the fallback file format to be used as key if no service in the registry is matched by one of
	 *                 the suggested MIME {@code types}
	 * @param types    the suggested MIME types for the file format service to be located
	 * @param <F>      the type of the file format services listed by the {@code registry}
	 * @param <S>      the type of the file file format service to be located
	 *
	 * @return the located file format service
	 *
	 * @throws NullPointerException if any parameter is null
	 */
	public static <F extends FileFormat, S> S service(
			final FileFormatServiceRegistry<F, S> registry, final F fallback, final Collection<String> types
	) {

		if ( registry == null ) {
			throw new NullPointerException("null registry");
		}

		if ( fallback == null ) {
			throw new NullPointerException("null fallback");
		}

		if ( types == null || types.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null types");
		}

		final Function<String, Optional<F>> matcher=prefix -> {

			for (final F format : registry.getKeys()) { // first try to match with the default MIME type
				if ( format.getDefaultMIMEType().toLowerCase(Locale.ROOT).startsWith(prefix) ) {
					return Optional.of(format);
				}
			}

			for (final F format : registry.getKeys()) { // try alternative MIME types too
				for (final String type : format.getMIMETypes()) {
					if ( type.toLowerCase(Locale.ROOT).startsWith(prefix) ) {
						return Optional.of(format);
					}
				}
			}

			return Optional.empty();

		};

		return registry

				.get(types.stream()

						.map(type -> type.equals("*/*") ? Optional.of(fallback)
								: type.endsWith("/*") ? matcher.apply(type.substring(0, type.indexOf('/')+1))
								: registry.getFileFormatForMIMEType(type)
						)

						.filter(Optional::isPresent)
						.map(Optional::get)
						.findFirst()

						.orElse(fallback)
				)

				.orElseThrow(() -> new IllegalArgumentException(String.format(
						"unsupported fallback format <%s>", fallback.getDefaultMIMEType()
				)));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates an RDF message format.
	 *
	 * @return a new RDF message format
	 */
	public static RDFFormat rdf() {
		return rdf(codec -> {});
	}

	/**
	 * Creates a customized RDF message format.
	 *
	 * @param customizer the RDF parser/writer customizer; takes as argument a customizable RIO configuration
	 *
	 * @return a new customized RDF message format
	 */
	public static RDFFormat rdf(final Consumer<RioConfig> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		return new RDFFormat(customizer);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Consumer<RioConfig> customizer;


	private RDFFormat(final Consumer<RioConfig> customizer) {
		this.customizer=customizer;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the RDF {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available, taking into account the RDF serialization format defined by the {@code message}
	 * {@code Content-Type} header, defaulting to {@code text/turtle}
	 */
	@Override public Either<MessageException, Collection<Statement>> decode(final Message<?> message) {
		return message.body(input()).fold(error -> Left(status(UnsupportedMediaType, "no RDF body")), source -> {

			final IRI focus=iri(message.item());

			final String base=focus.stringValue();
			final String type=message.header("Content-Type").orElse("");

			final RDFParser parser=service(RDFParserRegistry.getInstance(), TURTLE, types(type)).getParser();

			customizer.accept(parser.getParserConfig());

			final ParseErrorCollector errorCollector=new ParseErrorCollector();

			parser.setParseErrorListener(errorCollector);

			final Collection<Statement> model=new LinkedHashModel(); // order-preserving and writable

			parser.setRDFHandler(new AbstractRDFHandler() {

				@Override public void handleStatement(final Statement statement) {
					model.add(statement);
				}

			});

			try ( final InputStream input=source.get() ) {

				parser.parse(input, base); // resolve relative IRIs wrt the request focus

			} catch ( final RDFParseException e ) {

				if ( errorCollector.getFatalErrors().isEmpty() ) { // exception not always reported by parser…
					errorCollector.fatalError(e.getMessage(), e.getLineNumber(), e.getColumnNumber());
				}

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}

			final List<String> fatals=errorCollector.getFatalErrors();
			final List<String> errors=errorCollector.getErrors();
			final List<String> warnings=errorCollector.getWarnings();

			if ( fatals.isEmpty() ) { // return model

				return Right(model);

			} else { // report errors // !!! log warnings/error/fatals?

				final JsonObjectBuilder trace=Json.createObjectBuilder()

						.add("format", parser.getRDFFormat().getDefaultMIMEType());

				if ( !fatals.isEmpty() ) { trace.add("fatals", Json.createArrayBuilder(fatals)); }
				if ( !errors.isEmpty() ) { trace.add("errors", Json.createArrayBuilder(errors)); }
				if ( !warnings.isEmpty() ) { trace.add("warnings", Json.createArrayBuilder(warnings)); }

				return Left(status(BadRequest, trace.build()));

			}

		});
	}

	/**
	 * Configures {@code message} {@code Content-Type} header, unless already defined, and encodes
	 * the RDF {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the RDF serialization selected according to the {@code Accept} header of the {@code message}
	 * originating request, defaulting to {@code text/turtle}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

		final List<String> types=types(message.request().header("Accept").orElse(""));

		final RDFWriterRegistry registry=RDFWriterRegistry.getInstance();
		final RDFWriterFactory factory=service(registry, TURTLE, types);

		return message

				// try to set content type to the actual type requested even if it's not the default one

				.header("~Content-Type", types.stream()
						.filter(type -> registry.getFileFormatForMIMEType(type).isPresent())
						.findFirst()
						.orElseGet(() -> factory.getRDFFormat().getDefaultMIMEType())
				)

				.body(output(), output -> {

					final IRI focus=iri(message.item());
					final String base=focus.stringValue(); // relativize IRIs wrt the response focus

					try {

						final RDFWriter writer=factory.getWriter(output, base);

						customizer.accept(writer.getWriterConfig());

						Rio.write(value, writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("malformed base IRI {"+base+"}", e);
					}

				});
	}

}
