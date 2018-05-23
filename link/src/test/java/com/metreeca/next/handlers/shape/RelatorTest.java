/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.handlers.shape;


import com.metreeca.link.LinkTest;
import com.metreeca.link.LinkTest.Testbed;
import com.metreeca.next.Request;
import com.metreeca.next.Response;
import com.metreeca.spec.Spec;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import static com.metreeca.next.handlers.shape.Relator.relator;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.spec.things.ValuesTest.*;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;


public class RelatorTest {

	private Testbed testbed() {
		return LinkTest.testbed()

				.dataset(small())

				.handler(() -> relator(LinkTest.Employee));
	}


	private Request.Writer std(final Request.Writer writer) {
		return writer
				.method(Request.GET)
				.path("/employees/1370");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRelate() {
		testbed()

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(LinkTest.Manager)
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					try (final RepositoryConnection connection=tool(Graph.Tool).connect()) {

						final Model expected=construct(connection,
								"construct where { <employees/1370> a :Employee; :code ?c; :seniority ?s }");

						final Model actual=response.rdf();

						assertSubset("items retrieved", expected, actual);

					}

				});
	}

	@Test public void testRelateLimited() {
		testbed()

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(LinkTest.Salesman)
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					try (final RepositoryConnection connection=tool(Graph.Tool).connect()) {

						final Model expected=construct(connection,
								"construct where { <employees/1370> a :Employee; :code ?c }");

						final Model actual=response.rdf();

						assertSubset("items retrieved", expected, actual);

						assertTrue("properties restricted to manager role not included",
								actual.filter(null, term("seniority"), null).isEmpty());

					}

				});
	}

	@Test public void testRelatePiped() {
		testbed()

				.handler(() -> relator(LinkTest.Employee)

						.pipe((request, model) -> {

							model.add(statement(request.focus(), RDF.VALUE, RDF.FIRST));

							return model;

						})

						.pipe((request, model) -> {

							model.add(statement(request.focus(), RDF.VALUE, RDF.REST));

							return model;

						}))

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(LinkTest.Manager)
						.done())

				.response(response -> {

					assertSubset("items retrieved", asList(

							statement(response.focus(), RDF.VALUE, RDF.FIRST),
							statement(response.focus(), RDF.VALUE, RDF.REST)

					), response.rdf());

				});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testUnauthorized() {
		testbed()

				.request(request -> std(request)
						.user(Spec.none)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Unauthorized, response.status());

				});
	}

	@Test public void testForbidden() {
		testbed()

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(RDF.FIRST, RDF.REST)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Forbidden, response.status());

				});
	}

	@Test public void testUnknown() {
		testbed()

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(LinkTest.Salesman)
						.path("/employees/9999")
						.done())

				.response(response -> {

					assertEquals("error reported", Response.NotFound, response.status());

				});
	}

}
