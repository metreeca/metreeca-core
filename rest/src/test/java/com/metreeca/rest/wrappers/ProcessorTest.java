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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


final class ProcessorTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	private Handler echo() {
		return request -> request.reply(response -> request.body(rdf()).map(
				v -> response.body(rdf()).set(v),
				e -> response
		).status(Response.OK));
	}


	private BiFunction<Request, Model, Model> pre(final Value value) {
		return (request, model) -> {

			model.add(statement(request.item(), RDF.VALUE, value));

			return model;

		};
	}

	private BiFunction<Response, Model, Model> post(final Value value) {
		return (response, model) -> {

			model.add(statement(response.item(), RDF.VALUE, value));

			return model;

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessRequestRDFPayload() {
		exec(() -> new Processor() // multiple filters to test piping

				.pre(pre(RDF.FIRST))
				.pre(pre(RDF.REST))

				.wrap(echo())

				.handle(new Request()

						.body(rdf()).set(emptyList())) // empty body to activate pre-processing

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.as("items retrieved")
						.hasSubset(asList(
								statement(response.item(), RDF.VALUE, RDF.FIRST),
								statement(response.item(), RDF.VALUE, RDF.REST)
						))));
	}

	@Test void testIgnoreMissingRequestRDFPayload() {
		exec(() -> new Processor()

				.pre(pre(RDF.FIRST))

				.wrap(echo())

				.handle(new Request()) // no RDF payload

				.accept(response -> assertThat(response).hasBody(rdf())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessResponseRDFPayload() {
		exec(() -> new Processor() // multiple filters to test piping

				.post(post(RDF.FIRST))
				.post(post(RDF.REST))

				.wrap(echo())

				.handle(new Request()

						.body(rdf()).set(emptyList()))

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.as("items retrieved")
						.hasSubset(asList(
								statement(response.item(), RDF.VALUE, RDF.FIRST),
								statement(response.item(), RDF.VALUE, RDF.REST)
						))));
	}

	@Test void testIgnoreMissingResponseRDFPayload() {
		exec(() -> new Processor()

				.post(post(RDF.FIRST))

				.wrap(echo())

				.handle(new Request()) // no RDF payload

				.accept(response -> assertThat(response).hasBody(rdf())));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExecuteUpdateScriptOnRequestFocus() {
		exec(graph(decode("<test> rdf:value rdf:first.")), () -> new Processor()

				.update(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
				.wrap((Handler)request -> request.reply(response -> response.status(Response.OK)))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/test"))

				.accept(response -> assertThat(graph())
						.as("repository updated")
						.isIsomorphicTo(decode("<test> rdf:value rdf:first, rdf:rest."))
				));
	}

	@Test void testExecuteUpdateScriptOnResponseLocation() {
		exec(graph(decode("<test> rdf:value rdf:first.")), () -> new Processor()

				.update(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
				.wrap((Handler)request -> request.reply(response -> response
						.status(Response.OK)
						.header("Location", Base+"test")))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/"))

				.accept(response -> tool(Graph.Factory).query(connection -> {

					assertThat(decode("<test> rdf:value rdf:first, rdf:rest."))
							.as("repository updated")
							.isIsomorphicTo(export(connection));

				})));
	}

}
