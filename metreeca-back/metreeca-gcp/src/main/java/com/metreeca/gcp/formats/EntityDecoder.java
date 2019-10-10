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

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Meta.index;

import static java.util.stream.Collectors.toList;


final class EntityDecoder {

	private final Datastore datastore;


	EntityDecoder(final Datastore datastore) {
		this.datastore=datastore;
	}


	Value<?> decode(final JsonValue value, final Shape shape) {
		return value(value, shape);
	}

	FullEntity<?> decode(final JsonObject json, final Shape shape) {
		return entity(json, shape).get();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> value(final JsonValue value, final Shape shape) {
		switch ( value.getValueType() ) {

			case NULL: return NullValue.of();
			case TRUE: return BooleanValue.of(true);
			case FALSE: return BooleanValue.of(false);
			case NUMBER: return number((JsonNumber)value, shape);
			case STRING: return string((JsonString)value, shape);
			case ARRAY: return collection(value.asJsonArray(), shape);
			case OBJECT: return entity(value.asJsonObject(), shape);

			default: throw new UnsupportedOperationException("unsupported JSON value {"+value+"}");

		}
	}

	private Value<?> number(final JsonNumber number, final Shape shape) {
		return datatype(shape).orElse("").equals(ValueType.DOUBLE) ? DoubleValue.of(number.doubleValue())
				: number.isIntegral() ? LongValue.of(number.longValue())
				: DoubleValue.of(number.doubleValue());
	}

	private Value<?> string(final JsonString string, final Shape shape) {

		final Object datatype=datatype(shape)
				.orElseGet(() -> clazz(shape).map(c -> ValueType.ENTITY)
						.orElse(ValueType.NULL)
				);

		// !!! blob
		// !!! latlng
		// !!! key?

		return datatype == ValueType.BOOLEAN ? _boolean(string)
				: datatype == ValueType.LONG ? _long(string)
				: datatype == ValueType.DOUBLE ? _double(string)
				: datatype == ValueType.TIMESTAMP ? date(string)
				: datatype == ValueType.ENTITY ? entity(string, shape)
				: string(string);
	}


	private Value<?> _boolean(final JsonString string) {
		return BooleanValue.of(Boolean.parseBoolean(string.getString()));
	}

	private Value<?> _long(final JsonString string) {
		try {
			return LongValue.of(Long.parseLong(string.getString()));
		} catch ( final NumberFormatException e ) {
			return string(string);
		}
	}

	private Value<?> _double(final JsonString string) {
		try {
			return DoubleValue.of(Double.parseDouble(string.getString()));
		} catch ( final NumberFormatException e ) {
			return string(string);
		}
	}

	private Value<?> date(final JsonString string) {
		try {
			return TimestampValue.of(Timestamp.ofTimeMicroseconds(
					OffsetDateTime.parse(string.getString()).toInstant().toEpochMilli()*1000
			));
		} catch ( final DateTimeParseException e ) {
			return string(string);
		}
	}

	private Value<?> string(final JsonString string) {
		return StringValue.of(string.getString());
	}

	private Value<?> entity(final JsonString string, final Shape shape) {
		return EntityValue.of(Entity.newBuilder(datastore.newKeyFactory()
				.setKind(clazz(shape).map(Object::toString).orElse(GCP.Resource))
				.newKey(string.getString())
		).build());
	}


	private ListValue collection(final JsonArray values, final Shape shape) {
		return ListValue.of(values.stream().map(v -> value(v, shape)).collect(toList()));
	}

	private EntityValue entity(final JsonObject object, final Shape shape) {

		final String id=Optional.ofNullable(object.get(GCP.id))
				.filter(value -> value instanceof JsonString)
				.map(value -> ((JsonString)value).getString())
				.orElse("");

		final String type=Optional.ofNullable(object.get(GCP.type))
				.filter(value -> value instanceof JsonString)
				.map(value -> ((JsonString)value).getString())
				.orElseGet(() -> clazz(shape).map(Object::toString).orElse(""));

		final FullEntity.Builder<?> builder=id.isEmpty()
				? type.isEmpty() ? FullEntity.newBuilder() : FullEntity.newBuilder(datastore.newKeyFactory().setKind(type).newKey())
				: FullEntity.newBuilder(datastore.newKeyFactory().setKind(type.isEmpty() ? GCP.Resource : type).newKey(id));

		final Map<Object, Shape> fields=fields(shape);

		object.forEach((name, json) -> {
			if ( !name.equals(GCP.id) && !name.equals(GCP.type) ) {

				final Shape nested=fields.getOrDefault(name, and());
				final Value<?> value=value(json, nested);

				if ( value.getType() != ValueType.NULL ) {
					builder.set(name, value.toBuilder()
							.setExcludeFromIndexes(!index(nested).orElse(true))
							.build()
					);
				}

			}
		});

		return EntityValue.of(builder.build());
	}

}
