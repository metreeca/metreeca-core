/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next.handlers.shape;


import com.metreeca.next.Request;
import com.metreeca.next.Request.Writer;
import com.metreeca.next.Response;
import com.metreeca.spec.Spec;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import java.util.Map;

import static com.metreeca.jeep.rdf.ValuesTest.*;
import static com.metreeca.next.LinkTest.*;
import static com.metreeca.next.handlers.shape.Browser.browser;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public class BrowserTest {

	private Writer std(final Writer writer) {
		return writer
				.method(Request.GET)
				.path("/employees/");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testBrowse() {
		testbed().handler(() -> browser(Employee))

				.dataset(small())

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(Salesman)
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					try (final RepositoryConnection connection=tool(Graph.Tool).connect()) {

						final Model expected=construct(connection,
								"construct where { ?e a :Employee; rdfs:label ?label }");

						final Model actual=response.rdf();

						assertSubset("items retrieved", expected, actual);

						assertTrue("properties restricted to manager role not included",
								actual.filter(null, term("seniority"), null).isEmpty());

					}

				});
	}

	@Test public void testBrowseFiltered() {
		testbed().handler(() -> browser(Employee))

				.dataset(small())

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(Salesman)
						.query(json("{ 'filter': { 'title': 'Sales Rep' } }"))
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					try (final RepositoryConnection connection=tool(Graph.Tool).connect()) {

						final Model expected=construct(connection,
								"construct { ?e a :Employee; :title ?t }\n"
										+"where { ?e a :Employee; :title ?t, 'Sales Rep' }");

						final Model actual=response.rdf();

						assertSubset("details retrieved", expected, actual);
					}

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testUnauthorized() {
		testbed().handler(() -> browser(Employee))

				.request(request -> std(request)
						.user(Spec.none)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Unauthorized, response.status());

				});
	}

	@Test public void testForbidden() {
		testbed().handler(() -> browser(Employee))

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(RDF.FIRST, RDF.REST)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Forbidden, response.status());

				});
	}

	@Test public void testRestricted() {
		testbed().handler(() -> browser(Employee))

				.dataset(small())

				.request(request -> std(request)
						.user(RDF.NIL)
						.roles(Salesman)
						.query(json("{ 'items': 'seniority' }"))
						.done())

				.response(response -> {

					assertEquals("success reported", Response.BadRequest, response.status()); // !!! vs Forbidden
					assertTrue("error detailed", response.json() instanceof Map);

				});
	}

}
