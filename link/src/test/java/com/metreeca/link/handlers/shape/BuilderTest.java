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

package com.metreeca.link.handlers.shape;


import com.metreeca.link.LinkTest;
import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.spec.Spec;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.link.handlers.shape.Builder.builder;
import static com.metreeca.spec.things.ValuesTest.assertIsomorphic;
import static com.metreeca.spec.things.ValuesTest.parse;
import static com.metreeca.spec.things.ValuesTest.sparql;

import static org.junit.Assert.assertEquals;

import static java.util.Collections.emptySet;


public class BuilderTest {

	private Request.Writer relate(final Request.Writer writer) {
		return writer
				.method(Request.GET)
				.path("/virtual");
	}


	public Builder handler() {
		return builder(LinkTest.Employee, sparql("construct {\n"
				+"\n"
				+"\t$this a :Employee ;\n"
				+"\t\trdfs:label 'Tino Faussone' ;\n"
				+"\t\t:code '1234' ;\n"
				+"\t\t:surname 'Faussone' ;\n"
				+"\t\t:forename 'Tino' ;\n"
				+"\t\t:email 'tfaussone@classicmodelcars.com' ;\n"
				+"\t\t:title 'Sales Rep' ;\n"
				+"\t\t:supervisor <employees/1102> ;\n"
				+"\t\t:seniority '1'^^xsd:integer .\n"
				+"\n"
				+"} where {}"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testBuild() {
		testbed().handler(this::handler)

				.request(request -> relate(request)
						.user(RDF.NIL)
						.roles(LinkTest.Manager)
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					final Model expected=parse("<virtual> a :Employee ;\n"
							+"\trdfs:label 'Tino Faussone' ;\n"
							+"\t:code '1234' ;\n"
							+"\t:surname 'Faussone' ;\n"
							+"\t:forename 'Tino' ;\n"
							+"\t:email 'tfaussone@classicmodelcars.com' ;\n"
							+"\t:title 'Sales Rep' ;\n"
							+"\t:supervisor <employees/1102> ;\n"
							+"\t:seniority '1'^^xsd:integer .\n");

					final Model actual=response.rdf();

					assertIsomorphic("items generated", expected, actual);

				});
	}

	@Test public void testBuildLimited() {
		testbed().handler(this::handler)

				.request(request -> relate(request)
						.user(RDF.NIL)
						.roles(LinkTest.Salesman)
						.done())

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());

					final Model expected=parse("<virtual> a :Employee ;\n"
							+"\trdfs:label 'Tino Faussone' ;\n"
							+"\t:code '1234' ;\n"
							+"\t:surname 'Faussone' ;\n"
							+"\t:forename 'Tino' ;\n"
							+"\t:email 'tfaussone@classicmodelcars.com' ;\n"
							+"\t:title 'Sales Rep' .\n");

					final Model actual=response.rdf();

					assertIsomorphic("items generated", expected, actual);

				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testNotFoundOnEmptyModel() {
		testbed().handler(() -> builder(LinkTest.Employee, request -> emptySet()))

				.request(request -> relate(request)
						.user(RDF.NIL)
						.roles(LinkTest.Manager)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.NotFound, response.status());

				});
	}

	@Test public void testUnauthorized() {
		testbed().handler(this::handler)

				.request(request -> relate(request)
						.user(Spec.none)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Unauthorized, response.status());

				});
	}

	@Test public void testForbidden() {
		testbed().handler(this::handler)

				.request(request -> relate(request)
						.user(RDF.NIL)
						.roles(RDF.FIRST, RDF.REST)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.Forbidden, response.status());

				});
	}

}
