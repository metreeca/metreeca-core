/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;

import javax.json.*;
import java.util.function.Consumer;


public final class JSONAssert extends AbstractAssert<JSONAssert, JsonValue> {

	public static JSONAssert assertThat(final JsonValue json) {
		return new JSONAssert(json);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JSONAssert(final JsonValue actual) {
		super(actual, JSONAssert.class);
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public JSONAssert isEqualTo(final JsonObjectBuilder expected) {
		return isEqualTo(expected.build());
	}

	public JSONAssert isEqualTo(final JsonArrayBuilder expected) {
		return isEqualTo(expected.build());
	}


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public JSONAssert isObject() {

		isNotNull();

		if ( !(actual instanceof JsonObject) ) {
			failWithMessage(
					"expected <%s> to be a JSON object but was a <%s>", actual, actual.getValueType()
			);
		}

		return this;
	}


	public JSONAssert hasField(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> json.asJsonObject().containsKey(name),
				"expected <%s> to have field <%s>",
				actual, name
		));
	}

	public JSONAssert hasField(final String name, final boolean value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> json.asJsonObject().getBoolean(name) == value,
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

	public JSONAssert hasField(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return isObject().satisfies(new Condition<>(
				json -> value.equals(json.asJsonObject().getString(name, null)),
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

	public JSONAssert hasField(final String name, final JsonObjectBuilder builder) {
		return hasField(name, builder.build());
	}

	public JSONAssert hasField(final String name, final JsonArrayBuilder builder) {
		return hasField(name, builder.build());
	}

	public JSONAssert hasField(final String name, final JsonValue value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return isObject().satisfies(new Condition<>(
				json -> value.equals(json.asJsonObject().getOrDefault(name, null)),
				"expected <%s> to have field <%s> with value <%s>",
				actual, name, value
		));
	}

	public JSONAssert hasField(final String name, final Consumer<JsonValue> assertions) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isObject();

		final JsonValue value=actual.asJsonObject().getOrDefault(name, null);

		if ( value == null ) {
			failWithMessage("expected object to have field <%s> but has none", name);
		}

		assertions.accept(value);

		return myself;
	}


	public JSONAssert doesNotHaveField(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return isObject().satisfies(new Condition<>(
				json -> !json.asJsonObject().containsKey(name),
				"expected <%s> not to have field <%s>",
				actual, name
		));
	}

}
