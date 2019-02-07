/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;

import com.metreeca.form.Form;
import com.metreeca.form.truths.JSONAssert;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static javax.json.Json.createObjectBuilder;


final class RelatorTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(small()))
				.exec(task)
				.clear();
	}


	@Nested final class Resource {

		private Request resource() {
			return new Request()
					.method(Request.GET)
					.base(Base)
					.path("/employees/1370");
		}


		@Nested final class Simple {

			@Test void testRelate() {
				exec(() -> new Relator()

						.handle(resource())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)
								.doesNotHaveShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.as("response RDF body contains a resource description")
										.hasStatement(response.item(), null, null)
								)
						)
				);
			}


			@Test void testUnknown() {
				exec(() -> new Relator()

						.handle(resource().path("/employees/9999"))

						.accept(response -> assertThat(response).hasStatus(Response.NotFound))
				);
			}

		}

		@Nested final class Shaped {

			private Request resource() {
				return Resource.this.resource()
						.roles(Manager)
						.shape(Employee);
			}


			@Test void testRelate() {
				exec(() -> new Relator()

						.handle(resource())

						.accept(response -> tool(Graph.Factory).query(connection -> {

							assertThat(response)

									.hasStatus(Response.OK)

									.hasShape()

									.hasBody(rdf(), rdf -> assertThat(rdf)
											.as("items retrieved")
											.hasSubset(construct(connection,
													"construct where { <employees/1370> a :Employee; :code ?c; :seniority ?s }"
											)));

						}))
				);
			}

			@Test void testRelatePartial() {
				exec(() -> new Relator()

						.handle(resource().roles(Salesman))

						.accept(response -> tool(Graph.Factory).query(connection -> {

							assertThat(response)

									.hasStatus(Response.OK)

									.hasBody(rdf(), rdf -> assertThat(rdf)

											.as("items retrieved")
											.hasSubset(construct(connection,
													"construct where { <employees/1370> a :Employee; :code ?c }"
											))

											.as("properties restricted to manager role not included")
											.doesNotHaveStatement(null, term("seniority"), null));

						}))
				);
			}


			@Test void testUnauthorized() {
				exec(() -> new Relator()

						.handle(resource().shape(when(guard(Form.role, Form.root), field(RDF.TYPE))))

						.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

				);
			}

			@Test void testForbidden() {
				exec(() -> new Relator()

						.handle(resource().shape(or()))

						.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

				);
			}

			@Test void testUnknown() {
				exec(() -> new Relator()

						.handle(resource().path("/employees/9999"))

						.accept(response -> assertThat(response).hasStatus(Response.NotFound))

				);
			}

		}

	}

	@Nested final class Container {

		private Request simple() {
			return new Request()
					.roles(Manager)
					.method(Request.GET)
					.base(Base)
					.path("/employees/");
		}


		@Nested final class Simple {

			@Test void testBrowse() {
				exec(() -> new Relator()

						.handle(simple().query("{ \"filter\": { \">\": 10 } }"))

						.accept(response -> assertThat(response)
								.hasStatus(Response.NotImplemented)
								.hasBody(json())
						)
				);
			}

		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple().shape(Employees);
			}


			@Test void testBrowse() {
				exec(() -> new Relator()

						.handle(shaped())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.hasStatement(response.item(), LDP.CONTAINS, null)
										.hasSubset(graph("construct where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"))
								)
						)
				);
			}

			@Test void testBrowseLimited() {
				exec(() -> new Relator()

						.handle(shaped().roles(Salesman))

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.hasSubset(graph("construct where { ?e a :Employee; rdfs:label ?label }"))

										.as("properties restricted to manager role not included")
										.doesNotHaveStatement(null, term("seniority"), null)
								)
						)
				);
			}

			@Test void testBrowseFiltered() {
				exec(() -> new Relator()

						.handle(shaped()
								.roles(Salesman)
								.query(createObjectBuilder()
										.add("filter", createObjectBuilder().add("title", "Sales Rep"))
										.build().toString()))

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)

										.hasSubset(graph(""
												+"construct { ?e a :Employee; :title ?t }\n"
												+"where { ?e a :Employee; :title ?t, 'Sales Rep' }"
										))

										.as("only resources matching filter included")
										.doesNotHaveStatement(null, term("title"), literal("President"))
								)
						)
				);
			}


			@Test void testDrivePreferEmptyContainer() {
				exec(() -> new Relator()

						.handle(shaped().header("Prefer", String.format(
								"return=representation; include=\"%s\"", LDP.PREFER_MINIMAL_CONTAINER
						)))

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)
								.hasHeader("Preference-Applied", response.request().header("Prefer").orElse(""))

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.doesNotHaveStatement(null, LDP.CONTAINS, null)
								)
						)
				);
			}


			@Test void testUnauthorized() {
				exec(() -> new Relator()

						.handle(shaped().roles(Form.none))

						.accept(response -> assertThat(response)
								.hasStatus(Response.Unauthorized)
								.doesNotHaveBody()
						)
				);
			}

			@Test void testForbidden() {
				exec(() -> new Relator()

						.handle(shaped().user(RDF.NIL).roles(Form.none))

						.accept(response -> assertThat(response)
								.hasStatus(Response.Forbidden)
								.doesNotHaveBody()
						)
				);
			}

			@Test void testRestricted() {
				exec(() -> new Relator()

						.handle(shaped()
								.user(RDF.NIL)
								.roles(Salesman)
								.query(createObjectBuilder()
										.add("items", "seniority")
										.build().toString())
						)

						.accept(response -> assertThat(response)
								.hasStatus(Response.UnprocessableEntity)
								.hasBody(json(), json -> JSONAssert.assertThat(json)
										.hasField("error")
								)
						)
				);
			}

		}

	}

}
