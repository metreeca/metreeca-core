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

package com.metreeca.gcp.formats;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.services.Datastore;
import com.metreeca.tree.Shape;

import com.google.cloud.datastore.*;

import java.time.Instant;
import java.util.Map;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.fields;


final class EntityEncoder {

	private final Datastore datastore;


	EntityEncoder(final Datastore datastore) {
		this.datastore=datastore;
	}


	JsonObject encode(final Entity entity, final Shape shape) {
		return value(EntityValue.of(entity), shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValue value(final Value value, final Shape shape) {

		switch ( value.getType() ) {

			case NULL: return JsonValue.NULL;
			case STRING: return value((StringValue)value);
			case ENTITY: return value((EntityValue)value, shape);
			case LIST: return value((ListValue)value, shape);
			case LONG: return value((LongValue)value);
			case DOUBLE: return value((DoubleValue)value);
			case BOOLEAN: return value((BooleanValue)value);
			case TIMESTAMP: return value((TimestampValue)value);
			case KEY:
			case BLOB:
			case RAW_VALUE:
			case LAT_LNG: return error(value);
		}

		return error(value);
	}

	private JsonValue value(final BooleanValue _boolean) {
		return _boolean.get() ? JsonValue.TRUE : JsonValue.FALSE;
	}

	private JsonValue value(final LongValue _long) {
		return Json.createValue(_long.get());
	}

	private JsonValue value(final DoubleValue _double) {
		return Json.createValue(_double.get());
	}

	private JsonValue value(final StringValue string) {
		return Json.createValue(string.get());
	}

	private JsonValue value(final TimestampValue timestamp) {
		return Json.createValue(Instant.ofEpochMilli(timestamp.get().toDate().getTime()).toString());
	}

	private JsonValue value(final ListValue list, final Shape shape) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		list.get().forEach(item -> builder.add(value(item, shape)));

		return builder.build();
	}

	private JsonObject value(final EntityValue entity, final Shape shape) {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		final IncompleteKey key=entity.get().getKey();

		if ( key instanceof Key ) { builder.add(GCP.id, ((Key)key).getName()); }
		// if ( key != null ) { builder.add(GCloud.type, key.getKind()); }

		final Map<Object, Shape> fields=fields(shape);

		entity.get().getProperties().forEach((name, value) -> {

			if ( value.getType() != ValueType.NULL ) {
				builder.add(name, value(value, fields.getOrDefault(name, and())));
			}

		});

		return builder.build();
	}


	private JsonValue error(final Value<?> value) {
		throw new UnsupportedOperationException(String.format("unsupported value type {%s}", value.getType()));
	}

}
