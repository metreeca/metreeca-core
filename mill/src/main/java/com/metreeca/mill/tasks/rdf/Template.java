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

package com.metreeca.mill.tasks.rdf;


import com.metreeca.mill.Task;
import com.metreeca.mill._Cell;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.stream.Stream;


public class Template implements Task { // !!! merge into RDF

	private final Model template;


	public Template(final String template) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		try {

			this.template=Rio.parse(new StringReader(template), Values.Internal, RDFFormat.TURTLE);

		} catch ( final IOException unexpected ) {
			throw new UncheckedIOException(unexpected);
		}
	}


	@Override public Stream<_Cell> execute(final Stream<_Cell> items) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
