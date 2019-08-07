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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.Date;

import javax.json.Json;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Type.type;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class EntityDecoderTest extends GAETestBase {

	private Entity decode(final String json) {
		return decode(json, and());
	}

	private Entity decode(final String json, final Shape shape) {
		return new EntityDecoder().decode(
				Json.createReader(new StringReader(json.replace('\'', '"'))).readObject(), shape, "/"
		);
	}


	@Test void testParseEntity() {
		assertThat(decode("{}")).isEqualTo(new Entity("*", "/"));
	}

	@Test void testParseEntityWithExpectedKind() {
		assertThat(decode("{}", clazz("Class"))).isEqualTo(new Entity("Class", "/"));
	}


	@Test void testIgnoreNullFields() {
		assertThat(decode("{ 'field': null }").hasProperty("field")).isFalse();
	}

	@Test void testParseBooleanFields() {
		assertThat(decode("{ 'field': true }").getProperty("field")).isEqualTo(true);
	}

	@Test void testParseIntegerFields() {
		assertThat(decode("{ 'field': 123 }").getProperty("field")).isEqualTo(123L);

	}

	@Test void testParseDoubleFields() {
		assertThat(decode("{ 'field': 123.0 }").getProperty("field")).isEqualTo(123.0D);

	}

	@Test void testParseIntegerFieldsExpectedDouble() {
		assertThat(decode("{ 'field': 123 }", field("field", type(GAE.Floating))).getProperty("field"))
				.isEqualTo(123.0D);
	}

	@Test void testParseStringFields() { // !!! reject overly long strings
		assertThat(decode("{ 'field': 'string' }").getProperty("field")).isEqualTo("string");
	}

	@Test void testParseStringFieldsAsExpectedDate() {
		assertThat(decode("{ 'field': '2019-01-01T00:00Z' }", field("field", type(GAE.Date))).getProperty("field"))
				.isEqualTo(Date.from(OffsetDateTime.parse("2019-01-01T00:00Z").toInstant()));
	}

	@Test void testParseArrayFields() {
		assertThat(decode("{ 'field': [123, 'string'] }").getProperty("field")).isEqualTo(asList(123L, "string"));
	}

	@Test void testParseObjectFields() {
		assertThat(decode("{ 'field': { 'label' : 123 } }").getProperty("field")).satisfies(entity ->
				assertThat(((PropertyContainer)entity).getProperty("label")).isEqualTo(123L)
		);
	}

}
