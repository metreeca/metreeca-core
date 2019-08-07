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
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonValue;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.DataInvalid;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Type.type;

import static org.assertj.core.api.Assertions.assertThat;


final class DatastoreCreatorTest  extends GAETestBase {

	@Test void test() {
		exec(() -> new DatastoreEngine()

				.handle(new Request()
						.method(Request.POST)
						.path("/container/")
						.shape(and(
								clazz("Entity"),
								field("label", and(required(), type(GAE.String)))
						))
						.header("Slug", "slug")
						.body(json(), Json.createObjectBuilder()
								.add("label", "entity")
								.build()
						)
				)

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Created)
							.hasHeader("Location", "/container/slug");

					service(datastore()).exec(service -> {
						try {

							final Entity entity=service.get(KeyFactory.createKey("Entity", "/container/slug"));

							assertThat(entity.getProperties())
									.containsEntry("label", "entity");

							return this;

						} catch ( final EntityNotFoundException e ) {
							throw new RuntimeException(e);
						}
					});

				})

		);
	}

	@Test void testRejectClashingSlug() {
		exec(

				() -> service(datastore()).exec(service ->
						service.put(new Entity("Entity", "/id"))
				),

				() -> new DatastoreEngine()

						.handle(new Request()
								.method(Request.POST)
								.path("/")
								.shape(clazz("Entity"))
								.header("Slug", "id")
								.body(json(), JsonValue.EMPTY_JSON_OBJECT)
						)

						.accept(response -> assertThat(response)
								.hasStatus(Response.InternalServerError)
								.doesNotHaveHeader("Location")
						)
		);
	}

	@Test void testRejectInvalidPayload() {
		exec(() -> new DatastoreEngine()

				.handle(new Request()
						.method(Request.POST)
						.path("/")
						.shape(field("label", type(GAE.String)))
						.body(json(), Json.createObjectBuilder()
								.add("label", 123)
								.build()
						)
				)

				.accept(response -> assertThat(response)
						.hasStatus(Response.UnprocessableEntity)
						.doesNotHaveHeader("Location")
						.hasBody(json(), json -> assertThat(json)
								.containsEntry("error", Json.createValue(DataInvalid))
								.containsKey("trace")
						)
				)
		);
	}

	@Test void testRejectPOSTToResources() {
		exec(() -> new DatastoreEngine()

				.handle(new Request()
						.method(Request.POST)
						.path("/resource")
						.body(json(), JsonValue.EMPTY_JSON_OBJECT)
				)

				.accept(response -> assertThat(response)
						.hasStatus(Response.InternalServerError)
				)

		);
	}

}
