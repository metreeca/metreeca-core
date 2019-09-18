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
import com.metreeca.gae.GAETestBase;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.*;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Date;

import javax.json.Json;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class EntityDecoderTest extends GAETestBase {

	private PropertyContainer decode(final String json) {
		return decode(json, and());
	}

	private PropertyContainer decode(final String json, final Shape shape) {
		return new EntityDecoder().decode(
				Json.createReader(new StringReader(json.replace('\'', '"'))).readObject(), shape
		);
	}


	@Test void testDecodeEntity() {
		assertThat(decode("{ 'id': '/path', 'type': 'Entity' }")).satisfies(entity -> {
			assertThat(((EmbeddedEntity)entity).getKey()).isEqualTo(KeyFactory.createKey("Entity", "/path"));
		});
	}


	@Test void testIgnoreNullFields() {
		assertThat(decode("{ 'field': null }").hasProperty("field")).isFalse();
	}

	@Test void testDecodeBooleanFields() {
		assertThat(decode("{ 'field': true }").getProperty("field")).isEqualTo(true);
	}

	@Test void testDecodeIntegerFields() {
		assertThat(decode("{ 'field': 123 }").getProperty("field")).isEqualTo(123L);

	}

	@Test void testDecodeDoubleFields() {
		assertThat(decode("{ 'field': 123.0 }").getProperty("field")).isEqualTo(123.0D);

	}

	@Test void testDecodeIntegerFieldsExpectedFloating() {
		assertThat(decode("{ 'field': 123 }", field("field", datatype(GAE.Floating))).getProperty("field"))
				.isEqualTo(123.0D);
	}

	@Test void testDecodeStringFields() {
		assertThat(decode("{ 'field': 'string' }").getProperty("field")).isEqualTo("string");
	}

	@Test void testDecodeStringFieldsExceedingLimits() {

		final char[] chars=new char[2000];

		Arrays.fill(chars, '-');

		final String string=new String(chars);

		assertThat(decode("{ 'field': '"+string+"' }").getProperty("field"))
				.isEqualTo(new Text(string));
	}

	@Test void testDecodeStringFieldsAsExpectedIntegral() {
		assertThat(decode("{ 'field': '123' }", field("field", datatype(GAE.Integral))).getProperty("field"))
				.isEqualTo(123L);
	}

	@Test void testDecodeStringFieldsAsExpectedDate() {
		assertThat(decode("{ 'field': '2019-01-01T00:00Z' }", field("field", datatype(GAE.Date))).getProperty("field"))
				.isEqualTo(Date.from(OffsetDateTime.parse("2019-01-01T00:00Z").toInstant()));
	}

	@Test void testDecodeStringFieldsAsExpectedBoolean() {
		assertThat(decode("{ 'field': 'true' }", field("field", datatype(GAE.Boolean))).getProperty("field"))
				.isEqualTo(true);
	}

	@Test void testDecodeStringFieldsAsExpectedFloating() {
		assertThat(decode("{ 'field': '123' }", field("field", datatype(GAE.Floating))).getProperty("field"))
				.isEqualTo(123.0D);
	}

	@Test void testDecodeStringFieldsAsExpectedEntity() {
		assertThat(decode("{ 'field': '/path' }", field("field", and(datatype(GAE.Entity), clazz("Expected")))).getProperty("field"))
				.satisfies(entity -> assertThat(((EmbeddedEntity)entity).getKey()).isEqualTo(GAE.key("/path", "Expected")));
	}


	@Test void testDecodeArrayFields() {
		assertThat(decode("{ 'field': [null, 123, 'string'] }").getProperty("field"))
				.isEqualTo(asList(null, 123L, "string"));
	}


	@Test void testDecodeObjectFieldsEmpty() {
		assertThat(decode("{ 'field': {} }").getProperty("field"))

				.satisfies(entity -> assertThat(((EmbeddedEntity)entity).getKey()).isEqualTo(GAE.key("", "")));
	}

	@Test void testDecodeObjectFields() {
		assertThat(decode("{ 'field': { 'id': '/path', 'type': 'Class', 'label' : 123 } }").getProperty("field"))

				.isInstanceOf(EmbeddedEntity.class)

				.satisfies(entity -> assertThat(((EmbeddedEntity)entity).getKey()).isEqualTo(GAE.key("/path", "Class")))
				.satisfies(entity -> assertThat(((PropertyContainer)entity).getProperties().keySet()).containsOnly("label"))
				.satisfies(entity -> assertThat(((PropertyContainer)entity).getProperty("label")).isEqualTo(123L));
	}

	@Test void testDecodeObjectFieldsWithExpectedType() {
		assertThat(decode("{ 'field': { 'id': '/path' } }", field("field", clazz("Class"))).getProperty("field"))

				.satisfies(entity -> assertThat(((EmbeddedEntity)entity).getKey()).isEqualTo(GAE.key("/path", "Class")));
	}

}
