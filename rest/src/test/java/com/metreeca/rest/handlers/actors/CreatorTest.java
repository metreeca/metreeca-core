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
import com.metreeca.form.things.Values;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.function.Function;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.JSONAssert.assertThat;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.form.truths.ValueAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONFormat.json;


@Disabled final class CreatorTest {

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}


	private Request simple() {
		return new Request()
				.roles(Manager)
				.method(Request.POST)
				.base(Base)
				.path("/employees/")
				.map(body("@prefix : <http://example.com/terms#>. <>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone' ;"
						+" :email 'tfaussone@classicmodelcars.com' ;"
						+" :title 'Sales Rep' ;"
						+" :seniority 1 ."
				));
	}

	private Request shaped() {
		return simple()
				.shape(Employee);
	}


	private Function<Request, Request> body(final String rdf) {
		return request -> request.body(input(), () -> Codecs.input(new StringReader(rdf)));
	}


	@Nested final class Simple {

		@Test void testCreate() {
			exec(() -> new Creator()

					.handle(simple())

					.accept(response -> {

						final IRI location=response
								.header("Location")
								.map(Values::iri)
								.orElse(null);

						assertThat(response)
								.hasStatus(Response.Created)
								.doesNotHaveBody();

						assertThat(location)
								.as("resource created with IRI stemmed on request focus")
								.hasNamespace(response.request().item().stringValue());

						assertThat(graph())
								.as("resource description stored into the graph")
								.hasSubset(
										statement(location, term("forename"), literal("Tino")),
										statement(location, term("surname"), literal("Faussone"))
								);

						assertThat(graph())
								.as("basic container connected to resource description")
								.hasSubset(
										statement(response.request().item(), LDP.CONTAINS, location)
								);

					}));
		}

		@Test void testCreateSlug() {
			exec(() -> new Creator((request, model) -> "slug")

					.handle(simple())

					.accept(response -> {

						final IRI location=response.header("Location")
								.map(Values::iri)
								.orElse(null);

						assertThat(response)
								.hasStatus(Response.Created)
								.doesNotHaveBody();

						assertThat(location)
								.as("resource created with computed IRI")
								.isEqualTo(item("employees/slug"));

						assertThat(graph()).hasSubset(
								statement(location, term("forename"), literal("Tino")),
								statement(location, term("surname"), literal("Faussone"))
						);

					}));
		}


		@Test void testMalformedData() {
			exec(() -> new Creator()

					.handle(simple().map(body("!!!")))

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.BadRequest)
								.doesNotHaveHeader("Location")
								.hasBody(json(), json -> assertThat(json)
										.hasField("error")
								);

						assertThat(graph())
								.as("graph unchanged")
								.isEmpty();

					}));
		}

		@Test void testExceedingData() {
			exec(() -> new Creator()

					.handle(simple().map(body("@prefix : <http://example.com/terms#>. <>"
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
								.isEmpty();

					}));
		}

	}


	@Nested final class Shaped {

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

						assertThat(location)
								.as("resource created with IRI stemmed on request focus")
								.hasNamespace(response.request().item().stringValue());

						assertThat(graph())
								.as("resource description stored into the graph")
								.hasSubset(
										statement(location, RDF.TYPE, term("Employee")),
										statement(location, term("forename"), literal("Tino")),
										statement(location, term("surname"), literal("Faussone"))
								);

						assertThat(graph())
								.as("basic container connected to resource description")
								.hasSubset(
										statement(response.request().item(), LDP.CONTAINS, location)
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

						assertThat(location)
								.as("resource created with computed IRI")
								.isEqualTo(item("employees/slug"));

						assertThat(graph()).hasSubset(
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

						assertThat(graph())
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

						assertThat(graph())
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
								.hasBody(json(), json -> assertThat(json).hasField("error"));

						assertThat(graph())
								.as("graph unchanged")
								.isEmpty();

					}));
		}

		@Test void testInvalidData() {
			exec(() -> new Creator()

					.handle(shaped().map(body("@prefix : <http://example.com/terms#>. <>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone'. "
					)))

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.UnprocessableEntity)
								.doesNotHaveHeader("Location")
								.hasBody(json(), json -> assertThat(json).hasField("error"));

						assertThat(graph())
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
								.hasBody(json(), json -> assertThat(json).hasField("error"));

						assertThat(graph())
								.as("graph unchanged")
								.isEmpty();

					}));
		}

	}

}
