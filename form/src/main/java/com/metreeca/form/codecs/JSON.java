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

package com.metreeca.form.codecs;

import java.io.Reader;
import java.io.StringReader;
import java.math.BigInteger;
import java.util.*;

import javax.json.Json;
import javax.json.JsonException;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParserFactory;


/**
 * JSON codecs.
 *
 * @see "https://javaee.github.io/jsonp/"
 * @deprecated To be removed
 */
@Deprecated final class JSON {

	private static final JsonParserFactory parsers=Json.createParserFactory(null);


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
 		} catch ( final NoSuchElementException e ) {
			throw new JsonException("unexpected end of file", e);
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSON() {}

}
