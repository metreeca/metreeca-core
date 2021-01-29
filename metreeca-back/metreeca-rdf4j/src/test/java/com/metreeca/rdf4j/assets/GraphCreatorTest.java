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

package com.metreeca.rdf4j.assets;


import com.metreeca.json.Shape;
import com.metreeca.json.ValuesTest;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.ValueAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static org.assertj.core.api.Assertions.assertThat;

final class GraphCreatorTest {

	private static final Shape Employee=and(
			filter().then(field(RDF.TYPE, all(term("Employee")))),
			ValuesTest.Employee.redact(
					retain(Role, true),
					retain(Task, true),
					retain(Area, Detail)
			));


	@Nested final class Holder {

		private com.metreeca.rest.Request request() {
			return new com.metreeca.rest.Request()
					.base(Base)
					.path("/employees/")
					.header("Slug", "slug")
					.body(jsonld(), decode("</employees/>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :email 'tfaussone@classicmodelcars.com' ;"
							+" :title 'Sales Rep' ;"
							+" :seniority 1 ."
					)).attribute(JSONLDFormat.shape(), Employee);
		}


		@Test void testCreate() {
			exec(() -> new GraphCreator()

					.handle(request())

					.accept(response -> {

						final IRI location=response.header("Location")
								.map(path -> iri(response.request().base(), path)) // resolve root-relative location
								.orElse(null);

						assertThat(response)
								.hasStatus(com.metreeca.rest.Response.Created)
								.doesNotHaveBody();

						assertThat(location)
								.as("resource created with supplied slug")
								.isEqualTo(item("employees/slug"));

						assertThat(model())
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

				final GraphCreator creator=new GraphCreator();

				creator.handle(request()).accept(response -> {});

				final Model snapshot=model();

				creator.handle(request()).accept(response -> {

					assertThat(response)
							.hasStatus(com.metreeca.rest.Response.InternalServerError);

					assertThat(model())
							.as("graph unchanged")
							.isIsomorphicTo(snapshot);

				});

			});
		}

	}

	@Nested final class Member {

		@Test void testNotImplemented() {
			exec(() -> new GraphCreator()

					.handle(new com.metreeca.rest.Request()
							.roles(Manager)
							.base(Base)
							.path("/employees/9999").attribute(JSONLDFormat.shape(), ValuesTest.Employee)
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.InternalServerError);

						assertThat(asset(graph()).exec(RepositoryConnection::isEmpty))
								.as("storage unchanged")
								.isTrue();

					})
			);
		}

	}

}
