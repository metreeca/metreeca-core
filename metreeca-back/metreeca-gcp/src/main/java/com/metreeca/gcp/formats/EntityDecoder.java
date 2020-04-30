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

package com.metreeca.gcp.formats;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.services.Datastore;
import com.metreeca.json.formats.JSONFormat;
import com.metreeca.tree.Shape;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.json.*;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.json.formats.JSONFormat.context;
import static com.metreeca.json.formats.JSONFormat.resolver;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Meta.index;

import static java.util.stream.Collectors.toList;


final class EntityDecoder {

	private final Datastore datastore=service(datastore());;
	private final Function<String, String> resolver=resolver(service(context()));


	Value<?> decode(final JsonValue value, final Shape shape) {
		return value(resolver, value, shape);
	}

	FullEntity<?> decode(final JsonObject json, final Shape shape) {
		return entity(resolver, json, shape).get();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> value(final Function<String, String> resolver, final JsonValue value, final Shape shape) {
		switch ( value.getValueType() ) {

			case NULL: return NullValue.of();
			case TRUE: return BooleanValue.of(true);
			case FALSE: return BooleanValue.of(false);
			case NUMBER: return number((JsonNumber)value, shape);
			case STRING: return string((JsonString)value, shape);

			case ARRAY: return collection(resolver, value.asJsonArray(), shape);
			case OBJECT: return entity(resolver, value.asJsonObject(), shape);

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


	private ListValue collection(final Function<String, String> resolver, final JsonArray values, final Shape shape) {
		return ListValue.of(values.stream().map(v -> value(resolver, v, shape)).collect(toList()));
	}

	private EntityValue entity(final Function<String, String> resolver, final JsonObject object, final Shape shape) {

		String id="";
		String type="";

		final Map<Object, Shape> fields=fields(shape);

		final FullEntity.Builder<IncompleteKey> builder=FullEntity.newBuilder();

		for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {

			final String name=resolver.apply(entry.getKey());
			final JsonValue json=entry.getValue();

			if ( name.equals(JSONFormat.id) ) {

				id=Optional.of(json)
						.filter(value -> value instanceof JsonString)
						.map(value -> ((JsonString)value).getString())
						.orElse("");

			} else if ( name.equals(JSONFormat.type) ) {

				type=Optional.of(json)
						.filter(value -> value instanceof JsonString)
						.map(value -> ((JsonString)value).getString())
						.orElse("");

			} else if ( !name.startsWith("@") ) {

				final Shape nested=fields.getOrDefault(name, and());
				final Value<?> value=value(resolver, json, nested);

				if ( value.getType() != ValueType.NULL ) {
					builder.set(name, value.toBuilder()
							.setExcludeFromIndexes(!index(nested).orElse(true))
							.build()
					);
				}

			}
		}

		final KeyFactory factory=datastore.newKeyFactory()
				.setKind(type.isEmpty() ? clazz(shape).map(Object::toString).orElse(GCP.Resource) : type);

		builder.setKey(id.isEmpty() ? factory.newKey() : factory.newKey(id));

		return EntityValue.of(builder.build());
	}

}
