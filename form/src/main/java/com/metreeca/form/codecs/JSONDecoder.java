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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Datatype;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;

import java.net.URI;
import java.util.Collection;

import javax.json.*;

import static com.metreeca.form.things.Values.iri;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


final class JSONDecoder {

	private final URI base;


	JSONDecoder(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		this.base=base.isEmpty() ? null : URI.create(base);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	<V> V error(final String message) {
		throw new JsonException(message);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Collection<Value> values(final Shape shape, final JsonValue object) {
		return object instanceof JsonArray ? values(shape, (JsonArray)object)
				: singleton(value(shape, object));
	}

	private Collection<Value> values(final Shape shape, final JsonArray array) {
		return array.stream().map(o -> value(shape, o)).collect(toList());
	}


	Value value(final Shape shape, final JsonValue object) {
		return Datatype.datatype(shape)

				.map(datatype -> datatype.equals(Form.IRIType) && object instanceof JsonString ?
						iri(resolve(((JsonString)object).getString())) : value(object)
				)

				.orElseGet(() -> value(object));
	}

	private Value value(final JsonValue value) {
		return value instanceof JsonObject ? resource((JsonObject)value)
				: value instanceof JsonString ? literal((JsonString)value)
				: value instanceof JsonNumber ? literal((JsonNumber)value)
				: value .equals(JsonValue.TRUE)? Values.literal(true)
				: value .equals(JsonValue.FALSE)? Values.literal(false)
				: error("unsupported JSON value <"+value+">");
	}

	private IRI resource(final JsonObject object) {

		final JsonValue self=object.get("this");

		return self instanceof JsonString
				? iri(resolve(((JsonString)self).getString()))
				: error("this is not a string");
	}

	private Literal literal(final JsonString value) {
		return Values.literal(value.getString());
	}

	private Literal literal(final JsonNumber number) {
		return number.isIntegral()
				? Values.literal(number.bigIntegerValue())
				: Values.literal(number.bigDecimalValue());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String resolve(final String iri) {
		return base == null ? iri : base.resolve(iri).toString(); // !!! null base => absolute IRI
	}

}
