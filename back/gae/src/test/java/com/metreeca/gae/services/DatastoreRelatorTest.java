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
import com.metreeca.tree.Shape;

import com.google.appengine.api.datastore.Entity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tree.Shape.optional;
import static com.metreeca.tree.Shape.relate;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class DatastoreRelatorTest extends GAETestBase {

	private Runnable dataset() {
		return () -> service(datastore()).exec(datastore -> datastore.put(birt()));
	}


	private Shape employee() {
		return and(

				clazz("Employee"),

				field("code", and(required(), datatype(GAE.String))),
				field("label", and(required(), datatype(GAE.String))),
				field("forename", and(required(), datatype(GAE.String))),
				field("surname", and(required(), datatype(GAE.String))),
				field("email", and(required(), datatype(GAE.String))),

				field("title", and(required(), datatype(GAE.String))),
				field("seniority", and(required(), datatype(GAE.Integral))),

				field("office", and(required(), clazz("Office"), relate().then(
						field("label", and(required(), datatype(GAE.String)))
				))),

				field("supervisor", and(optional(), clazz("Employee"), relate().then(
						field("label", and(required(), datatype(GAE.String)))
				)))

		);
	}


	@Nested final class Container {

		@Nested final class Items {

			@Test void test() {
				exec(dataset(), () -> new DatastoreRelator()

						.handle(new Request()
								.path("/employees/")
								.shape(employee())
						)

						.accept(response -> assertThat(response)
								.hasStatus(Response.OK)
								.hasShape()
								.hasBody(entity(), entity-> assertThat(entity.getProperty(GAE.contains))
										.isEqualTo(asList())
								)
						)

				);
			}

		}

	}

	@Nested final class Resource {

		@Test void testRelateResource() {
			exec(dataset(), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/offices/1")
							.shape(and(
									clazz("Office"),
									field("label", and(required(), datatype(GAE.String)))
							))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasShape()
							.hasBody(entity(), entity -> assertThat(((Entity)entity).getKey().getName())
									.isEqualTo("/offices/1")
							)
					)

			);
		}

		@Test void testHandleUnknownResources() {
			exec(dataset(), () -> new DatastoreRelator()

					.handle(new Request()
							.path("/offices/9999")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
							.doesNotHaveShape()
							.doesNotHaveBody(entity())
					)

			);
		}

	}

}
