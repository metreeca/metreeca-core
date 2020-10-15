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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Field;

import org.eclipse.rdf4j.model.IRI;

import javax.json.*;
import java.util.*;

import static com.metreeca.rest.formats.JSONLDCodec.*;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;


final class JSONLDTrimmer {

	private final IRI focus;
	private final Shape shape;
	private final Map<String, String> keywords;


	JSONLDTrimmer(final IRI focus, final Shape shape, final Map<String, String> keywords) {

		this.focus=focus;
		this.shape=driver(shape);

		this.keywords=keywords;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	JsonValue trim(final JsonValue value) {
		return trim(focus, shape, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue trim(final IRI focus, final Shape shape, final JsonValue value) {
		return value instanceof JsonObject ? trim(focus, shape, value.asJsonObject())
				: value instanceof JsonArray ? trim(focus, shape, value.asJsonArray())
				: value;
	}

	private JsonObject trim(final IRI focus, final Shape shape, final JsonObject object) {

		final JsonObjectBuilder builder=createObjectBuilder();

		if ( tagged(shape) ) {

			final Set<String> langs=langs(shape).orElseGet(Collections::emptySet);

			object.forEach((label, value) -> {
				if ( label.startsWith("@") || keywords.containsValue(label) ) {

					builder.add(label, value);

				} else if ( langs.contains(label) && value instanceof JsonString ) {

					builder.add(label, value);

				} else if ( langs.contains(label) && value instanceof JsonArray ) {

					builder.add(label, createArrayBuilder(value.asJsonArray().stream()
							.filter(JsonString.class::isInstance)
							.map(JsonString.class::cast)
							.map(JsonString::getString)
							.collect(toList())
					));

				}
			});

		} else {

			final Map<String, Field> fields=fields(shape, keywords);

			object.forEach((label, value) -> {
				if ( label.startsWith("@") || keywords.containsValue(label) ) {

					builder.add(label, value);

				} else {

					Optional.of(label).map(fields::get).ifPresent(field ->
							builder.add(label, trim(focus, field.shape(), value))
					);

				}
			});

		}

		return builder.build();

	}

	private JsonArray trim(final IRI focus, final Shape shape, final JsonArray array) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		array.forEach(value -> builder.add(trim(focus, shape, value)));

		return builder.build();
	}

}
