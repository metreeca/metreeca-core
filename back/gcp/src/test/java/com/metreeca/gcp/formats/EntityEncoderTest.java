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

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.services.DatastoreTestBase;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.shapes.And.and;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class EntityEncoderTest extends DatastoreTestBase {

	private JsonObject encode(final Consumer<Entity.Builder> task) {

		final Entity.Builder entity=Entity.newBuilder(service(datastore()).key(GCP.Resource, "/"));

		task.accept(entity);

		return new EntityEncoder().encode(entity.build(), and());
	}


	@Test void testHandleMetadata() {
		exec(() -> assertThat(encode(entity -> {}))
				.containsEntry("id", Json.createValue("/"))
		);
	}

	@Test void testIgnoreNullFields() {
		exec(() -> assertThat(encode(entity -> entity.setNull("field")))
				.doesNotContainKey("field")
		);
	}


	@Test void testEncodeBooleanFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", true)))
				.containsEntry("field", JsonValue.TRUE)
		);
	}

	@Test void testEncodeIntegerFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", 123)))
				.containsEntry("field", Json.createValue(123))
		);
	}

	@Test void testEncodeDoubleFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", 123.0)))
				.containsEntry("field", Json.createValue(123.0D))
		);
	}

	@Test void testEncodeStringFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", "string")))
				.containsEntry("field", Json.createValue("string"))
		);
	}

	@Test void testEncodeStringFieldsExceedingIndexingLimits() {

		final char[] chars=new char[2000];

		Arrays.fill(chars, '-');

		final String string=new String(chars);

		exec(() -> assertThat(encode(entity -> entity.set("field", string)))
				.containsEntry("field", Json.createValue(string))
		);
	}

	@Test void testEncodeDateFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", Timestamp.ofTimeMicroseconds(123_000))))
				.containsEntry("field", Json.createValue("1970-01-01T00:00:00.123Z"))
		);
	}

	@Test void testEncodeCollectionFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", asList(NullValue.of(), LongValue.of(123), StringValue.of("string")))))
				.containsEntry("field", Json.createArrayBuilder()
						.add(JsonValue.NULL)
						.add(123)
						.add("string")
						.build()
				)
		);
	}

	@Test void testEncodeEmbeddedEntityFields() {
		exec(() -> assertThat(encode(entity -> entity.set("field", Entity
				.newBuilder(service(datastore()).key("Embedded", "/path"))
				.setNull("null")
				.set("nested", 123)
				.build()
		))).containsEntry("field", Json.createObjectBuilder()

				.add("id", "/path")
				.add("nested", 123L)

				.build()
		));
	}

}

