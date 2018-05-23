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

package com.metreeca.mill.fn;


import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import java.net.URI;


public class ResolveURI implements Function {

	private static final String Space="http://www.w3.org/2005/xpath-functions#";
	private static final String Name="resolve-uri";


	//// RDF4J /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String getURI() {
		return Space+Name;
	}

	@Override public Value evaluate(final ValueFactory factory, final Value... args) {

		if ( args.length == 2 ) {

			final String relative=args[0].stringValue();
			final String base=args[1].stringValue();

			return factory.createIRI(resolve(base, relative));

		} else {

			throw new IllegalArgumentException(String.format("usage %s(<base>, <relative-uri>)", Name));

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String resolve(final String base, final String uri) {
		return URI.create(base).resolve(uri).toString();
	}

}
