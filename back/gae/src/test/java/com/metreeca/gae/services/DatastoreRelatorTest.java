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
import com.metreeca.gae.GAETest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.appengine.api.datastore.Entity;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Type.type;

import static java.util.Arrays.asList;


final class DatastoreRelatorTest extends GAETest {

	@Nested final class Container {

		@Test void test() {
			exec(
					() -> service(datastore()).exec(datastore -> datastore.put(asList(
							new Entity("Office", "/offices/1"),
							new Entity("Office", "/offices/2"),
							new Entity("Employee", "/employees/2"),
							new Entity("Employee", "/employees/2")
					))),

					() -> new DatastoreEngine()

							.handle(new Request()
									.method(Request.GET)
									.path("/offices/1")
									.shape(and(
											clazz("Office"),
											field("label", and(required(), type(GAE.String)))
									))
							)

							.accept(response -> assertThat(response)
									.hasStatus(Response.OK)
									.hasShape()
									.hasBody(entity())
							)

			);
		}

	}

	@Nested final class Resource {

	}

}
