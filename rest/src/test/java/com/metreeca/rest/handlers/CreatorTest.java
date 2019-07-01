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

import com.metreeca.form.Form;
import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.Values;
import com.metreeca.form.truths.JsonAssert;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.form.truths.ValueAssert;
import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.function.Function;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.Tray.tool;


final class CreatorTest {

	private void exec(final Runnable... tasks) {
		new Tray()
				.set(engine(), EngineMock::new)
				.exec(tasks)
				.clear();
	}


	private Function<Request, Request> body(final String rdf) {
		return request -> request.body(input(), () -> Codecs.input(new StringReader(turtle(rdf))));
	}


	@Nested final class Resource {

		private Request simple() {
			return new Request()
					.roles(Manager)
					.method(Request.POST)
					.base(Base)
					.path("/employees/9999");
		}


		@Nested final class Simple {

			@Test void testNotImplemented() {
				exec(() -> new Creator()

						.handle(simple())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NotImplemented)
									.hasBody(json(), json -> JsonAssert.assertThat(json)
											.hasField("cause")
									);

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("storage unchanged")
									.isEmpty();

						})
				);
			}

		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple().shape(Employee);
			}


			@Test void testNotImplemented() {
				exec(() -> new Creator()

						.handle(shaped())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NotImplemented)
									.hasBody(json(), json -> JsonAssert.assertThat(json)
											.hasField("cause")
									);

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("storage unchanged")
									.isEmpty();

						})
				);
			}

		}

	}

	@Nested final class Container {

		private Request simple() {
			return new Request()
					.roles(Manager)
					.method(Request.POST)
					.base(Base)
					.path("/employees/")
					.map(body("<>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :email 'tfaussone@classicmodelcars.com' ;"
							+" :title 'Sales Rep' ;"
							+" :seniority 1 ."
					));
		}


		@Nested final class Simple { // containers are virtual => no unknown error

			@Test void testCreate() {
				exec(() -> new Creator()

						.handle(simple())

						.accept(response -> {

							final IRI container=response
									.request()
									.item();

							final IRI resource=response
									.header("Location")
									.map(Values::iri)
									.orElse(null);

							assertThat(response)
									.hasStatus(Response.Created)
									.doesNotHaveBody();

							ValueAssert.assertThat(resource)
									.as("resource created with IRI stemmed on request focus")
									.hasNamespace(container.stringValue());

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("resource description stored into the graph")
									.hasSubset(
											statement(resource, term("forename"), literal("Tino")),
											statement(resource, term("surname"), literal("Faussone"))
									);

							//ModelAssert.assertThat(model())
							//		.as("basic container connected to resource description")
							//		.hasSubset(
							//				statement(container, LDP.CONTAINS, resource)
							//		);

						}));
			}

			@Test void testCreateSlug() {
				exec(() -> new Creator((request, model) -> "slug")

						.handle(simple())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Created)
									.doesNotHaveBody();

							ValueAssert.assertThat(response.item())
									.as("resource created with computed IRI")
									.isEqualTo(item("employees/slug"));

							//ModelAssert.assertThat(model())
							//		.hasSubset(
							//				statement(response.item(), term("forename"), literal("Tino")),
							//				statement(response.item(), term("surname"), literal("Faussone"))
							//		);

						}));
			}


			@Test void testMalformedData() {
				exec(() -> new Creator()

						.handle(simple().map(body("!!!")))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.BadRequest)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> JsonAssert.assertThat(json)
											.hasField("error")
									);

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testExceedingData() {
				exec(() -> new Creator()

						.handle(simple().map(body("<>"
								+" :forename 'Tino' ;"
								+" :surname 'Faussone' ;"
								+" :office <offices/1> . <offices/1> :value 'exceeding' ."
						)))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> JsonAssert.assertThat(json)
											.hasField("error")
									);

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testConflictingSlug() {
				exec(() -> {

					final Handler creator=new Creator((request, model) -> "slug");

					creator.handle(simple()).accept(response -> {});

					//final Model snapshot=model();

					creator.handle(simple()).accept(response -> {

						assertThat(response)
								.hasStatus(Response.InternalServerError);

						//assertThat(model())
						//		.as("graph unchanged")
						//		.isIsomorphicTo(snapshot);

					});

				});
			}

		}

		@Nested final class Shaped { // containers are virtual => no unknown error

			private Request shaped() {
				return simple().shape(Employees);
			}


			@Test void testCreate() {
				exec(() -> new Creator()

						.handle(shaped())

						.accept(response -> {

							final IRI location=response
									.header("Location")
									.map(Values::iri)
									.orElse(null);

							assertThat(response)
									.hasStatus(Response.Created)
									.doesNotHaveBody();

							ValueAssert.assertThat(location)
									.as("resource created with IRI stemmed on request focus")
									.hasNamespace(response.request().item().stringValue());

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("resource description stored into the graph")
									.hasSubset(
											statement(location, RDF.TYPE, term("Employee")),
											statement(location, term("forename"), literal("Tino")),
											statement(location, term("surname"), literal("Faussone"))
									);

						}));
			}

			@Test void testCreateSlug() {
				exec(() -> new Creator((request, model) -> "slug")

						.handle(shaped())

						.accept(response -> {

							final IRI location=response.header("Location")
									.map(Values::iri)
									.orElse(null);

							assertThat(response)
									.hasStatus(Response.Created)
									.doesNotHaveBody();

							ValueAssert.assertThat(location)
									.as("resource created with computed IRI")
									.isEqualTo(item("employees/slug"));

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("resource description stored into the graph")
									.hasSubset(
											statement(location, RDF.TYPE, term("Employee")),
											statement(location, term("forename"), literal("Tino")),
											statement(location, term("surname"), literal("Faussone"))
									);

						}));
			}


			@Test void testUnauthorized() {
				exec(() -> new Creator()

						.handle(shaped().roles(Form.none))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Unauthorized)
									.doesNotHaveHeader("Location")
									.doesNotHaveBody();

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testForbidden() {
				exec(() -> new Creator()

						.handle(shaped().shape(or()))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Forbidden)
									.doesNotHaveHeader("Location")
									.doesNotHaveBody();

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testMalformedData() {
				exec(() -> new Creator()

						.handle(shaped().map(body("!!!")))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.BadRequest)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> JsonAssert.assertThat(json).hasField("error"));

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testInvalidData() {
				exec(() -> new Creator()

						.handle(shaped().map(body("<>"
								+" :forename 'Tino' ;"
								+" :surname 'Faussone'. "
						)))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> JsonAssert.assertThat(json).hasField("error"));

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testRestrictedData() {
				exec(() -> new Creator()

						.handle(shaped()
								.roles(Salesman))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> JsonAssert.assertThat(json).hasField("error"));

							ModelAssert.assertThat(tool(engine()).relate(response.item(), edges(and())))
									.as("graph unchanged")
									.isEmpty();

						}));
			}

			@Test void testConflictingSlug() {
				exec(() -> {

					final Creator creator=new Creator((request, model) -> "slug");

					creator.handle(shaped()).accept(response -> {});

					//final Model snapshot=model();

					creator.handle(shaped()).accept(response -> {

						assertThat(response)
								.hasStatus(Response.InternalServerError);

						//assertThat(model())
						//		.as("graph unchanged")
						//		.isIsomorphicTo(snapshot);

					});

				});
			}

		}

	}

}
