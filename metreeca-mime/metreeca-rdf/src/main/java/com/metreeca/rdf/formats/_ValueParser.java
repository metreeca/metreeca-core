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

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.ParserConfig;

import javax.json.JsonException;
import javax.json.JsonValue;
import java.util.*;
import java.util.regex.Pattern;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.rdf.formats._Aliases.aliases;

public final class _ValueParser {

	private static final Pattern DotPattern=Pattern.compile("\\.");


	public static List<IRI> path(final Shape shape, final String path) throws JsonException {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		final List<IRI> steps=new ArrayList<>();

		Shape reference=shape;

		for (final String step : DotPattern.split(path)) {
			if ( !step.isEmpty() ) {

				final Map<Object, Shape> fields=fields(reference);
				final Map<IRI, String> aliases=aliases(reference);

				final IRI iri=aliases.entrySet().stream()

						.filter(entry -> entry.getValue().equals(step))
						.map(Map.Entry::getKey)
						.findFirst()

						.orElseThrow(() -> new JsonException("unknown path step <"+step+">"));


				steps.add(iri);
				reference=fields.get(iri);
			}
		}

		return steps;
	}

	public static Object value(final Shape shape, final JsonValue value) throws JsonException {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		final IRI focus=iri();

		return new JSONLDDecoder(focus, shape, new ParserConfig()).value(value, shape).getKey();
	}

}
