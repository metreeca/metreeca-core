/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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
import com.metreeca.form.things.ValuesTest;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


final class RelatorTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(small()))
				.exec(task)
				.clear();
	}


	private Request direct() {
		return new Request()
				.method(Request.GET)
				.base(ValuesTest.Base)
				.path("/employees/1370");
	}

	private Request driven() {
		return direct()
				.roles(ValuesTest.Manager)
				.shape(ValuesTest.Employee);
	}


	//// Direct ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectRelate() {
		exec(() -> new Relator()

				.handle(direct())

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK)
						.doesNotHaveShape()

						.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
								.as("response RDF body contains a resource description")
								.hasStatement(response.item(), null, null))));
	}

	@Test void testDirectUnknown() {
		exec(() -> new Relator()

				.handle(direct().path("/employees/9999"))

				.accept(response -> assertThat(response).hasStatus(Response.NotFound))
		);
	}


	//// Driven ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenRelate() {
		exec(() -> new Relator()

				.handle(driven())

				.accept(response -> tool(Graph.Factory).query(connection -> {

					assertThat(response)

							.hasStatus(Response.OK)

							.hasShape()

							.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
									.as("items retrieved")
									.hasSubset(construct(connection,
											"construct where { <employees/1370> a :Employee; :code ?c; :seniority ?s }"
									)));

				}))
		);
	}

	@Test void testDrivenRelateLimited() {
		exec(() -> new Relator()

				.handle(driven().roles(ValuesTest.Salesman))

				.accept(response -> tool(Graph.Factory).query(connection -> {

					assertThat(response)

							.hasStatus(Response.OK)

							.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)

									.as("items retrieved")
									.hasSubset(construct(connection,
											"construct where { <employees/1370> a :Employee; :code ?c }"
									))

									.as("properties restricted to manager role not included")
									.doesNotHaveStatement(null, term("seniority"), null));

				}))
		);
	}


	@Test void testDrivenForbidden() {
		exec(() -> new Relator()

				.handle(driven().shape(or()))

				.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

		);
	}

	@Test void testDrivenUnauthorized() {
		exec(() -> new Relator()

				.handle(driven().roles(Form.none))

				.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

		);
	}

	@Test void testDrivenUnknown() {
		exec(() -> new Relator()

				.handle(driven().path("/employees/9999"))

				.accept(response -> assertThat(response).hasStatus(Response.NotFound))

		);
	}

}
