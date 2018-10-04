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

package com.metreeca.rest.handlers.shape;

import com.metreeca.form.Form;
import com.metreeca.form.things.Values;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ModelAssert.assertThat;
import static com.metreeca.form.things.ValueAssert.assertThat;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;
import static com.metreeca.tray.Tray.tool;


final class _CreatorTest {

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}

	private Request direct() {
		return new Request()
				.roles(Manager)
				.method(Request.POST)
				.base(Base)
				.path("/employees/")
				.body(rdf()).set(decode("<>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone' ;"
						+" :email 'tfaussone@classicmodelcars.com' ;"
						+" :title 'Sales Rep' ;"
						+" :seniority 1 ."
				));
	}

	private Request driven() {
		return direct()
				.body(shape()).set(Employee);
	}


	private Model graph() {
		return tool(Graph.Factory).query(connection -> { return export(connection); });
	}


	//// Driven ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenCreate() {
		exec(() -> new _Creator()

				.handle(driven())

				.accept(response -> {

					final IRI location=response.header("Location")
							.map(Values::iri)
							.orElse(null);

					assertThat(response)
							.hasStatus(Response.Created)
							.doesNotHaveBody();

					assertThat(location)
							.as("resource created with IRI stemmed on request focus")
							.hasNamespace(response.request().item().stringValue());

					assertThat(graph()).hasSubset(
							statement(location, RDF.TYPE, term("Employee")),
							statement(location, term("forename"), literal("Tino")),
							statement(location, term("surname"), literal("Faussone"))
					);

				}));
	}

	@Test void testDrivenCreateSlug() {
		exec(() -> new _Creator()

				.slug((request, model) -> "slug")

				.handle(driven())

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


	@Test void testDrivenUnauthorized() {
		exec(() -> new _Creator()

				.handle(driven().roles(Form.none))

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

	@Test void testDrivenForbidden() {
		exec(() -> new _Creator()

				.handle(driven().body(shape()).set(or()))

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

	@Test void testDrivenMalformedData() {
		exec(() -> new _Creator()

				.handle(driven().body(shape()).set(or()))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.BadRequest)
							.doesNotHaveHeader("Location")
							.doesNotHaveBody();

					// !!! assertTrue("error detailed", response.json() instanceof Map);


					assertThat(graph())
							.as("graph unchanged")
							.isEmpty();

				}));
	}

	@Test void testDrivenInvalidData() {
		exec(() -> new _Creator()

				.handle(driven().body(rdf()).set(decode("<>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone'. "
				)))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.doesNotHaveHeader("Location")
							.doesNotHaveBody();

					// !!! assertTrue("error detailed", response.json() instanceof Map);

					assertThat(graph())
							.as("graph unchanged")
							.isEmpty();

				}));
	}

	@Test void testDrivenRestrictedData() {
		exec(() -> new _Creator()

				.handle(driven()
						.roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.BadRequest)  // !!! vs Forbidden / UnprocessableEntity
							.doesNotHaveHeader("Location")
							.doesNotHaveBody();

					// !!! assertTrue("error detailed", response.json() instanceof Map);

					assertThat(graph())
							.as("graph unchanged")
							.isEmpty();

				}));
	}

}
