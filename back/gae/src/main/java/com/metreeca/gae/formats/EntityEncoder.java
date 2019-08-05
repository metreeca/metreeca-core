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

import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;

import java.util.Map;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.fields;


final class EntityEncoder {

	JsonObject encode(final Entity entity, final Shape shape) {
		return value(entity, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue value(final Object object, final Shape shape) {
		return object == null ? null
				: object instanceof Boolean ? value((Boolean)object, shape)
				: object instanceof Double || object instanceof Float ? value(((Number)object).doubleValue(), shape)
				: object instanceof Long || object instanceof Integer || object instanceof Short ? value(((Number)object).longValue(), shape)
				: object instanceof String ? value((String)object, shape)
				: object instanceof Iterable ? value((Iterable<?>)object, shape)
				: object instanceof PropertyContainer ? value((PropertyContainer)object, shape)
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

	private JsonValue value(final Iterable<?> iterable, final Shape shape) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		iterable.forEach(item -> builder.add(value(item, shape)));

		return builder.build();
	}

	private JsonObject value(final PropertyContainer entity, final Shape shape) {

		final Map<String, Shape> fields=fields(shape);
		final Shape empty=and();

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		entity.getProperties().forEach((name, object) -> {

			final JsonValue value=value(object, fields.getOrDefault(name, empty));

			if ( value != null ) {
				builder.add(name, value);
			}

		});

		return builder.build();
	}


	private JsonValue error(final Object value) {
		throw new UnsupportedOperationException(String.format("unsupported data type {%s}", value.getClass()));
	}

}
