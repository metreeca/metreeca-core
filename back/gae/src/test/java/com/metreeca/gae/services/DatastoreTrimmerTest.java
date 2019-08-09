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

import com.metreeca.gae.GAETestBase;
import com.metreeca.rest.Request;
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Consumer;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;


final class DatastoreTrimmerTest extends GAETestBase {

	private Map<String, Object> trim(final Shape shape, final Consumer<Entity> builder) {

		final Entity entity=new Entity("*", "*");

		builder.accept(entity);

		return new DatastoreTrimmer()
				.trim(new Request()
						.shape(shape)
						.body(entity(), entity)
				)
				.process(message -> message.body(entity()))
				.fold(PropertyContainer::getProperties, failure -> fail("missing message body"));
	}


	@Test void testRetainOnlyCompatibleFields() {
		exec(() -> assertThat(trim(field("field", and()), entity -> {

					entity.setProperty("field", "value");
					entity.setProperty("unknown", "value");

				}))

						.containsEntry("field", "value")
						.doesNotContainEntry("unknown", "value")

		);
	}

	@Test void testTrimEmbeddedEntities() {
		exec(() -> assertThat(((PropertyContainer)trim(

				field("embedded", field("field", and())),

				entity -> {

					final EmbeddedEntity embedded=new EmbeddedEntity();

					embedded.setProperty("field", "value");
					embedded.setProperty("unknown", "value");

					entity.setProperty("embedded", embedded);

				}).get("embedded")).getProperties())

				.containsEntry("field", "value")
				.doesNotContainEntry("unknown", "value")

		);
	}

}
