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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.rest.Request;
import com.metreeca.tree.Shape;

import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


final class DatastoreTrimmerTest extends DatastoreTestBase {

	private Map<String, Value<?>> trim(final Shape shape, final Consumer<Entity.Builder> builder) {

		final Entity.Builder entity=Entity.newBuilder(service(datastore()).key("/entities/test", GAE.Resource));

		builder.accept(entity);

		return new DatastoreTrimmer()
				.trim(new Request()
						.shape(shape)
						.body(entity(), entity.build())
				)
				.process(message -> message.body(entity()))
				.fold(BaseEntity::getProperties, failure -> fail("missing message body"));
	}


	@Test void testRetainOnlyCompatibleFields() {
		exec(() -> assertThat(trim(field("field", and()), entity -> entity

						.set("field", "value")
						.set("unknown", "value")

				))

						.containsEntry("field", StringValue.of("value"))
						.doesNotContainEntry("unknown", StringValue.of("value"))

		);
	}

	@Test void testTrimEmbeddedEntities() {
		exec(() -> assertThat(((EntityValue)trim(

				field("embedded", field("field", and())),

				entity -> {

					entity.set("embedded", FullEntity.newBuilder()

							.set("field", "value")
							.set("unknown", "value")
							.build()

					);

				}).get("embedded")).get().getProperties())

				.containsEntry("field", StringValue.of("value"))
				.doesNotContainEntry("unknown", StringValue.of("value"))

		);
	}

}
