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
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.form.truths.ModelAssert.assertThat;
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


	private Request container() {
		return new Request()
				.method(Request.GET)
				.base(ValuesTest.Base)
				.path("/employees/");
	}

	private Request resource() {
		return new Request()
				.method(Request.GET)
				.base(ValuesTest.Base)
				.path("/employees/1370");
	}


	@Nested final class ContainerSimple {

	}

	@Nested final class ContainerBasic {

	}

	@Nested final class ContainerDirect {

	}

	@Nested final class ContainerIndirect {

	}


	@Nested final class ResourceSimple {

		@Test void testRelate() {
			exec(() -> new Relator()

					.handle(resource())

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.doesNotHaveShape()

							.hasBody(rdf(), rdf -> assertThat(rdf)
									.as("response RDF body contains a resource description")
									.hasStatement(response.item(), null, null))));
		}

		@Test void testUnknown() {
			exec(() -> new Relator()

					.handle(resource().path("/employees/9999"))

					.accept(response -> assertThat(response).hasStatus(Response.NotFound))
			);
		}

	}

	@Nested final class ResourceShaped {

		private Request shaped() {
			return resource()
					.roles(ValuesTest.Manager)
					.shape(ValuesTest.Employee);
		}


		@Test void testRelate() {
			exec(() -> new Relator()

					.handle(shaped())

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

					.handle(shaped().roles(ValuesTest.Salesman))

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


		@Test void testForbidden() {
			exec(() -> new Relator()

					.handle(shaped().shape(or()))

					.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

			);
		}

		@Test void testUnauthorized() {
			exec(() -> new Relator()

					.handle(shaped().roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

			);
		}

		@Test void testUnknown() {
			exec(() -> new Relator()

					.handle(shaped().path("/employees/9999"))

					.accept(response -> assertThat(response).hasStatus(Response.NotFound))

			);
		}

	}

}
