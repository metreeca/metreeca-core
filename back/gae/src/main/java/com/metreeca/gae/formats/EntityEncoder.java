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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.fields;


final class EntityEncoder {

	JsonObject encode(final PropertyContainer entity, final Shape shape) {
		return value(entity, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue value(final Object object, final Shape shape) {
		return object == null ? JsonValue.NULL
				: GAE.Entity(object) ? value((PropertyContainer)object, shape)
				: GAE.Boolean(object) ? value((Boolean)object, shape)
				: GAE.Floating(object) ? value(((Number)object).doubleValue(), shape)
				: GAE.Integral(object) ? value(((Number)object).longValue(), shape)
				: GAE.String(object) ? value((String)object, shape)
				: GAE.Date(object) ? value((Date)object, shape)
				: object instanceof Text ? value(((Text)object).getValue(), shape)
				: object instanceof Iterable ? value((Iterable<?>)object, shape)
				: error(object);
	}

	private JsonValue value(final Boolean _boolean, final Shape shape) {
		return _boolean ? JsonValue.TRUE : JsonValue.FALSE;
	}

	private JsonValue value(final double _double, final Shape shape) {
		return Json.createValue(_double);
	}

	private JsonValue value(final long _long, final Shape shape) {
		return Json.createValue(_long);
	}

	private JsonValue value(final String string, final Shape shape) {
		return Json.createValue(string);
	}

	private JsonValue value(final Date date, final Shape shape) {
		return Json.createValue(Instant.ofEpochMilli(date.getTime()).truncatedTo(ChronoUnit.SECONDS).toString());
	}

	private JsonValue value(final Iterable<?> iterable, final Shape shape) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		iterable.forEach(item -> builder.add(value(item, shape)));

		return builder.build();
	}

	private JsonObject value(final PropertyContainer entity, final Shape shape) {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		final Key key=entity instanceof Entity ? ((Entity)entity).getKey()
				: entity instanceof EmbeddedEntity ? ((EmbeddedEntity)entity).getKey()
				: null;

		if ( key != null ) {
			builder.add(GAE.id, key.getName());
			builder.add(GAE.type, key.getKind());
		}

		final Map<String, Shape> fields=fields(shape);

		entity.getProperties().forEach((name, object) -> {

			final JsonValue value=value(object, fields.getOrDefault(name, and()));

			if ( value.getValueType() != JsonValue.ValueType.NULL ) {
				builder.add(name, value);
			}

		});

		return builder.build();
	}


	private JsonValue error(final Object value) {
		throw new UnsupportedOperationException(String.format("unsupported data type {%s}", value.getClass()));
	}

}
