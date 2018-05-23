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


import com.metreeca.spec.things.Values;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function;

import java.util.Locale;


/**
 * Convert a label to a lower-case hyphenated identifier.
 */
public class Identifier implements Function {

	private static final String Space=Values.User;
	private static final String Name="identifier";


	//// RDF4J /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String getURI() {
		return Space+Name;
	}

	@Override public Value evaluate(final ValueFactory factory, final Value... args) {

		if ( args.length == 1 ) {

			final String label=args[0].stringValue();

			return factory.createLiteral(identifier(label));

		} else {

			throw new IllegalArgumentException(String.format("usage %s(<string>)", Name));

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String identifier(final String label) { // !!! strip leading/trailing garbage
		return label == null ? null : label.toLowerCase(Locale.ROOT).replaceAll("\\W+", "-");
	}

}
