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
import com.metreeca.rdf.Values;
import com.metreeca.rdf.wrappers.Rewriter;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.*;

import javax.json.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.Either.Right;
import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.BadRequest;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.JSONFormat.context;
import static com.metreeca.core.formats.OutputFormat.output;
import static com.metreeca.rdf.Values.statement;
import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.eclipse.rdf4j.rio.RDFFormat.TURTLE;


/**
 * RDF message format.
 */
public final class RDFFormat extends Format<Collection<Statement>> {

	/**
	 * Custom header providing the external base for on-demand RDF body rewriting.
	 *
	 * @see Rewriter
	 */
	public static final String ExternalBase="-External-Base"; // !!! review/remove/hide


	/**
	 * The plain <a href="http://www.json.org/">JSON</a> file format.
	 *
	 * The file extension {@code .json} is recommend for JSON documents.
	 *
	 * The media type is {@code application/json}.
	 * <br>The character encoding is {@code UTF-8}.
	 */
	public static final org.eclipse.rdf4j.rio.RDFFormat RDFJSONFormat=new org.eclipse.rdf4j.rio.RDFFormat("JSON",
			asList("application/json", "text/json"),
			StandardCharsets.UTF_8,
			singletonList("json"),
			Values.iri("http://www.json.org/"),
			org.eclipse.rdf4j.rio.RDFFormat.NO_NAMESPACES,
			org.eclipse.rdf4j.rio.RDFFormat.NO_CONTEXTS,
			false
	);

	/**
	 * Sets the focus resource for codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Resource> RioFocus=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Focus", "Resource focus", null
	);

	/**
	 * Sets the expected shape for the resources handled by codecs.
	 *
	 * <p>Defaults to {@code null}.</p>
	 */
	public static final RioSetting<Shape> RioShape=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Shape", "Resource shape", null
	);

	/**
	 * Sets the expected JSON-LD context for codecs.
	 *
	 * <p>Defaults to an empty object.</p>
	 */
	public static final RioSetting<JsonObject> RioContext=new RioSettingImpl<>(
			RDFFormat.class.getName()+"#Context", "JSON-LD context", JsonValue.EMPTY_JSON_OBJECT
	);


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
	 * @param customizer the RDF parsers/writers customizer; takes as argument a customizable RDF codec
	 *
	 * @return a new customized RDF message format
	 */
	public static RDFFormat rdf(final Consumer<Codec> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		return new RDFFormat(customizer);
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

	private final Consumer<Codec> customizer;

	private final JsonObject context=Context.asset(context());


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
		return message.body(input()).fold(

				error -> Left(status(UnsupportedMediaType, "no RDF body")), source -> {

					final IRI focus=Values.iri(message.item());
					final Shape shape=message.shape();

					final String base=focus.stringValue();
					final String type=message.header("Content-Type").orElse("");

					final RDFParser parser=Formats
							.service(RDFParserRegistry.getInstance(), TURTLE, type)
							.getParser();

					parser.set(RioShape, shape);
					parser.set(RioFocus, focus);
					parser.set(RioContext, context);

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

					final String internal=message.request().base();
					final String external=message.header(ExternalBase).orElse(internal); // made available by Rewriter

					parser.setRDFHandler(external.equals(internal) ? new AbstractRDFHandler() {

						@Override public void handleStatement(final Statement statement) {
							model.add(statement);
						}

					} : new AbstractRDFHandler() {

						@Override public void handleStatement(final Statement statement) {
							model.add(rewrite(external, internal, statement));
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

				}

		);
	}

	/**
	 * Configures {@code message} {@code Content-Type} header, unless already defined, and encodes
	 * the RDF {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body,
	 * taking into account the RDF serialization selected according to the {@code Accept} header of the {@code message}
	 * originating request, defaulting to {@code text/turtle}
	 */
	@Override public <M extends Message<M>> M encode(final M message, final Collection<Statement> value) {

		final List<String> types=Formats.types(message.request().headers("Accept"));

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

					final IRI focus=Values.iri(message.item());
					final Shape shape=message.shape();

					final String base=focus.stringValue();

					final String internal=message.request().base();
					final String external=message.header(ExternalBase).orElse(internal); // made available by Rewriter

					try {

						final RDFWriter writer=factory.getWriter(output, base); // relativize IRIs wrt the response
						// focus

						writer.set(RioShape, shape);
						writer.set(RioFocus, focus);
						writer.set(RioContext, context);

						customizer.accept(new Codec() {
							@Override public <T> Codec set(final RioSetting<T> setting, final T value) {

								writer.set(setting, value);

								return this;

							}
						});

						Rio.write(external.equals(internal) ? value : rewrite(internal, external, value), writer);

					} catch ( final URISyntaxException e ) {
						throw new UnsupportedOperationException("unsupported base IRI {"+base+"}", e);
					}

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Iterable<Statement> rewrite(
			final String source, final String target, final Iterable<Statement> statements) {
		return () -> new Iterator<Statement>() {

			private final Iterator<Statement> iterator=statements.iterator();

			@Override public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override public Statement next() {
				return rewrite(source, target, iterator.next());
			}

		};
	}


	private Statement rewrite(final String source, final String target, final Statement statement) {
		return statement == null ? null : statement(
				rewrite(source, target, statement.getSubject()),
				rewrite(source, target, statement.getPredicate()),
				rewrite(source, target, statement.getObject()),
				rewrite(source, target, statement.getContext())
		);
	}

	private Value rewrite(final String source, final String target, final Value value) {
		return value instanceof IRI ? rewrite(source, target, (IRI)value) : value;
	}

	private Resource rewrite(final String source, final String target, final Resource resource) {
		return resource instanceof IRI ? rewrite(source, target, (IRI)resource) : resource;
	}

	private IRI rewrite(final String source, final String target, final IRI iri) {
		return iri == null ? null : Values.iri(rewrite(source, target, iri.stringValue()));
	}

	private String rewrite(final String source, final String target, final String string) {
		return string.startsWith(source) ? target+string.substring(source.length()) : string;
	}


	///// !!! /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public List<IRI> path(final String base, final Shape shape, final String path) {
		return new RDFJSONDecoder(base, context) {}.path(path, shape); // !!! pass base as argument and factor decoder
		// instance
	}

	@Override public Object value(final String base, final Shape shape, final JsonValue value) {
		// !!! pass base as argument and factor decoder instance
		return new RDFJSONDecoder(base, context) {}.value(value, shape, null).getKey();
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
	public static IRI iri(final Object object) {
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
	public static Value value(final Object object) {
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

}
