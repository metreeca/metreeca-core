/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.services;

import com.metreeca.json.*;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Shape.required;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rdf4j.services.GraphFactsTest.EmployeeShape;
import static com.metreeca.rdf4j.services.GraphTest.exec;
import static com.metreeca.rdf4j.services.GraphTest.model;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

final class GraphEngineTest {

	@Nested final class Create {

		private final Shape Employee=and(
				filter(field(RDF.TYPE, all(term("Employee")))),
				field(RDFS.LABEL, required(), datatype(XSD.STRING)),
				field(term("code"), required(), datatype(XSD.STRING), pattern("\\d+")),
				field(term("forename"), required(), datatype(XSD.STRING), maxLength(80)),
				field(term("surname"), required(), datatype(XSD.STRING), maxLength(80)),
				field(term("email"), required(), datatype(XSD.STRING), maxLength(80)),
				field(term("title"), required(), datatype(XSD.STRING), maxLength(80))

		);

		private Request request() {
			return new Request()
					.base(Base)
					.path("/employees/slug")
					.body(jsonld(), decode("</employees/slug>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :email 'tfaussone@classicmodelcars.com' ;"
							+" :title 'Sales Rep' ;"
							+" :seniority 1 ."
					)).attribute(shape(), Employee);
		}


		@Test void testCreate() {
			exec(() -> new GraphEngine()

					.create(request())

					.accept(response -> {

						final IRI location=response.header("Location")
								.map(path -> iri(response.request().base(), path)) // resolve root-relative location
								.orElse(null);

						assertThat(response)
								.hasStatus(com.metreeca.rest.Response.Created)
								.doesNotHaveBody();

						ValueAssert.assertThat(location)
								.as("resource created with supplied slug")
								.isEqualTo(item("employees/slug"));

						ModelAssert.assertThat(model())
								.as("resource description stored into the graph")
								.hasSubset(
										statement(location, RDF.TYPE, term("Employee")),
										statement(location, term("forename"), literal("Tino")),
										statement(location, term("surname"), literal("Faussone"))
								);

					}));
		}

		@Test void testConflictingSlug() {
			exec(() -> {

				new GraphEngine().create(request()).accept(response -> {});

				final Model snapshot=model();

				new GraphEngine().create(request()).accept(response -> {

					assertThat(response)
							.hasStatus(com.metreeca.rest.Response.InternalServerError);

					ModelAssert.assertThat(model())
							.as("graph unchanged")
							.isIsomorphicTo(snapshot);

				});

			});
		}

	}

	@Nested final class Relate {

		private Request request() {
			return new Request()
					.base(Base)
					.path("/employees/1370")
					.attribute(shape(), EmployeeShape);
		}


		@Test void testRelate() {
			exec(model(small()), () -> new GraphEngine()

					.relate(request())

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK).hasAttribute(shape(),
									shape -> Assertions.assertThat(shape).isNotEqualTo(and()))

							.hasBody(jsonld(), rdf -> ModelAssert.assertThat(rdf)
									.as("items retrieved")
									.isSubsetOf(model(
											"construct where { <employees/1370> ?p ?o }"
									))
							)
					)
			);
		}

	}

	@Nested final class Browse {

		private Request request() {
			return new Request()
					.base(Base)
					.path("/employees/")
					.attribute(shape(), EmployeeShape);
		}


		@Test void testBrowse() {
			exec(model(small()), () -> new GraphEngine()

					.browse(request())

					.accept(response -> assertThat(response)

							.hasStatus(OK)
							.hasAttribute(shape(), shape -> Assertions.assertThat(shape).isNotEqualTo(and()))

							.hasBody(jsonld(), rdf -> ModelAssert.assertThat(rdf)
									.hasStatement(iri(response.item()), Shape.Contains, null)
									.hasSubset(model("construct { ?e rdfs:label ?label; :seniority ?seniority }\n"
											+"where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"
									))
							)
					)
			);
		}

		@Test void testBrowseFiltered() {
			exec(model(small()), () -> new GraphEngine()

					.browse(request()
							.query("title=Sales+Rep")
					)

					.accept(response -> assertThat(response)

							.hasStatus(OK)
							.hasAttribute(shape(), shape -> Assertions.assertThat(shape).isNotEqualTo(and()))

							.hasBody(jsonld(), rdf -> ModelAssert.assertThat(rdf)

									.hasSubset(model(""
											+"construct { ?e :title ?t; :seniority ?seniority }\n"
											+"where { ?e a :Employee; :title ?t, 'Sales Rep'; :seniority ?seniority }"
									))

									.as("only resources matching filter included")
									.doesNotHaveStatement(null, Values.term("title"), literal("President"))
							)
					)
			);
		}

		@Test void testSliceTermsQueries() {
			exec(model(small()), () -> new GraphEngine()

					.browse(request()
							.query(".terms=office&.offset=1&.limit=3")
					)

					.accept(response -> assertThat(response)

							.hasStatus(OK)
							.hasAttribute(shape(), shape -> Assertions.assertThat(shape).isNotEqualTo(and()))

							.hasBody(jsonld(), rdf -> ModelAssert.assertThat(rdf)

									.isIsomorphicTo(model(""
											+"construct { \n"
											+"\n"
											+"\t<employees/> :terms [:value ?o; :count ?c]. \n"
											+"\t?o rdfs:label ?l\n"
											+"\n"
											+"} where { { select ?o ?l (count(?e) as ?c) {\n"
											+"\n"
											+"\t?e a :Employee; :office ?o.\n"
											+"\t?o rdfs:label ?l.\n"
											+"\n"
											+"} group by ?o ?l order by desc(?c) offset 1 limit 3 } }"
									))

							)
					)
			);
		}


	}

	@Nested final class Update {

		@Test void testUpdate() {
			exec(model(small()), () -> new GraphEngine()

					.update(new Request()
							.base(Base)
							.path("/employees/1370")
							.attribute(shape(), and(
									field(term("forename"), required()),
									field(term("surname"), required()),
									field(term("email"), required()),
									field(term("title"), required()),
									field(term("seniority"), required())
							))
							.body(jsonld(), decode("</employees/1370>"
									+":forename 'Tino';"
									+":surname 'Faussone';"
									+":email 'tfaussone@example.com';"
									+":title 'Sales Rep' ;"
									+":seniority 5 ." // outside salesman envelope
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(NoContent)
								.doesNotHaveBody();

						ModelAssert.assertThat(model())

								.as("updated values inserted")
								.hasSubset(decode("</employees/1370>"
										+":forename 'Tino';"
										+":surname 'Faussone';"
										+":email 'tfaussone@example.com';"
										+":title 'Sales Rep' ;"
										+":seniority 5 ."
								))

								.as("previous values removed")
								.doesNotHaveSubset(decode("</employees/1370>"
										+":forename 'Gerard';"
										+":surname 'Hernandez'."
								));

					}));
		}

		@Test void testReportMissing() {
			exec(() -> new GraphEngine()

					.update(new Request()
							.base(Base)
							.path("/employees/9999")
							.body(jsonld(), decode(""))
					)

					.accept(response -> assertThat(response)
							.hasStatus(NotFound)
							.doesNotHaveBody()
					)
			);

		}

	}

	@Nested final class Delete {

		@Test void testDelete() {
			exec(model(small()), () -> new GraphEngine()

					.delete(new Request()
							.base(Base)
							.path("/employees/1370")
							.attribute(shape(), and(

									field(RDF.TYPE),
									field(RDFS.LABEL),

									field(term("forename")),
									field(term("surname")),
									field(term("email")),
									field(term("title")),
									field(term("code")),
									field(term("office")),
									field(term("seniority")),
									field(term("supervisor")),
									field(term("subordinate"))
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(NoContent)
								.doesNotHaveBody();

						ModelAssert.assertThat(model("construct where { <employees/1370> ?p ?o }"))
								.isEmpty();

						ModelAssert.assertThat(model("construct where { ?s a :Employee; ?p ?o. }"))
								.isNotEmpty();

					}));
		}

		@Test void testReportUnknown() {
			exec(() -> new GraphEngine()

					.delete(new Request()
							.path("/unknown")
					)

					.accept(response -> assertThat(response)
							.hasStatus(NotFound)
							.doesNotHaveBody()
					)
			);
		}

	}

}