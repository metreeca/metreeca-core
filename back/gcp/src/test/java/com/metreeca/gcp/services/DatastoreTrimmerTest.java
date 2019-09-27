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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.formats.EntityFormat;
import com.metreeca.rest.Request;
import com.metreeca.tree.Shape;

import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


final class DatastoreTrimmerTest extends DatastoreTestBase {

	private Map<String, Value<?>> trim(final Shape shape, final Consumer<Entity.Builder> builder) {

		final Entity.Builder entity=Entity.newBuilder(service(datastore()).newKeyFactory().setKind(GCP.Resource).newKey("/entities/test"));

		builder.accept(entity);

		return new DatastoreTrimmer(service(datastore()))
				.trim(new Request()
						.shape(shape)
						.body(EntityFormat.entity(), entity.build())
				)
				.process(message -> message.body(EntityFormat.entity()))
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
