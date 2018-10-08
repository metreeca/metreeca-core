/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;

import static javax.json.Json.createObjectBuilder;


final class BrowserTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(small()))
				.exec(task)
				.clear();
	}


	private Request direct() {
		return new Request()
				.roles(Manager)
				.method(Request.GET)
				.base(Base)
				.path("/employees/");
	}

	private Request driven() {
		return direct()
				.body(shape()).set(Employee);
	}


	//// Direct ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectBrowse() {
		exec(() -> new Browser()

				.handle(direct())

				.accept(response -> assertThat(response)
						.hasStatus(Response.NotImplemented)
						.hasBody(json())
				)
		);
	}



	//// Driven ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenBrowse() {
		exec(() -> new Browser()

				.handle(driven())

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK)

						.hasBody(shape())

						.hasBodyThat(rdf())
						.hasStatement(response.item(), LDP.CONTAINS, null)
						.hasSubset(graph("construct where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"))
				)
		);
	}

	@Test void testDrivenBrowseLimited() {
		exec(() -> new Browser()

				.handle(driven().roles(Salesman))

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK)

						.hasBody(shape())

						.hasBodyThat(rdf())
						.hasSubset(graph("construct where { ?e a :Employee; rdfs:label ?label }"))

						.as("properties restricted to manager role not included")
						.doesNotHaveStatement(null, term("seniority"), null)
				)
		);
	}

	@Test void testDrivenBrowseFiltered() {
		exec(() -> new Browser()

				.handle(driven()
						.roles(Salesman)
						.query(createObjectBuilder()
								.add("filter", createObjectBuilder().add("title", "Sales Rep"))
								.build().toString()))

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK)

						.hasBody(shape())

						.hasBodyThat(rdf())
						.hasSubset(graph(""
								+"construct { ?e a :Employee; :title ?t }\n"
								+"where { ?e a :Employee; :title ?t, 'Sales Rep' }"
						))

						.as("only resources matching filter included")
						.doesNotHaveStatement(null, term("title"), literal("President"))
				)
		);
	}


	@Test void testDrivePreferEmptyContainer() {
		exec(() -> new Browser()

				.handle(driven().header("Prefer", String.format(
						"return=representation; include=\"%s\"", LDP.PREFER_EMPTY_CONTAINER
				)))

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK)
						.hasHeader("Preference-Applied", response.request().header("Prefer").orElse(""))

						.hasBody(shape())

						.hasBodyThat(rdf())
						.doesNotHaveStatement(null, LDP.CONTAINS, null)
				)
		);
	}


	@Test void testDrivenUnauthorized() {
		exec(() -> new Browser()

				.handle(driven().roles(Form.none))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				)
		);
	}

	@Test void testDrivenForbidden() {
		exec(() -> new Browser()

				.handle(driven().user(RDF.NIL).roles(Form.none))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Forbidden)
						.doesNotHaveBody()
				)
		);
	}

	@Test void testDrivenRestricted() {
		exec(() -> new Browser()

				.handle(driven()
						.user(RDF.NIL)
						.roles(Salesman)
						.query(createObjectBuilder()
								.add("items", "seniority")
								.build().toString())
				)

				.accept(response -> assertThat(response)
						.hasStatus(Response.UnprocessableEntity)
						.hasBodyThat(json())
						.hasField("error")
				)
		);
	}

}
