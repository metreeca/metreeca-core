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

package com.metreeca.gcp.formats;

import com.metreeca.gcp.services.DatastoreTestBase;
import com.metreeca.tree.Shape;

import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.Date;

import javax.json.Json;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class EntityDecoderTest extends DatastoreTestBase {

	private FullEntity<?> decode(final String json) {
		return decode(json, and());
	}

	private FullEntity<?> decode(final String json, final Shape shape) {
		return new EntityDecoder(service(datastore())).decode(
				Json.createReader(new StringReader(json.replace('\'', '"'))).readObject(), shape
		);
	}


	@Test void testDecodeEntity() {
		exec(()-> assertThat(decode("{ 'id': '/path', 'type': 'Class' }")).satisfies(entity -> {
			assertThat(entity.getKey()).isEqualTo(service(datastore()).key("Class", "/path"));
		}));
	}


	@Test void testIgnoreNullFields() {
		exec(()-> assertThat(decode("{ 'field': null }").contains("field")).isFalse());
	}

	@Test void testDecodeBooleanFields() {
		exec(()-> assertThat(decode("{ 'field': true }").getBoolean("field")).isEqualTo(true));
	}

	@Test void testDecodeLongFields() {
		exec(()-> assertThat(decode("{ 'field': 123 }").getLong("field")).isEqualTo(123L));
	}

	@Test void testDecodeDoubleFields() {
		exec(()-> assertThat(decode("{ 'field': 123.0 }").getDouble("field")).isEqualTo(123.0D));
	}

	@Test void testDecodeIntegerFieldsExpectedDouble() {
		exec(()-> assertThat(decode("{ 'field': 123 }", field("field", datatype(ValueType.DOUBLE))).getDouble("field"))
				.isEqualTo(123.0D)
		);
	}

	@Test void testDecodeStringFields() {
		exec(()-> assertThat(decode("{ 'field': 'string' }").getString("field"))
				.isEqualTo("string")
		);
	}

	@Test void testDecodeStringFieldsAsExpectedLong() {
		exec(()-> assertThat(decode("{ 'field': '123' }", field("field", datatype(ValueType.LONG))).getLong("field"))
				.isEqualTo(123L)
		);
	}

	@Test void testDecodeStringFieldsAsExpectedTimestamp() {
		exec(()-> assertThat(decode("{ 'field': '2019-01-01T00:00:00.123Z' }", field("field", datatype(ValueType.TIMESTAMP))).getTimestamp("field").toDate())
				.isEqualTo(Date.from(OffsetDateTime.parse("2019-01-01T00:00:00.123Z").toInstant()))
		);
	}

	@Test void testDecodeStringFieldsAsExpectedBoolean() {
		exec(()-> assertThat(decode("{ 'field': 'true' }", field("field", datatype(ValueType.BOOLEAN))).getBoolean("field"))
				.isEqualTo(true)
		);
	}

	@Test void testDecodeStringFieldsAsExpectedDouble() {
		exec(()-> assertThat(decode("{ 'field': '123' }", field("field", datatype(ValueType.DOUBLE))).getDouble("field"))
				.isEqualTo(123.0D)
		);
	}

	@Test void testDecodeStringFieldsAsExpectedEntity() {
		exec(()-> assertThat(decode("{ 'field': '/path' }", field("field", and(datatype(ValueType.ENTITY), clazz("Class")))).getEntity("field"))
				.satisfies(entity -> assertThat(entity.getKey()).isEqualTo(service(datastore()).key("Class", "/path")))
		);
	}


	@Test void testDecodeArrayFields() {
		exec(()-> assertThat(decode("{ 'field': [null, 123, 'string'] }").getList("field"))
				.isEqualTo(asList(NullValue.of(), LongValue.of(123L), StringValue.of("string")))
		);
	}


	@Test void testDecodeObjectFieldsEmpty() {
		exec(()-> assertThat(decode("{ 'field': {} }").getEntity("field"))

				.satisfies(entity -> assertThat(entity.hasKey()).isFalse())
		);
	}

	@Test void testDecodeObjectFields() {
		exec(()-> assertThat(decode("{ 'field': { 'id': '/path', 'type': 'Class', 'value' : 123 } }").getEntity("field"))

				.satisfies(entity -> assertThat(entity.getKey()).isEqualTo(service(datastore()).key("Class", "/path")))
				.satisfies(entity -> assertThat(entity.getProperties().keySet()).containsOnly("value"))
				.satisfies(entity -> assertThat(entity.getLong("value")).isEqualTo(123L))
		);
	}

	@Test void testDecodeObjectFieldsWithExpectedType() {
		exec(()-> assertThat(decode("{ 'field': { 'id': '/path' } }", field("field", clazz("Class"))).getEntity("field"))

				.satisfies(entity -> assertThat(entity.getKey()).isEqualTo(service(datastore()).key("Class", "/path")))
		);
	}

}
