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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.Base;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.tray.rdf.GraphTest.graph;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.rest.wrappers.Connector.update;

import static java.util.Arrays.asList;


final class PostprocessorTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	private BiFunction<Response, Model, Model> post(final Value value) {
		return (response, model) -> {

			model.add(statement(response.item(), RDF.VALUE, value));

			return model;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessResponseRDFPayload() {
		exec(() -> echo()

				.with(new Postprocessor(post(RDF.FIRST), post(RDF.REST))) // multiple filters to test piping

				.handle(new Request().body(rdf(), set()))

				.accept(response -> assertThat(response)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.as("filters executed")
								.hasSubset(asList(
										statement(response.item(), RDF.VALUE, RDF.FIRST),
										statement(response.item(), RDF.VALUE, RDF.REST)
								))
						)
				)
		);
	}

	@Test void testIgnoreUnsuccessfulResponses() {
		exec(() -> echo(response -> response.status(Response.NotFound))

				.with(new Postprocessor(post(RDF.NIL)))

				.handle(new Request())

				.accept(response -> assertThat(response)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.as("filters not executed")
								.isEmpty()
						)
				)
		);

	}


	@Test void testSupportSPARQLUpdateScripts() {
		exec(() -> echo()

				.with(new Postprocessor(
						update("insert { ?this rdf:value rdf:first } where {}"),
						update("insert { ?this rdf:value rdf:rest } where {}")
				))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/test")
						.body(rdf(), set())
				)

				.accept(response -> {

					assertThat(response)
							.as("retrieve body to activate postprocessing")
							.hasBody(rdf());

					assertThat(graph())
							.as("repository updated")
							.isIsomorphicTo(decode("<test> rdf:value rdf:first, rdf:rest."));

				})

		);
	}

}
