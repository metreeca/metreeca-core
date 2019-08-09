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

import com.metreeca.gae.GAETestBase;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.tree.shapes.And.and;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class EntityEncoderTest extends GAETestBase {

	private JsonObject encode(final Consumer<Entity> task) {

		final Entity entity=new Entity("*", "/");

		task.accept(entity);

		return new EntityEncoder().encode(entity, and());
	}


	@Test void testIgnoreNullFields() {
		assertThat(encode(entity -> entity.setProperty("field", null)))
				.doesNotContainKey("field");
	}

	@Test void testFormatBooleanFields() {
		assertThat(encode(entity -> entity.setProperty("field", true)))
				.containsEntry("field", JsonValue.TRUE);
	}

	@Test void testFormatIntegerFields() {
		assertThat(encode(entity -> entity.setProperty("field", 123)))
				.containsEntry("field", Json.createValue(123));
	}

	@Test void testFormatDoubleFields() {
		assertThat(encode(entity -> entity.setProperty("field", 123.0)))
				.containsEntry("field", Json.createValue(123.0D));
	}

	@Test void testFormatStringFields() {
		assertThat(encode(entity -> entity.setProperty("field", "string")))
				.containsEntry("field", Json.createValue("string"));
	}

	@Test void testFormatStringFieldsExceedingLimits() {

		final char[] chars=new char[2000];

		Arrays.fill(chars, '-');

		final String string=new String(chars);

		assertThat(encode(entity -> entity.setProperty("field", new Text(string))))
				.containsEntry("field", Json.createValue(string));
	}

	@Test void testFormatDateFields() {
		assertThat(encode(entity -> entity.setProperty("field", new Date(0))))
				.containsEntry("field", Json.createValue("1970-01-01T00:00:00Z"));
	}

	@Test void testFormatCollectionFields() {
		assertThat(encode(entity -> entity.setProperty("field", asList(null, 123, "string"))))
				.containsEntry("field", Json.createArrayBuilder()
						.add(JsonValue.NULL)
						.add(123)
						.add("string")
						.build()
				);
	}

	@Test void testFormatEntityFields() {
		assertThat(encode(entity -> {

			final EmbeddedEntity embedded=new EmbeddedEntity();

			embedded.setProperty("null", null);
			embedded.setProperty("nested", 123);

			entity.setProperty("field", embedded);

		})).containsEntry("field", Json.createObjectBuilder()

				.add("nested", 123L)

				.build()
		);
	}

}

