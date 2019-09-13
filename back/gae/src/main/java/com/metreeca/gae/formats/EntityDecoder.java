/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gae.formats;

import com.metreeca.gae.GAE;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.*;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;


final class EntityDecoder {

	Object decode(final JsonValue value, final Shape shape) {

		switch ( value.getValueType() ) {
			case NULL: return null;
			case TRUE: return true;
			case FALSE: return false;
			case NUMBER: return number((JsonNumber)value, shape);
			case STRING: return string((JsonString)value, shape);
			case ARRAY: return value.asJsonArray().stream().map(v -> decode(v, shape)).collect(toList());
			case OBJECT: return entity(value.asJsonObject(), shape);
		}

		return null; // unexpected
	}

	PropertyContainer decode(final JsonObject json, final Shape shape) {
		return entity(json, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Number number(final JsonNumber number, final Shape shape) { // ;( removing casts boxes everything to double
		return datatype(shape).orElse("").equals(GAE.Floating) ? (Number)new Double(number.doubleValue())
				: number.isIntegral() ? (Number)new Long(number.longValue())
				: (Number)new Double(number.doubleValue());
	}

	private Object string(final JsonString string, final Shape shape) {
		switch ( datatype(shape).orElse("") ) {

			case GAE.Date: return date(string);

			default:

				final String value=string.getString();

				return value.getBytes(UTF_8).length > 1500 ? new Text(value) : value;

		}
	}

	private Date date(final JsonString string) {
		return Date.from(OffsetDateTime.parse(string.getString()).toInstant());
	}

	private PropertyContainer entity(final JsonObject object, final Shape shape) {

		final EmbeddedEntity entity=new EmbeddedEntity();

		final String id=Optional.ofNullable(object.get("id"))
				.filter(value -> value instanceof JsonString)
				.map(value   -> ((JsonString)value).getString())
				.orElse("*");

		final String type=Optional.ofNullable(object.get("type"))
				.filter(value -> value instanceof JsonString)
				.map(value   -> ((JsonString)value).getString())
				.orElse("*");

		entity.setKey(KeyFactory.createKey(type, id));

		final Map<String, Shape> fields=fields(shape);

		object.forEach((name, json) -> {

			final Object value=decode(json, fields.getOrDefault(name, and()));

			if ( value != null ) {
				if ( GAE.isEntity(value) ) { // !!! un/indexed from shape metadata?

					entity.setIndexedProperty(name, value); // force embedded entity indexing

				} else {

					entity.setProperty(name, value);

				}
			}

		});

		return entity;
	}

}
