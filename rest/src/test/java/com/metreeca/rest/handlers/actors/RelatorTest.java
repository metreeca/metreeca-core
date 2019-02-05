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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


@Disabled final class RelatorTest {

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
					.base(ValuesTest.Base)
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
						.roles(ValuesTest.Manager)
						.shape(ValuesTest.Employee);
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

						.handle(resource().roles(ValuesTest.Salesman))

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

}
