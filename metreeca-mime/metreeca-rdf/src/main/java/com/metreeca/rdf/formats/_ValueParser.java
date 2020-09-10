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
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.rio.ParserConfig;

import javax.json.JsonException;
import javax.json.JsonValue;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.rdf.Values.format;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.formats.JSONLDCodecs.aliases;

public final class _ValueParser {

	private static final Pattern StepPattern=Pattern.compile( // !!! review/remove
			"(?:^|[./])(?<step>(?<alias>\\w+)|(?<inverse>\\^)?((?<naked>\\w+:.*)|<(?<bracketed>\\w+:[^>]*)>))"
	);


	public static List<IRI> path(final String base, final Shape shape, final String path) {

		final List<IRI> steps=new ArrayList<>();
		final Matcher matcher=StepPattern.matcher(path);

		int last=0;
		Shape reference=shape;

		while ( matcher.lookingAt() ) {

			final Map<Object, Shape> fields=fields(reference);
			final Map<IRI, String> aliases=aliases(reference);

			final Map<String, IRI> index=new HashMap<>();

			// leading '^' for inverse edges added by Values.Inverse.toString() and Values.format(IRI)

			fields.keySet().stream().map(_ValueParser::_iri).forEach(edge -> {
				index.put(format(edge), edge); // inside angle brackets
				index.put(edge.toString(), edge); // naked IRI
			});

			aliases.forEach((iri, alias) -> index.put(alias, iri));

			final String step=matcher.group("step");
			final IRI iri=index.get(step);

			if ( iri == null ) {
				throw new JsonException("unknown path step ["+step+"]");
			}

			steps.add(iri);
			reference=fields.get(iri);

			matcher.region(last=matcher.end(), path.length());
		}

		if ( last != path.length() ) {
			throw new JsonException("malformed path ["+path+"]");
		}

		return steps;
	}

	public static Object value(final Shape shape, final JsonValue value) {

		final IRI focus=iri();

		return new JSONLDDecoder(focus, shape, new ParserConfig())
				.value(value, shape).getKey();
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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

}
