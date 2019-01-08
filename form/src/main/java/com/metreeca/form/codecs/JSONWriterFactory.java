/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.codecs;

import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.RDFWriterFactory;

import java.io.OutputStream;
import java.io.Writer;
import java.net.URISyntaxException;


public final class JSONWriterFactory implements RDFWriterFactory {

	@Override public RDFFormat getRDFFormat() {
		return JSONCodec.JSONFormat;
	}


	@Override public RDFWriter getWriter(final OutputStream output) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		return new JSONWriter(output);
	}

	@Override public RDFWriter getWriter(final OutputStream output, final String baseURI) throws URISyntaxException {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		return new JSONWriter(output);
	}


	@Override public RDFWriter getWriter(final Writer writer) {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		return new JSONWriter(writer);
	}

	@Override public RDFWriter getWriter(final Writer writer, final String baseURI) throws URISyntaxException {

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		return new JSONWriter(writer);
	}

}
