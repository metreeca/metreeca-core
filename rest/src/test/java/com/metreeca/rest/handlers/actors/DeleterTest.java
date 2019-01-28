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


import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Form.none;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;


final class DeleterTest {

	private static final Model Dataset=small();


	private void exec(final Runnable task) {
		new Tray().exec(graph(small()), task).clear();
	}


	private Request direct() {
		return new Request()
				.roles(Manager)
				.method(Request.DELETE)
				.base(Base)
				.path("/employees/1370");
	}

	private Request driven() {
		return direct()
				.shape(Employee);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectDelete() {
		exec(() -> new Deleter()

				.handle(direct())

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.NoContent)
							.doesNotHaveBody();

					assertThat(graph("construct where { <employees/1370> ?p ?o }"))
							.as("cell deleted")
							.isEmpty();

					assertThat(graph("construct where { ?s ?p <employees/1370> }"))
							.as("inbound links removed")
							.isEmpty();

					assertThat(graph("construct where { <employees/1102> rdfs:label ?o }"))
							.as("connected resources preserved")
							.isNotEmpty();

				}));
	}


	@Test void testDirectUnauthorized() {
		exec(() -> new Deleter().role(Manager)

				.handle(direct().user(none).roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDirectForbidden() {
		exec(() -> new Deleter().role(Manager)

				.handle(direct().user(RDF.NIL).roles(Salesman))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Forbidden)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenDelete() {
		exec(() -> new Deleter()

				.handle(driven())

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.NoContent)
							.doesNotHaveBody();

					assertThat(graph("construct where { <employees/1370> ?p ?o }"))
							.isEmpty();

				}));
	}


	@Test void testDrivenUnauthorized() {
		exec(() -> new Deleter()

				.handle(driven().roles(none))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

	@Test void testDrivenForbidden() {
		exec(() -> new Deleter()

				.handle(driven().shape(or()))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Forbidden)
							.doesNotHaveBody();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(Dataset);

				}));
	}

}
