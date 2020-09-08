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
import com.metreeca.json.Shape;
import com.metreeca.rdf.Formats;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.*;

import javax.json.*;
import java.io.*;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import static com.metreeca.core.Context.asset;
import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.OutputFormat.output;
import static com.metreeca.rdf.Formats.types;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.formats.JSONLDFormat.context;
import static java.lang.Boolean.FALSE;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;


/**
 * RDF message format.
 */
public final class RDFFormat extends Format<Collection<Statement>> {

	///// !!! /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static List<IRI> path(final String base, final Shape shape, final String path) {
		return new JSONLDDecoder(base, asset(context())) {}.path(path, shape); // !!! pass base as argument and
		// factor decoder
		// instance
	}

	public static Object value(final String base, final Shape shape, final JsonValue value) {
		// !!! pass base as argument and factor decoder instance
		return new JSONLDDecoder(base, asset(context())) {}.value(value, shape, null).getKey();
	}


	/**
	 * Converts an object to an IRI.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return an IRI obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to an IRI
	 */
	public static IRI _iri(final Object object) {
		return as(object, IRI.class);
	}

	/**
	 * Converts an object to a value.
	 *
	 * @param object the object to be converted; may be null
	 *
	 * @return a value obtained by converting {@code object} or {@code null} if {@code object} is null
	 *
	 * @throws UnsupportedOperationException if {@code object} cannot be converted to a value
	 */
	public static Value _value(final Object object) {
		return as(object, Value.class);
	}


	private static <T> T as(final Object object, final Class<T> type) {
		if ( object == null || type.isInstance(object) ) {

			return type.cast(object);

		} else {

			throw new UnsupportedOperationException(String.format("unsupported type {%s} / expected %s",
					object.getClass().getName(), type.getName()
			));

		}
	}


	/**
	 * Customizable RDF codec.
	 */
	@FunctionalInterface public static interface Codec { // !!! review

		/**
		 * Configures a setting for this configurable codec
		 *
		 * @param setting the setting to be configured
		 * @param value   the value for {@code setting}
		 * @param <T>     the type of the {@code setting} value
		 *
		 * @return this codec
		 */
		public <T> Codec set(final RioSetting<T> setting, T value);

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
	 * @param customizer the RDF parser/writer customizer; takes as argument a customizable RDF codec
	 *
	 * @return a new customized RDF message format
	 */
	public static RDFFormat rdf(final Consumer<Codec> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		return new RDFFormat(customizer);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Consumer<Codec> customizer;


	private RDFFormat(final Consumer<Codec> customizer) {
		this.customizer=customizer;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the RDF {@code message} body from the input stream supplied by the {@code message} {@link InputFormat}
	 * body, if one is available, taking into account the RDF serialization format defined by the {@code message}
	 * {@code Content-Type} header and defaulting to {@code text/turtle}
	 */
	@Override public Either<MessageException, Collection<Statement>> decode(final Message<?> message) {
		return message.body(input()).fold(error -> Left(status(UnsupportedMediaType, "no RDF body")), source -> {

			final IRI focus=iri(message.item());

			final String base=focus.stringValue();
			final String type=message.header("Content-Type").orElse("");

			final RDFParser parser=Formats
					.service(RDFParserRegistry.getInstance(), TURTLE, types(type))
					.getParser();

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

			customizer.accept(new Codec() {
				@Override public <T> Codec set(final RioSetting<T> setting, final T value) {

					parser.set(setting, value);

					//  ;(dbpedia) ignore malformed rdf:langString literals
					//  (https://github.com/eclipse/rdf4j/issues/2004)

					if ( BasicParserSettings.VERIFY_DATATYPE_VALUES.equals(setting) && FALSE.equals(value) ) {
						parser.setValueFactory(new SimpleValueFactory() {

							@Override public Literal createLiteral(final String value, final IRI datatype) {
								return RDF.LANGSTRING.equals(datatype) ?
										createLiteral(value) : super.createLiteral(value, datatype);
							}

						});
					}

					return this;

				}
			});

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
		final RDFWriterFactory factory=Formats.service(registry, TURTLE, types);

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

						customizer.accept(new Codec() {
							@Override public <T> Codec set(final RioSetting<T> setting, final T value) {

								writer.set(setting, value);

								return this;

							}
						});

						Rio.write(value, writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("malformed base IRI {"+base+"}", e);
					}

				});
	}

}
