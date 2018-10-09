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

import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Processor.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;


final class ProcessorTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	private Handler echo() {
		return request -> request.reply(response -> response

				.status(Response.OK)
				.shape(request.shape())

				.map(r -> request.body(rdf()).map(
						v -> r.body(rdf()).set(v),
						e -> r
				))

		);
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
		exec(() -> new Processor()

				// multiple filters to test piping

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

	@Test void testTrimRequestRDFPayloadToRequestShape() {
		exec(() -> new Processor()

				.pre((response, model) -> {

					model.add(response.item(), RDF.FIRST, RDF.NIL);
					model.add(response.item(), RDF.REST, RDF.NIL);

					return model;

				})

				.wrap(echo())

				.handle(new Request()

						.shape(trait(RDF.FIRST))
						.body(rdf()).set(emptyList())) // empty body to activate pre-processing

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.as("statements outside shape envelope trimmed")
						.isIsomorphicTo(statement(response.item(), RDF.FIRST, RDF.NIL))
				)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessResponseRDFPayload() {
		exec(() -> new Processor()

				// multiple filters to test piping

				.post(post(RDF.FIRST))
				.post(post(RDF.REST))

				.wrap(echo())

				.handle(new Request()

						.body(rdf()).set(emptyList())) // empty body to activate post-processing

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

				.accept(response -> assertThat(response).hasBody(rdf()))
		);
	}

	@Test void testTrimResponseRDFPayloadToResponseShape() {
		exec(() -> new Processor()

				.post((response, model) -> {

					model.add(response.item(), RDF.FIRST, RDF.NIL);
					model.add(response.item(), RDF.REST, RDF.NIL);

					return model;

				})

				.wrap(echo())

				.handle(new Request()

						.shape(trait(RDF.FIRST))
						.body(rdf()).set(emptyList())) // empty body to activate post-processing

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.as("statements outside shape envelope trimmed")
						.isIsomorphicTo(statement(response.item(), RDF.FIRST, RDF.NIL))
				)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExecuteScriptOnRequestFocus() {
		exec(() -> new Processor()

				.sync(sparql("insert { ?this rdf:value rdf:first } where {}"))
				.sync(sparql("insert { ?this rdf:value rdf:rest } where {}"))

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

	@Test void testExecuteScriptOnResponseLocation() {
		exec(() -> new Processor()

				.sync(sparql("insert { ?this rdf:value rdf:first } where {}"))
				.sync(sparql("insert { ?this rdf:value rdf:rest } where {}"))

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPreInsertStaticRDF() {
		exec(() -> new Processor().pre(rdf(String.format("@prefix rdf: <%s> . <> rdf:value rdf:nil .", RDF.NAMESPACE)))

				.wrap(echo())

				.handle(new Request().body(rdf()).set(emptySet()))

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.hasStatement(response.item(), RDF.VALUE, RDF.NIL)
				)
		);
	}

	@Test void testPostInsertStaticRDF() {
		exec(() -> new Processor().post(rdf(String.format("@prefix rdf: <%s> . <> rdf:value rdf:nil .", RDF.NAMESPACE)))

				.wrap(echo())

				.handle(new Request().body(rdf()).set(emptySet()))

				.accept(response -> assertThat(response)
						.hasBodyThat(rdf())
						.hasStatement(response.item(), RDF.VALUE, RDF.NIL)
				)
		);
	}

}
