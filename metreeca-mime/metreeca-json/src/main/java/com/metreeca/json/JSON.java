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

package com.metreeca.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.*;


public final class JSON {

	private static JsonValue value(final java.lang.Object value) {

		return value == null ? JsonValue.NULL

				: value instanceof Boolean ? (Boolean)value ? JsonValue.TRUE : JsonValue.FALSE
				: value instanceof String ? Json.createValue((String)value)

				: value instanceof Byte ? Json.createValue((Byte)value)
				: value instanceof Short ? Json.createValue((Short)value)
				: value instanceof Integer ? Json.createValue((Integer)value)
				: value instanceof Long ? Json.createValue((Long)value)
				: value instanceof Float ? Json.createValue((Float)value)
				: value instanceof Double ? Json.createValue((Double)value)
				: value instanceof BigInteger ? Json.createValue((BigInteger)value)
				: value instanceof BigDecimal ? Json.createValue((BigDecimal)value)
				: value instanceof Number ? Json.createValue(((Number)value).doubleValue())

				: value instanceof Map ? value((Map<?, ?>)value)
				: value instanceof Iterable ? value((Iterable<?>)value)
				: value instanceof Stream ? value((Stream<?>)value)
				: value instanceof Optional ? value((Optional<?>)value)

				: value instanceof JsonValue ? (JsonValue)value
				: value instanceof JsonObjectBuilder ? ((JsonObjectBuilder)value).build()
				: value instanceof JsonArrayBuilder ? ((JsonArrayBuilder)value).build()

				: value instanceof Object ? ((Object)value).build()
				: value instanceof Array ? ((Array)value).build()

				: value(value.getClass());

	}


	private static JsonValue value(final Map<?, ?> map) {

		final Object object=new Object();

		map.forEach((field, value) -> object.set(String.valueOf(field), value));

		return object.build();
	}

	private static JsonValue value(final Iterable<?> collection) {

		final Array array=new Array();

		collection.forEach(array::add);

		return array.build();
	}

	private static JsonValue value(final Stream<?> stream) {

		final Array array=new Array();

		stream.forEachOrdered(array::add);

		return array.build();
	}

	private static JsonValue value(final Optional<?> optional) {
		return optional.map(JSON::value).orElse(null);
	}


	private static JsonValue value(final Class<?> type) {
		throw new UnsupportedOperationException("unsupported type {"+type.getName()+"}");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Object {

		private final JsonObjectBuilder builder=Json.createObjectBuilder();


		public Object set(final String field, final java.lang.Object value) {

			if ( field == null ) {
				throw new NullPointerException("null field");
			}

			final JsonValue json=value(value);

			if ( json != null ) {
				builder.add(field, json);
			}

			return this;
		}


		public JsonObject build() {
			return builder.build();
		}

	}

	public static final class Array {

		private final JsonArrayBuilder builder=Json.createArrayBuilder();


		public Array add(final java.lang.Object value) {

			final JsonValue json=value(value);

			if ( json != null ) {
				builder.add(json);
			}

			return this;
		}


		public JsonArray build() {
			return builder.build();
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSON() {}

}
