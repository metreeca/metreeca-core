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

package com.metreeca.form.things;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

import static javax.json.stream.JsonGenerator.PRETTY_PRINTING;


/**
 * JSON codecs.
 *
 * @see "https://javaee.github.io/jsonp/"
 */
public final class _JSON { // !!! review/remove

	private static final JsonParserFactory parsers=Json
			.createParserFactory(null);

	private static final JsonGeneratorFactory generators=Json
			.createGeneratorFactory(singletonMap(PRETTY_PRINTING, true));


	//// Factories /////////////////////////////////////////////////////////////////////////////////////////////////////

	public static List<Object> array(final Object... items) {
		return asList(items);
	}

	@SafeVarargs public static Map<String, Object> object(final Map.Entry<String, Object>... fields) {

		final Map<String, Object> object=new LinkedHashMap<>();

		for (final Map.Entry<String, Object> field : fields) {
			object.put(field.getKey(), field.getValue());
		}

		return object;
	}

	public static Map.Entry<String, Object> field(final String label, final Object value) {
		return new AbstractMap.SimpleEntry<>(label, value);
	}


	//// Decoder ///////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Object decode(final String json) throws JsonException {

		if ( parsers == null ) {
			throw new NullPointerException("null json");
		}

		return decode(new StringReader(json));
	}

	public static Object decode(final Reader reader) throws JsonException {

		if ( reader == null ) {
			throw new NullPointerException("null reader");
		}

		try (final JsonParser parser=parsers.createParser(reader)) {
			return json(parser, parser.next());
		}
	}


	private static Object json(final JsonParser parser, final JsonParser.Event next) {
		return next == JsonParser.Event.START_OBJECT ? object(parser)
				: next == JsonParser.Event.START_ARRAY ? array(parser)
				: next == JsonParser.Event.VALUE_STRING ? parser.getString()
				: next == JsonParser.Event.VALUE_NUMBER ?
				parser.isIntegralNumber() ? BigInteger.valueOf(parser.getLong()) : parser.getBigDecimal()
				: next == JsonParser.Event.VALUE_TRUE ? Boolean.TRUE
				: next == JsonParser.Event.VALUE_FALSE ? Boolean.FALSE
				: null;
	}


	private static Object object(final JsonParser parser) {

		final Map<Object, Object> object=new LinkedHashMap<>();

		String name=null;

		while ( true ) {

			final JsonParser.Event next=parser.next();

			if ( next == JsonParser.Event.END_OBJECT ) {

				return object;

			} else if ( next == JsonParser.Event.KEY_NAME ) {

				name=parser.getString();

			} else {

				object.put(name, json(parser, next));

			}
		}
	}

	private static Object array(final JsonParser parser) {

		final Collection<Object> array=new ArrayList<>();

		while ( true ) {

			final JsonParser.Event next=parser.next();

			if ( next == JsonParser.Event.END_ARRAY ) {

				return array;

			} else {

				array.add(json(parser, next));

			}
		}
	}


	//// Encoder ///////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String encode(final Object object) throws JsonException {

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		return encode(object, new StringWriter()).toString();
	}

	public static <W extends Writer> W encode(final Object object, final W writer) throws JsonException {

		if ( object == null ) {
			throw new NullPointerException("null object");
		}

		if ( writer == null ) {
			throw new NullPointerException("null writer");
		}

		try (final JsonGenerator generator=generators.createGenerator(writer)) {
			write(generator, null, object);
		}

		return writer;
	}


	private static JsonGenerator write(final JsonGenerator generator, final String name, final Object value) {
		return value instanceof Map<?, ?> ? write(generator, name, (Map<?, ?>)value)
				: value instanceof Iterable<?> ? write(generator, name, (Iterable<?>)value)
				: value instanceof Boolean ? write(generator, name, (Boolean)value)
				: value instanceof Integer ? write(generator, name, (Integer)value)
				: value instanceof Long ? write(generator, name, (Long)value)
				: value instanceof Float ? write(generator, name, (Float)value)
				: value instanceof Double ? write(generator, name, (Double)value)
				: value instanceof BigInteger ? write(generator, name, (BigInteger)value)
				: value instanceof BigDecimal ? write(generator, name, (BigDecimal)value)
				: value != null ? write(generator, name, value.toString())
				: generator.writeNull(name);
	}


	private static JsonGenerator write(final JsonGenerator generator, final String name, final Map<?, ?> value) {

		if ( name == null ) {
			generator.writeStartObject();
		} else {
			generator.writeStartObject(name);
		}

		for (final Map.Entry<?, ?> entry : value.entrySet()) {
			write(generator, String.valueOf(entry.getKey()), entry.getValue());
		}

		return generator.writeEnd();
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Iterable<?> value) {

		if ( name == null ) {
			generator.writeStartArray();
		} else {
			generator.writeStartArray(name);
		}

		for (final Object item : value) {
			write(generator, null, item);
		}

		return generator.writeEnd();
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Boolean value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Integer value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Long value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Float value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final Double value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final BigInteger value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final BigDecimal value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}

	private static JsonGenerator write(final JsonGenerator generator, final String name, final String value) {
		return name == null ? generator.write(value) : generator.write(name, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _JSON() {}

}
