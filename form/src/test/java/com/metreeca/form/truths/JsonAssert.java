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

package com.metreeca.form.truths;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;

import javax.json.JsonObject;
import javax.json.JsonValue;


public final class JsonAssert extends AbstractAssert<JsonAssert, JsonValue> {

	public static JsonAssert assertThat(final JsonValue json) {
		return new JsonAssert(json);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonAssert(final JsonValue actual) {
		super(actual, JsonAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public JsonAssert isObject() {

		isNotNull();

		if ( !(actual instanceof JsonObject) ) {
			failWithMessage(
					"expected <%s> to be a JSON object but was a <%s>", actual, actual.getValueType()
			);
		}

		return this;
	}


	public JsonAssert hasField(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> json.asJsonObject().containsKey(name),
				"expected <%s> to have field <%s>",
				actual, name
		));
	}

	public JsonAssert hasField(final String name, final boolean value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> json.asJsonObject().getBoolean(name) == value,
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

	public JsonAssert hasField(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> json.asJsonObject().getString(name).equals(value),
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

	public JsonAssert hasField(final String name, final JsonValue value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return isObject().satisfies(new Condition<>(
				json -> value.equals(json.asJsonObject().getValue("/"+name)),
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

}
