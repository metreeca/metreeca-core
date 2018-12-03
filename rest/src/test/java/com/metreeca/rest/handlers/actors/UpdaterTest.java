/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.things.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.function.Function;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONFormat.json;


final class UpdaterTest {

	private static final Model Dataset=small();


	private void exec(final Runnable task) {
		new Tray().exec(graph(Dataset), task).clear();
	}


	private Request direct() {
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

	private Request driven() {
		return direct()
				.shape(Employee);
	}


	private Function<Request, Request> body(final String rdf) {
		return request -> request.body(input(), () -> Codecs.input(new StringReader(rdf)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectUpdate() {
		exec(() -> new Updater()

				.handle(direct())

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


	@Test void testDirectUnauthorized() {
		exec(() -> new Updater().roles(Manager)

				.handle(direct().user(Form.none).roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDirectForbidden() {
		exec(() -> new Updater().roles(Manager)

				.handle(direct().user(RDF.NIL).roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Forbidden)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDirectMalformedData() {
		exec(() -> new Updater()

				.handle(direct().map(body("!!!")))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.BadRequest)
							.hasBodyThat(json())
							.hasField("error");

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDirectExceedingData() {
		exec(() -> new Creator()

				.handle(direct().map(body("@prefix : <http://example.com/terms#>. <>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone' ;"
						+" :office <offices/1> . <offices/1> :value 'exceeding' ."
				)))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.doesNotHaveHeader("Location")
							.hasBodyThat(json())
							.hasField("error");

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenUpdate() {
		exec(() -> new Updater()

				.handle(driven())

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


	@Test void testDrivenUnauthorized() {
		exec(() -> new Updater()

				.handle(driven().roles(Form.none))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDrivenForbidden() {
		exec(() -> new Updater().roles(RDF.FIRST, RDF.REST)

				.handle(driven().shape(or()))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Forbidden)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDrivenMalformedData() {
		exec(() -> new Updater()

				.handle(driven().map(body("!!!")))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.BadRequest)
							.hasBodyThat(json())
							.hasField("error");

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDrivenInvalidData() {
		exec(() -> new Updater()

				.handle(driven().map(body("@prefix : <http://example.com/terms#>. <employees/1370>"
						+":forename 'Tino';"
						+":surname 'Faussone';"
						+":email 'tfaussone@example.com' ;"
						+":title 'Sales Rep'." // missing seniority/supervisor/subordinate
				)))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.hasBodyThat(json())
							.hasField("error");

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDrivenRestrictedData() {
		exec(() -> new Updater()

				.handle(driven()
						.roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.hasBodyThat(json())
							.hasField("error");

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

}
