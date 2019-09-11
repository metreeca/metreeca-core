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
import com.metreeca.gae.GAETestBase;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tree.shapes.Clazz.clazz;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;


final class DatastoreDeleterTest extends GAETestBase {

	@Nested final class Container {

		@Test void testReject() {
			exec(() -> new DatastoreDeleter()

					.handle(new Request()
							.path("/offices/")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
					)

			);
		}

	}

	@Nested final class Resource {

		private List<Entity> entities(final String... codes) {
			return Arrays.stream(codes).map(code -> {

				final Entity entity=new Entity(GAE.key(String.format("/entities/%s", code), "Entity"));

				entity.setProperty("code", code);

				return entity;

			}).collect(toList());
		}


		@Test void testDelete() {
			exec(load(entities("1", "2", "3")), () -> new DatastoreDeleter()

					.handle(new Request()
							.path("/entities/1")
							.shape(clazz("Entity"))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.NoContent)
								.doesNotHaveBody();

						service(datastore()).exec(service -> {

							assertThat(service.prepare(new Query("Entity")).asIterable())
									.containsExactlyInAnyOrderElementsOf(entities("2", "3"));

							return this;

						});

					})

			);
		}

		@Test void testRejectMissing() {
			exec(() -> new DatastoreDeleter()

					.handle(new Request()
							.path("/entities/9999")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
							.doesNotHaveBody()
					)

			);
		}

	}

}
