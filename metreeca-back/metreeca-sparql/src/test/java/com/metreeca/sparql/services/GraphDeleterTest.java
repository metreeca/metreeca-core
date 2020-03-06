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

package com.metreeca.sparql.services;


import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.ValuesTest.small;
import static com.metreeca.rdf.ValuesTest.term;
import static com.metreeca.sparql.services.GraphTest.exec;
import static com.metreeca.sparql.services.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.tree.Shape.convey;
import static com.metreeca.tree.Shape.filter;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;


final class GraphDeleterTest {

	@Nested final class Holder {

		@Test void testNotImplemented() {
			exec(() -> new GraphDeleter()

					.handle(new Request()
							.path("/employees/"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
							.hasBody(json())
					)
			);
		}

	}

	@Nested final class Member {

		@Test void testDelete() {
			exec(model(small()), () -> new GraphDeleter()

					.handle(new Request()
							.base(ValuesTest.Base)
							.path("/employees/1370")
							.shape(and(
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
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.NoContent)
								.doesNotHaveBody();

						assertThat(model("construct where { <employees/1370> ?p ?o }"))
								.isEmpty();

					}));
		}

		@Test void testRejectUnknown() {
			exec(() -> new GraphDeleter()

					.handle(new Request()
							.path("/unknown")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
							.doesNotHaveBody()
					)
			);
		}

	}

}
