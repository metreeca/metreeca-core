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


import com.metreeca.rdf.Values;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collection;

import javax.json.*;
import javax.json.stream.JsonParsingException;

import static com.metreeca.rdf.formats.RDFFormat.*;
import static com.metreeca.rdf.formats.RDFJSONCodec.driver;
import static com.metreeca.tree.Shape.pass;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;


/**
 * Idiomatic RDF/JSON {@linkplain RDFParser parser}.
 */
public final class RDFJSONParser extends AbstractRDFParser {

	private static final JsonReaderFactory readers=Json.createReaderFactory(null);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public RDFFormat getRDFFormat() {
		return RDFJSONFormat;
	}

	@Override public Collection<RioSetting<?>> getSupportedSettings() {

		final Collection<RioSetting<?>> settings=super.getSupportedSettings();

		settings.add(RioFocus);
		settings.add(RioShape);

		return settings;
	}


	@Override public void parse(final InputStream in, final String baseURI)
			throws RDFParseException, RDFHandlerException {

		if ( in == null ) {
			throw new NullPointerException("null input stream");
		}

		if ( baseURI == null ) {
			throw new NullPointerException("null base URI");
		}

		parse(new InputStreamReader(in, UTF_8), baseURI);
	}

	@Override public void parse(final Reader reader, final String baseURI)
			throws RDFParseException, RDFHandlerException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		if ( baseURI == null ) {
			throw new NullPointerException("null base URI");
		}

		final ParserConfig config=getParserConfig();

		final Resource focus=config.get(RioFocus);
		final Shape shape=config.get(RioShape);
		final JsonObject context=config.get(RioContext);

		final Shape driver=shape == null || pass(shape)? null : driver(shape);

		if ( rdfHandler != null ) {

			rdfHandler.startRDF();

			try {

				new Decoder(baseURI, context)

						.values(readers.createReader(reader).readValue(), driver, focus)
						.values()
						.stream()
						.flatMap(identity())
						.forEachOrdered(rdfHandler::handleStatement);

			} catch ( final JsonParsingException e ) {

				throw new RDFParseException(e.getMessage(),
						e.getLocation().getLineNumber(), e.getLocation().getColumnNumber());

			} catch ( final JsonException e ) {

				throw new RDFParseException(e.getMessage(), e);

			}

			rdfHandler.endRDF();

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class Decoder extends RDFJSONDecoder {

		private Decoder(final String baseURI, final JsonObject context) {
			super(baseURI, context);
		}

		@Override protected Resource bnode() {
			return createNode();
		}

		@Override protected Resource bnode(final String id) {
			return id.isEmpty() ? createNode() : id.startsWith("_:") ? createNode(id.substring(2)) : createNode(id);
		}

		@Override protected IRI iri(final String iri) {
			return iri.isEmpty() ? Values.iri() : createURI(resolve(iri));
		}

		@Override protected Literal literal(final String text, final IRI type) {
			return createLiteral(text, null, type);
		}

		@Override protected Literal literal(final String text, final String lang) {
			return createLiteral(text, lang, null);
		}

		@Override protected Statement statement(final Resource subject, final IRI predicate, final Value object) {
			return createStatement(subject, predicate, object);
		}

		@Override protected <V> V error(final String message) {

			// !!! report error location

			reportFatalError(message);

			return null; // unreachable
		}

	}

}