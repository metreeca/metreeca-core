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

import com.metreeca.gae.GAE;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

import javax.json.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Type.type;

import static java.util.stream.Collectors.toList;


final class EntityDecoder {

	Entity decode(final JsonObject json, final Shape shape, final String id) {

		final Entity value=new Entity(clazz(shape).orElse("*"), id);

		value.setPropertiesFrom(entity(json, shape));

		return value;
	}


	private Object value(final JsonValue value, final Shape shape) {

		switch ( value.getValueType() ) {
			case NULL: return null;
			case TRUE: return true;
			case FALSE: return false;
			case NUMBER: return number((JsonNumber)value, shape);
			case STRING: return string((JsonString)value, shape);
			case ARRAY: return value.asJsonArray().stream().map(v -> value(v, shape)).collect(toList());
			case OBJECT: return entity(value.asJsonObject(), shape);
		}

		return null; // unexpected
	}

	private Number number(final JsonNumber number, final Shape shape) {
		return type(shape).orElse("").equals(GAE.Decimal) ? (Number)new Double(number.doubleValue())
				: number.isIntegral() ? (Number)new Long(number.longValue())
				: (Number)new Double(number.doubleValue());
	}

	private Object string(final JsonString string, final Shape shape) {
		switch ( type(shape).orElse("") ) {

			case GAE.Date: return date(string);
			case GAE.Entity: return entity(string);

			default: return string.getString();

		}
	}

	private Date date(final JsonString string) {
		return Date.from(OffsetDateTime.parse(string.getString()).toInstant());
	}

	private PropertyContainer entity(final JsonString string) {

		final EmbeddedEntity entity=new EmbeddedEntity();

		entity.setIndexedProperty("id", string.getString());

		return entity;
	}

	private PropertyContainer entity(final JsonObject object, final Shape shape) {

		final Map<String, Shape> fields=fields(shape);
		final Shape empty=and();

		final EmbeddedEntity entity=new EmbeddedEntity();

		object.forEach((name, json) -> {

			final Object value=value(json, fields.getOrDefault(name, empty));

			if ( value != null ) {
				entity.setIndexedProperty(name, value); // !!! un/indexed from shape metadata?
			}

		});

		return entity;
	}

}
