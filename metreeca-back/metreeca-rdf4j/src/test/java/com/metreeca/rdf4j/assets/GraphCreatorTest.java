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

						assertThat(com.metreeca.rest.Context.asset(graph()).exec(RepositoryConnection::isEmpty))
								.as("storage unchanged")
								.isTrue();

					})
			);
		}

	}

}