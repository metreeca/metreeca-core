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
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rdf.Values.none;
import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rdf.services.GraphTest.model;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.services.Engine.engine;


final class _DeleterTest {

	private static final Model Dataset=ValuesTest.small();


	private void exec(final Runnable... tasks) {
		new Context()
				.set(engine(), GraphEngine::new)
				.set(graph(), GraphTest::graph)
				.exec(GraphTest.model(ValuesTest.small()))
				.exec(tasks)
				.clear();
	}


	@Nested final class Holder {

		private Request request() {
			return new Request()
					.base(ValuesTest.Base)
					.path("/employees/")
					.shape(ValuesTest.Employees);
		}


		@Test void testNotImplemented() {
			exec(() -> new _Deleter()

					.handle(request())

					.accept(response -> ResponseAssert.assertThat(response)
							.hasStatus(Response.NotImplemented)
							.hasBody(json())
					)
			);
		}

	}

	@Nested final class Member {

		private Request shaped() {
			return new Request()
					.roles(ValuesTest.Manager)
					.method(Request.DELETE)
					.base(ValuesTest.Base)
					.path("/employees/1370").shape(ValuesTest.Employee);
		}


		@Test void testDelete() {
			exec(() -> new _Deleter()

					.handle(shaped())

					.accept(response -> {

						ResponseAssert.assertThat(response)
								.hasStatus(Response.NoContent)
								.doesNotHaveBody();

						ModelAssert.assertThat(model("construct where { <employees/1370> ?p ?o }"))
								.isEmpty();

					}));
		}


		@Test void testUnauthorized() {
			exec(() -> new _Deleter()

					.handle(shaped().roles(none))

					.accept(response -> {

						ResponseAssert.assertThat(response)
								.hasStatus(Response.Unauthorized)
								.doesNotHaveBody();

						ModelAssert.assertThat(model())
								.as("graph unchanged")
								.isIsomorphicTo(Dataset);

					}));
		}

		@Test void testForbidden() {
			exec(() -> new _Deleter()

					.handle(shaped().user(RDF.NIL).roles(none))

					.accept(response -> {

						ResponseAssert.assertThat(response)
								.hasStatus(Response.Forbidden)
								.doesNotHaveBody();

						ModelAssert.assertThat(model())
								.as("graph unchanged")
								.isIsomorphicTo(Dataset);

					}));
		}

		@Test void testUnknown() {
			exec(() -> new _Deleter()

					.handle(shaped().path("/unknown"))

					.accept(response -> {

						ResponseAssert.assertThat(response)
								.hasStatus(Response.NotFound)
								.doesNotHaveBody();

						ModelAssert.assertThat(model())
								.as("graph unchanged")
								.isIsomorphicTo(Dataset);

					}));
		}

	}

}
