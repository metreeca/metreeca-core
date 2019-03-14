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

package com.metreeca.form.things;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

import javax.json.*;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;


public final class JsonValues {

	@SuppressWarnings("unchecked") public static JsonValue json(final Object object) {
		return object instanceof JsonValue ? (JsonValue)object

				: object instanceof Boolean ? (Boolean)object ? JsonValue.TRUE : JsonValue.FALSE

				: object instanceof Integer ? Json.createValue((Integer)object)
				: object instanceof Long ? Json.createValue((Long)object)
				: object instanceof Double ? Json.createValue((Double)object)
				: object instanceof BigInteger ? Json.createValue((BigInteger)object)
				: object instanceof BigDecimal ? Json.createValue((BigDecimal)object)

				: object instanceof String ? Json.createValue((String)object)

				: object instanceof Collection<?> ? array((Collection<Object>)object)
				: object instanceof Map<?, ?> ? object((Map<String, Object>)object)

				: JsonValue.NULL;
	}


	public static JsonArray array() {
		return array(JsonValue.EMPTY_JSON_ARRAY);
	}

	public static JsonArray array(final JsonArray array) {
		return array;
	}

	public static JsonArray array(final Object... items) {
		return array(list(items));
	}

	public static JsonArray array(final Collection<Object> items) {
		return Json.createArrayBuilder(items).build();
	}


	public static JsonObject object() {
		return object(JsonValue.EMPTY_JSON_OBJECT);
	}

	public static JsonObject object(final JsonObject object) {
		return object;
	}

	@SafeVarargs public static JsonObject object(final Map.Entry<String, Object>... fields) {
		return object(map(fields));
	}

	public static JsonObject object(final Map<String, Object> map) {
		return Json.createObjectBuilder(map).build();
	}


	public static Map.Entry<String, Object> field(final String name, final Object value) {
		return entry(name, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonValues() {} // utility

}
