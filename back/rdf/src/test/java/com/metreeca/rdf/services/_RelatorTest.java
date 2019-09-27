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

package com.metreeca.rdf.services;

import com.metreeca.rdf.ModelAssert;
import com.metreeca.rdf._Form;
import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.JSONAssert;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rdf.ValuesTest.*;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rdf.services.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.services.Engine.engine;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.data.MapEntry.entry;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toMap;

import static javax.json.Json.createObjectBuilder;


final class _RelatorTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.set(engine(), GraphEngine::new)
				.set(graph(), GraphTest::graph)
				.exec(model(small()))
				.exec(tasks)
				.clear();
	}


	@SafeVarargs private final String query(final Map.Entry<String, ?>... entries) {
		return createObjectBuilder(Stream.of(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue))).build().toString();
	}

	private Map.Entry<String, String> path(final String path) {
		return entry("path", path);
	}

	private Map.Entry<String, Map<String, Object>> filter(final String path, final Object value) {
		return entry("filter", singletonMap(path, value));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Resource {

		private Request simple() {
			return new Request()
					.method(Request.GET)
					.base(Base)
					.path("/employees/1102");
		}


		@Nested final class Simple {

			@Test void testRelate() {
				exec(() -> new _Relator()

						.handle(simple())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)
								.doesNotHaveShape()

								.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
										.as("response RDF body contains a resource description")
										.hasStatement(iri(response.item()), null, null)
								)
						)
				);
			}

			@Test void testRelateFiltered() {
				exec(() -> new _Relator()

						.handle(simple().query(query(entry("offset", 100))))

						.accept(response -> assertThat(response)
								.hasStatus(Response.NotImplemented)
								.hasBody(json(), json -> JSONAssert.assertThat(json)
										.hasField("cause")
								)
						)
				);
			}


			@Test void testUnknown() {
				exec(() -> new _Relator()

						.handle(simple().path("/employees/9999"))

						.accept(response -> assertThat(response).hasStatus(Response.NotFound))
				);
			}

		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple()
						.roles(Manager)
						.shape(Employee);
			}


			@Test void testRelate() {
				exec(() -> new _Relator()

						.handle(shaped())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.as("items retrieved")
										.hasSubset(model(
												"construct where { <employees/1102> a :Employee; :code ?c; :seniority ?s }"
										))
								)
						)
				);
			}

			@Test void testRelateThrottled() {
				exec(() -> new _Relator()

						.handle(shaped().roles(Salesman))

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasBody(rdf(), rdf -> assertThat(rdf)

										.as("items retrieved")
										.hasSubset(model(
												"construct where { <employees/1102> a :Employee; :code ?c }"
										))

										.as("properties restricted to manager role not included")
										.doesNotHaveStatement(null, term("seniority"), null)
								)
						)
				);
			}

			@Test void testRelateFiltered() {
				exec(() -> new _Relator()

						.handle(shaped().query(query(
								path("subordinate"),
								filter("seniority", singletonMap(">=", 2))
						)))

						.accept(response -> assertThat(response)

								.hasStatus(Response.NotImplemented)

								.hasBody(json(), json -> JSONAssert.assertThat(json)
										.hasField("cause")
								)

								.hasStatus(Response.OK)

								.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)

										.as("items retrieved")
										.hasSubset(model(
												"construct {\n"
														+"\n"
														+"\t<employees/1102> ldp:contains ?employee.\n"
														+"\t?employee rdfs:label ?label.\n"
														+"\n"
														+"} where {\n"
														+"\n"
														+"\t<employees/1102> :subordinate ?employee.\n"
														+"\n"
														+"\t?employee rdfs:label ?label; :seniority ?seniority.\n"
														+"\n"
														+"\tfilter ( ?seniority >= 2 )\n"
														+"\n"
														+"}"
										))

								)
						)
				);
			}


			@Test void testUnauthorized() {
				exec(() -> new _Relator()

						.handle(shaped().shape(when(guard(Shape.Role, _Form.root), field(RDF.TYPE))))

						.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

				);
			}

			@Test void testForbidden() {
				exec(() -> new _Relator()

						.handle(shaped().shape(or()))

						.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

				);
			}

			@Test void testUnknown() {
				exec(() -> new _Relator()

						.handle(shaped().path("/employees/9999"))

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
					.path("/employees-basic/");
		}


		@Nested final class Simple {

			@Test void testBrowse() {
				exec(() -> new _Relator()

						.handle(simple())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.doesNotHaveShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)

										.as("labelled descriptions included")
										.hasSubset(model("construct {\n"
												+"\n"
												+"\t<employees-basic/> ldp:contains ?employee.\n"
												+"\n"
												+"\t?employee :code ?code; :office ?office.\n"
												+"\t?office rdfs:label ?label.\n"
												+"\n"
												+"} where {\n"
												+"\n"
												+"\t?employee a :Employee; :code ?code; :office ?office.\n"
												+"\t?office rdfs:label ?label.\n"
												+"\n"
												+"}"
										))

										.as("connected resources not described")
										.doesNotHaveSubset(model("construct {\n"
												+"\n"
												+"\t?office :code ?code.\n"
												+"\n"
												+"} where {\n"
												+"\n"
												+"\t?employee a :Employee; :office ?office.\n"
												+"\t?office :code ?code.\n"
												+"\n"
												+"}"
										))

								)
						)
				);
			}

			@Test void testBrowseFiltered() {
				exec(() -> new _Relator()

						.handle(simple().query(query(filter("seniority", singletonMap(">=", 2)))))

						.accept(response -> assertThat(response)
								.hasStatus(Response.UnprocessableEntity)
								.hasBody(json())
						)
				);
			}

		}

		@Nested final class Shaped {

			private Request shaped() {
				return simple()
						.shape(Employees)
						.path("/employees/");
			}


			@Test void testBrowse() {
				exec(() -> new _Relator()

						.handle(shaped())

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
										.hasStatement(iri(response.item()), LDP.CONTAINS, null)
										.hasSubset(model("construct where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"))
								)
						)
				);
			}

			@Test void testBrowseThrottled() {
				exec(() -> new _Relator()

						.handle(shaped().roles(Salesman))

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)
										.hasSubset(model("construct where { ?e a :Employee; rdfs:label ?label }"))

										.as("properties restricted to manager role not included")
										.doesNotHaveStatement(null, term("seniority"), null)
								)
						)
				);
			}

			@Test void testBrowseFiltered() {
				exec(() -> new _Relator()

						.handle(shaped()
								.roles(Salesman)
								.query(query(filter("title", "Sales Rep")))
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

								.hasShape()

								.hasBody(rdf(), rdf -> assertThat(rdf)

										.hasSubset(model(""
												+"construct { ?e a :Employee; :title ?t }\n"
												+"where { ?e a :Employee; :title ?t, 'Sales Rep' }"
										))

										.as("only resources matching filter included")
										.doesNotHaveStatement(null, term("title"), literal("President"))
								)
						)
				);
			}


			@Test void testHandlePreferMinimalContainer() {
				exec(() -> new _Relator()

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
				exec(() -> new _Relator()

						.handle(shaped().roles(_Form.none))

						.accept(response -> assertThat(response)
								.hasStatus(Response.Unauthorized)
								.doesNotHaveBody()
						)
				);
			}

			@Test void testForbidden() {
				exec(() -> new _Relator()

						.handle(shaped().user(RDF.NIL).roles(_Form.none))

						.accept(response -> assertThat(response)
								.hasStatus(Response.Forbidden)
								.doesNotHaveBody()
						)
				);
			}

			@Test void testRestricted() {
				exec(() -> new _Relator()

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
