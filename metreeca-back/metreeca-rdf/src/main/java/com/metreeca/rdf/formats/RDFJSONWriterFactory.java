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

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;

/**
 * Idiomatic RDF/JSON {@linkplain RDFWriterFactory writer factory}.
 */
public final class RDFJSONWriterFactory implements RDFWriterFactory {

	@Override public RDFFormat getRDFFormat() {
		return com.metreeca.rdf.formats.RDFFormat.RDFJSONFormat;
	}


	@Override public RDFWriter getWriter(final OutputStream output) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		return new RDFJSONWriter(output);
	}

	@Override public RDFWriter getWriter(final OutputStream output, final String baseURI) throws URISyntaxException {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		return new RDFJSONWriter(output, baseURI);
	}


	@Override public RDFWriter getWriter(final Writer writer) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		return new RDFJSONWriter(writer);
	}

	@Override public RDFWriter getWriter(final Writer writer, final String baseURI) throws URISyntaxException {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		return new RDFJSONWriter(writer, baseURI);
	}

}
