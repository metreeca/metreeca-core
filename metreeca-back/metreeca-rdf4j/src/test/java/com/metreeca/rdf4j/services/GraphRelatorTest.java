/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.services;

import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rdf.ValuesTest.small;
import static com.metreeca.rdf.ValuesTest.term;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.services.GraphTest.exec;
import static com.metreeca.rdf4j.services.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.tree.Shape.convey;
import static com.metreeca.tree.Shape.filter;
import static com.metreeca.tree.Shape.member;
import static com.metreeca.tree.shapes.Field.field;


final class GraphRelatorTest {

	private static final Shape EmployeeShape=member().then(
			filter().then(
					field(RDF.TYPE, term("Employee"))
			),
			convey().then(
					field(RDFS.LABEL),
					field(term("forename")),
					field(term("surname")),
					field(term("email")),
					field(term("title")),
					field(term("code")),
					field(term("office")),
					field(term("seniority")),
					field(term("supervisor"))
			)
	);


	@Nested final class Holder {

		private Request request() {
			return new Request()
					.base(ValuesTest.Base)
					.path("/employees/")
					.shape(EmployeeShape);
		}


		@Test void testBrowse() {
			exec(model(small()), () -> new GraphRelator()

					.handle(request())

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)

							.hasShape()

							.hasBody(rdf(), rdf -> assertThat(rdf)
									.hasStatement(iri(response.item()), LDP.CONTAINS, null)
									.hasSubset(model("construct { ?e rdfs:label ?label; :seniority ?seniority }\n"
											+ "where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"))
							)
					)
			);
		}

		@Test void testBrowseFiltered() {
			exec(model(small()), () -> new GraphRelator()

					.handle(request()
							.query("title=Sales+Rep")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)

							.hasShape()

							.hasBody(rdf(), rdf -> assertThat(rdf)

									.hasSubset(model(""
											+"construct { ?e :title ?t; :seniority ?seniority }\n"
											+"where { ?e a :Employee; :title ?t, 'Sales Rep'; :seniority ?seniority }"
									))

									.as("only resources matching filter included")
									.doesNotHaveStatement(null, ValuesTest.term("title"), literal("President"))
							)
					)
			);
		}


		@Test void testHandlePreferMinimalContainer() {
			exec(model(small()), () -> new GraphRelator()

					.handle(request().header("Prefer", String.format(
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

	}

	@Nested final class Member {

		private Request request() {
			return new Request()
					.base(ValuesTest.Base)
					.path("/employees/1370")
					.shape(EmployeeShape);
		}


		@Test void testRelate() {
			exec(model(small()), () -> new GraphRelator()

					.handle(request())

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)

							.hasShape()

							.hasBody(rdf(), rdf -> assertThat(rdf)
									.as("items retrieved")
									.isSubsetOf(model(
											"construct where { <employees/1370> ?p ?o }"
									))
							)
					)
			);
		}

		@Test void testRelateFiltered() {
			exec(model(small()), () -> new GraphRelator()

					.handle(request()
							.query(">= subordinate/seniority=2")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.NotImplemented)

							.doesNotHaveShape()
							.hasBody(json())

					)
			);
		}

	}

}
