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

import org.eclipse.rdf4j.model.IRI;

import javax.json.*;
import java.util.*;
import java.util.Map.Entry;

import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.Meta.aliases;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createObjectBuilder;


final class JSONTrimmer {

	JsonValue trim(final IRI focus, final JsonValue value, final Shape shape) {
		return value instanceof JsonObject ? trim(focus, shape, value.asJsonObject())
				: value instanceof JsonArray ? trim(focus, shape, value.asJsonArray())
				: value;
	}


	private JsonObject trim(final IRI focus, final Shape shape, final Map<String, JsonValue> object) {

		final Map<IRI, Shape> fields=fields(shape);
		final Map<String, IRI> aliases=aliases(shape)
				.entrySet().stream().collect(toMap(Entry::getValue, Entry::getKey));

		final JsonObjectBuilder builder=createObjectBuilder();

		object.forEach((label, value) -> Optional.of(label)

				.map(aliases::get)
				.map(fields::get)

				.ifPresent(nested ->
						builder.add(label, trim(focus, value, nested))
				));

		return builder.build();
	}

	private JsonArray trim(final IRI focus, final Shape shape, final Collection<JsonValue> array) {
		return Json.createArrayBuilder(array.stream().map(v -> trim(focus, v, shape)).collect(toList())).build();
	}

}
