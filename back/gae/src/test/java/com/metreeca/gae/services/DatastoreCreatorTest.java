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

import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.ValueType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonValue;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;
import static org.assertj.core.api.Assertions.assertThat;


final class DatastoreCreatorTest extends DatastoreTestBase {

	@Nested final class Container {

		@Test void testCreateResource() {
			exec(() -> new DatastoreCreator()

					.handle(new Request()
							.path("/container/")
							.shape(and(
									clazz("Entity"),
									field("label", and(required(), datatype(ValueType.STRING)))
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

							final Entity entity=service.run(Query.newEntityQueryBuilder()
									.setKind("Entity")
									.setFilter(eq("label", "entity"))
									.build()
							).next();

							assertThat(entity.getKey().getName()).isEqualTo("/container/slug");

							return this;

						});

					})

			);
		}

		@Test void testRejectClashingSlug() {

			final Request request=new Request()
					.path("/")
					.shape(clazz("Entity"))
					.header("Slug", "id")
					.body(json(), JsonValue.EMPTY_JSON_OBJECT);

			exec(

					() -> new DatastoreCreator()

							.handle(request),

					() -> new DatastoreCreator()

							.handle(request)

							.accept(response -> assertThat(response)
									.hasStatus(Response.InternalServerError)
									.doesNotHaveHeader("Location")
							)
			);
		}

	}

	@Nested final class Resource {

		@Test void testReject() {
			exec(() -> new DatastoreCreator()

					.handle(new Request()
							.path("/resource")
							.body(json(), JsonValue.EMPTY_JSON_OBJECT)
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
					)

			);
		}

	}

}
