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
import com.metreeca.form.things.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.function.Function;

import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.JSONAssert.assertThat;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.tray.rdf.GraphTest.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONFormat.json;


final class UpdaterTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(small()))
				.exec(task)
				.clear();
	}


	private Function<Request, Request> body(final String rdf) {
		return request -> request.body(input(), () -> Codecs.input(new StringReader(rdf)));
	}


	@Nested final class Resource {

		private Request request() {
			return new Request()
					.roles(Manager)
					.method(Request.POST)
					.base(Base)
					.path("/employees/1370") // Gerard Hernandez
					.map(body("@prefix : <http://example.com/terms#> . <>"
							+":forename 'Tino';"
							+":surname 'Faussone';"
							+":email 'tfaussone@example.com';"
							+":title 'Sales Rep' ;"
							+":seniority 5 ." // outside salesman envelope
					));
		}


		@Nested final class Simple {

			@Test void testUpdate() {
				exec(() -> new Updater()

						.handle(request())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NoContent)
									.doesNotHaveBody();

							assertThat(graph())

									.as("graph updated")

									.hasSubset(decode("@prefix : <http://example.com/terms#> . </employees/1370>"
											+":forename 'Tino';"
											+":surname 'Faussone';"
											+":email 'tfaussone@example.com';"
											+":title 'Sales Rep' ;"
											+":seniority 5 ."
									))

									.doesNotHaveStatement(item("employees/1370"), term("forename"), literal("Gerard"))
									.doesNotHaveStatement(item("employees/1370"), term("surname"), literal("Hernandez"));

						}));
			}


			@Test void testMalformedData() {
				exec(() -> new Updater()

						.handle(request().map(body("!!!")))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.BadRequest)
									.hasBody(json(), json -> assertThat(json)
											.hasField("error")
									);

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

			@Test void testExceedingData() {
				exec(() -> new Updater()

						.handle(request().map(body("@prefix : <http://example.com/terms#>. <>"
								+" :forename 'Tino' ;"
								+" :surname 'Faussone' ;"
								+" :office <offices/1> . <offices/1> :value 'exceeding' ."
						)))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.doesNotHaveHeader("Location")
									.hasBody(json(), json -> assertThat(json)
											.hasField("error")
									);

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

		}

		@Nested final class Shaped {

			private Request request() {
				return Resource.this.request().shape(Employees);
			}


			@Test void testUpdate() {
				exec(() -> new Updater()

						.handle(request())

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.NoContent)
									.doesNotHaveBody();

							assertThat(graph())

									.as("graph updated")

									.hasSubset(decode("</employees/1370>"
											+":forename 'Tino';"
											+":surname 'Faussone';"
											+":email 'tfaussone@example.com';"
											+":title 'Sales Rep' ;"
											+":seniority 5 ."
									))

									.doesNotHaveStatement(item("employees/1370"), term("forename"), literal("Gerard"))
									.doesNotHaveStatement(item("employees/1370"), term("surname"), literal("Hernandez"));

						}));
			}


			@Test void testUnauthorized() {
				exec(() -> new Updater()

						.handle(request().roles(Form.none))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Unauthorized)
									.doesNotHaveBody();

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

			@Test void testForbidden() {
				exec(() -> new Updater()

						.handle(request().user(RDF.NIL).roles(Form.none))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.Forbidden)
									.doesNotHaveBody();

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

			@Test void testMalformedData() {
				exec(() -> new Updater()

						.handle(request().map(body("!!!")))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.BadRequest)
									.hasBody(json(), json -> assertThat(json)
											.hasField("error"));

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

			@Test void testInvalidData() {
				exec(() -> new Updater()

						.handle(request().map(body("@prefix : <http://example.com/terms#>. <employees/1370>"
								+":forename 'Tino';"
								+":surname 'Faussone';"
								+":email 'tfaussone@example.com' ;"
								+":title 'Sales Rep'." // missing seniority/supervisor/subordinate
						)))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.hasBody(json(), json -> assertThat(json)
											.hasField("error"));

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

			@Test void testRestrictedData() {
				exec(() -> new Updater()

						.handle(request().roles(Salesman))

						.accept(response -> {

							assertThat(response)
									.hasStatus(Response.UnprocessableEntity)
									.hasBody(json(), json -> assertThat(json)
											.hasField("error")
									);

							assertThat(graph())
									.as("graph unchanged")
									.isIsomorphicTo(small());

						}));
			}

		}

	}

	@Nested final class Container {

		private Request request() {
			return new Request()
					.roles(Manager)
					.method(Request.POST)
					.base(Base)
					.path("/employees/")
					.map(body("@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>. <> rdfs:label 'Updated!'."));
		}

		@Test void testUpdate() {
			exec(() -> new Updater()

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotImplemented)
							.hasBody(json(), json -> assertThat(json)
									.hasField("cause")
							)
					)
			);
		}

	}

}
