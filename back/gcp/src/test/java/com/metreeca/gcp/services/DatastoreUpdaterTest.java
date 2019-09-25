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
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.cloud.datastore.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;


final class DatastoreUpdaterTest extends DatastoreTestBase {

	@Nested final class Container {

		@Test void testReject() {
			exec(() -> new DatastoreUpdater()

					.handle(new Request()
							.path("/entities/")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
					)

			);
		}

	}

	@Nested final class Resource {

		@Test void testUpdate() {
			exec(() -> {

				final Datastore service=service(datastore());

				final Key key=service.key(GCP.Resource, "/entities/test");

				final Entity original=Entity.newBuilder(key)
						.set("code", "test")
						.set("label", "Entity")
						.build();

				final Entity updated=Entity.newBuilder(original)
						.set("label", "Entity (Updated)")
						.build();

				service.exec(datastore -> datastore.put(original));

				new DatastoreUpdater()

						.handle(new Request()
								.path(key.getName())
								.body(entity(), updated)
						)

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NoContent)
									.doesNotHaveBody();

							service.exec(datastore -> {

								final EntityQuery query=Query.newEntityQueryBuilder()
										.setKind(GCP.Resource)
										.build();

								assertThat((Iterable<Entity>)() -> datastore.run(query))
										.containsExactly(updated);

								return this;

							});

						});

			});
		}


		@Test void testRejectMissing() {
			exec(() -> new DatastoreUpdater()

					.handle(new Request()
							.path("/entities/9999")
							.body(entity(), Entity.newBuilder(service(datastore()).key(
									"Test", "/entities/9999")
							).build())
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
							.doesNotHaveBody()
					)

			);
		}

	}

}
