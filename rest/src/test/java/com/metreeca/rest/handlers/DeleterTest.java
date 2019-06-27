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

package com.metreeca.rest.handlers;


import com.metreeca.form.truths.JsonAssert;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca._repo.GraphEngine;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.GraphTest;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Form.none;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.rdf.Graph.graph;
import static com.metreeca.tray.rdf.GraphTest.model;


final class DeleterTest {

	private static final Model Dataset=small();


	private void exec(final Runnable ...tasks) {
		new Tray()
				.set(engine(), GraphEngine::new)
				.set(graph(), GraphTest::graph)
				.exec(model(small()))
				.exec(tasks)
				.clear();
	}


	@Nested final class Resource {

		private Request simple() {
			return new Request()
					.roles(Manager)
					.method(Request.DELETE)
					.base(Base)
					.path("/employees/1370");
		}


		@Nested final class Simple {

			@Test void testDelete() {
				exec(() -> new Deleter()

						.handle(simple())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NoContent)
									.doesNotHaveBody();

							assertThat(model("construct where { <employees/1370> ?p ?o }"))
									.as("cell deleted")
									.isEmpty();

							assertThat(model("construct where { ?s ?p <employees/1370> }"))
									.as("inbound links removed")
									.isEmpty();

							assertThat(model("construct where { <employees/1102> rdfs:label ?o }"))
									.as("connected resources preserved")
									.isNotEmpty();

						}));
			}


			@Test void testUnknown() {
				exec(() -> new Deleter()

						.handle(simple().path("/unknown"))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NotFound)
									.doesNotHaveBody();

							assertThat(model())
									.as("graph unchanged")
									.isIsomorphicTo(Dataset);

						}));
			}
		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple().shape(Employee);
			}


			@Test void testDelete() {
				exec(() -> new Deleter()

						.handle(shaped())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NoContent)
									.doesNotHaveBody();

							assertThat(model("construct where { <employees/1370> ?p ?o }"))
									.isEmpty();

						}));
			}


			@Test void testUnauthorized() {
				exec(() -> new Deleter()

						.handle(shaped().roles(none))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Unauthorized)
									.doesNotHaveBody();

							assertThat(model())
									.as("graph unchanged")
									.isIsomorphicTo(Dataset);

						}));
			}

			@Test void testForbidden() {
				exec(() -> new Deleter()

						.handle(shaped().user(RDF.NIL).roles(none))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Forbidden)
									.doesNotHaveBody();

							assertThat(model())
									.as("graph unchanged")
									.isIsomorphicTo(Dataset);

						}));
			}

			@Test void testUnknown() {
				exec(() -> new Deleter()

						.handle(shaped().path("/unknown"))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NotFound)
									.doesNotHaveBody();

							assertThat(model())
									.as("graph unchanged")
									.isIsomorphicTo(Dataset);

						}));
			}

		}

	}

	@Nested final class Container {

		private Request simple() {
			return new Request()
					.roles(Manager)
					.method(Request.DELETE)
					.base(Base)
					.path("/employees/");
		}


		@Nested final class Simple {

			@Test void testNotImplemented() {
				exec(() -> new Deleter()

						.handle(simple())

						.accept(response -> assertThat(response)
								.hasStatus(Response.NotImplemented)
								.hasBody(json(), json -> JsonAssert.assertThat(json)
										.hasField("cause")
								)
						)
				);
			}

		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple().shape(Employees);
			}


			@Test void testNotImplemented() {
				exec(() -> new Deleter()

						.handle(shaped())

						.accept(response -> assertThat(response)
								.hasStatus(Response.NotImplemented)
								.hasBody(json(), json -> JsonAssert.assertThat(json)
										.hasField("cause")
								)
						)
				);
			}

		}

	}

}
