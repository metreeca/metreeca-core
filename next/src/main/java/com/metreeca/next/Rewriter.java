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

package com.metreeca.next;

import com.metreeca.tray.IO;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.jeep.rdf.Values.iri;


/**
 * IRI rewriter.
 */
public final class Rewriter {

	private static final Pattern BasePattern=Pattern.compile("^\\w+:.*/$");


	public static Rewriter rewriter(final String external, final String internal) {

		if ( external == null ) {
			throw new NullPointerException("null external");
		}

		if ( internal == null ) {
			throw new NullPointerException("null internal");
		}

		if ( !external.isEmpty() && !BasePattern.matcher(external).matches() ) {
			throw new IllegalArgumentException("not an absolute external base ["+external+"]");
		}

		if ( !internal.isEmpty() && !BasePattern.matcher(internal).matches() ) {
			throw new IllegalArgumentException("not an absolute internal base ["+internal+"]");
		}

		return new Rewriter(external, internal);
	}


	private final boolean identity;

	private final String external;
	private final String internal;


	private Rewriter(final String external, final String internal) {

		this.identity=external.isEmpty() || internal.isEmpty() || external.equals(internal);

		this.external=external;
		this.internal=internal;
	}


	//// External > Internal ///////////////////////////////////////////////////////////////////////////////////////////

	public Value internal(final Value value) {
		return identity || !(value instanceof IRI) ? value : internal((IRI)value);
	}

	public IRI internal(final IRI iri) {
		return identity || iri == null || !iri.stringValue().startsWith(external) ?
				iri : iri(internal, iri.stringValue().substring(external.length()));
	}


	public String internal(final String text) {
		return identity || text == null ? text : text.replace(external, internal);
	}


	public InputStream internal(final InputStream input) {
		return input;
	}

	public Reader internal(final Reader reader) { // !!! streaming replacement
		return identity || reader == null ? reader : new FilterReader(
				new StringReader(IO.text(reader).replace(external, internal))) {

			@Override public void close() throws IOException { reader.close(); }

		};
	}


	//// Internal > External ///////////////////////////////////////////////////////////////////////////////////////////

	public Value external(final Value value) {
		return identity || !(value instanceof IRI) ? value : external((IRI)value);
	}

	public IRI external(final IRI iri) {
		return identity || iri == null || !iri.stringValue().startsWith(internal) ?
				iri : iri(external, iri.stringValue().substring(internal.length()));
	}


	public String external(final String text) {
		return identity || text == null ? text : text.replace(internal, external);
	}


	public OutputStream external(final OutputStream output) {
		return output;
	}

	public Writer external(final Writer writer) { // !!! streaming replacement
		return identity || writer == null ? writer : new FilterWriter(new StringWriter()) {

			@Override public void close() throws IOException {
				writer.write(out.toString().replace(internal, external));
				writer.close();
			}

		};
	}

}
