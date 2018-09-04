/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.codecs;

import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Maps;
import com.metreeca.form.things.Values;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import javax.json.*;

import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.format;


public abstract class JSONAdapterTest {

	protected static final String value="http://www.w3.org/1999/02/22-rdf-syntax-ns#value";


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static List<Object> array(final Object... items) {
		return Lists.list(items);
	}

	@SafeVarargs protected static Map<String, Object> object(final Map.Entry<String, Object>... fields) {
		return Maps.map(fields);
	}

	protected static Map<String, Object> object(final Map<String, Object> fields) {
		return fields;
	}

	protected static Map.Entry<String, Object> field(final String label, final Object value) {
		return Maps.entry(label, value);
	}


	//// Converters ////////////////////////////////////////////////////////////////////////////////////////////////////

	protected static JsonValue json(final Object object) {
		return object instanceof List<?> ? json((List<?>)object)
				: object instanceof Map<?, ?> ? json((Map<?, ?>)object)
				: null;
	}

	protected static JsonArray json(final List<?> array) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		for (final Object item : array) {
			if ( item instanceof List<?> ) {
				builder.add(json((List<?>)item));
			} else if ( item instanceof Map<?, ?> ) {
				builder.add(json((Map<?, ?>)item));
			} else if ( item instanceof Boolean ) {
				builder.add((Boolean)item);
			} else if ( item instanceof BigInteger ) {
				builder.add((BigInteger)item);
			} else if ( item instanceof BigDecimal ) {
				builder.add((BigDecimal)item);
			} else if ( item instanceof Double ) {
				builder.add((Double)item);
			} else if ( item instanceof String ) {
				builder.add((String)item);
			} else {
				builder.addNull();
			}
		}

		return builder.build();
	}

	protected static JsonObject json(final Map<?, ?> object) {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		for (final Map.Entry<?, ?> field : object.entrySet()) {

			final String label=field.getKey().toString();
			final Object value=field.getValue();

			if ( value instanceof List<?> ) {
				builder.add(label, json((List<?>)value));
			} else if ( value instanceof Map<?, ?> ) {
				builder.add(label, json((Map<?, ?>)value));
			} else if ( value instanceof Boolean ) {
				builder.add(label, (Boolean)value);
			} else if ( value instanceof BigInteger ) {
				builder.add(label, (BigInteger)value);
			} else if ( value instanceof BigDecimal ) {
				builder.add(label, (BigDecimal)value);
			} else if ( value instanceof Double ) {
				builder.add(label, (Double)value);
			} else if ( value instanceof String ) {
				builder.add(label, (String)value);
			} else {
				builder.addNull(label);
			}
		}

		return builder.build();
	}


	//// !!! review ////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Object blanks(final Object... json) {
		return array(blank(field(value, array(json))));
	}


	@SafeVarargs protected final Map<String, Object> blank(final Map.Entry<String, Object>... fields) {
		return object(Maps.union(
				Maps.map(field("this", Values.format(Values.bnode()))),
				Maps.map(fields)
		));
	}

}
