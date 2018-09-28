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

import com.metreeca.rest.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.BiFunction;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.fail;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


final class ProcessorTest {

	private Handler echo() {
		return request -> request.reply(response -> {
			return ((Result<Collection<Statement>>)request.body(rdf())).map(v -> response.body(rdf()).set(v), e -> response).status(Response.OK);
				}
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
		new Tray()

				.get(() -> new Processor() // multiple filters to test piping

						.pre(pre(RDF.FIRST))
						.pre(pre(RDF.REST)))

				.wrap(echo())

				.handle(new Request()

						.body(rdf()).set(emptyList()))

				.accept(response -> {
					response.body(rdf()).use(
							model -> assertSubset("items retrieved", asList(
									statement(response.item(), RDF.VALUE, RDF.FIRST),
									statement(response.item(), RDF.VALUE, RDF.REST)
							), model),
							error -> fail("missing RDF payload")
					);

				});
	}

	@Test void testIgnoreMissingRequestRDFPayload() {
		new Tray()

				.get(() -> new Processor()

						.pre(pre(RDF.FIRST)))

				.wrap(echo())

				.handle(new Request()) // no RDF payload

				.accept(response -> {
					response.body(rdf()).use(
							model -> fail("unexpected RDF payload"),
							error -> {}
					);
				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessResponseRDFPayload() {
		new Tray()

				.get(() -> new Processor() // multiple filters to test piping

						.post(post(RDF.FIRST))
						.post(post(RDF.REST)))

				.wrap(echo())

				.handle(new Request()

						.body(rdf()).set(emptyList()))

				.accept(response -> {
					response.body(rdf()).use(
							model -> assertSubset("items retrieved", asList(
									statement(response.item(), RDF.VALUE, RDF.FIRST),
									statement(response.item(), RDF.VALUE, RDF.REST)
							), model),
							error -> fail("missing RDF payload")
					);

				});
	}

	@Test void testIgnoreMissingResponseRDFPayload() {
		new Tray()

				.get(() -> new Processor()

						.post(post(RDF.FIRST)))

				.wrap(echo())

				.handle(new Request()) // no RDF payload

				.accept(response -> {
					response.body(rdf()).use(
							model -> fail("unexpected RDF payload"),
							error -> {}
					);
				});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExecuteUpdateScriptOnRequestFocus() {

		final Tray tray=new Tray();
		final Graph graph=tray.get(Graph.Factory);

		tray

				.exec(() -> tool(Graph.Factory).update(connection -> {
					connection.add(decode("<test> rdf:value rdf:first."));
				}))

				.get(() -> new Processor()
						.update(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((Handler)request -> request.reply(response -> response.status(Response.OK))))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/test"))

				.accept(response -> graph.query(connection -> {
					assertIsomorphic("repository updated",
							decode("<test> rdf:value rdf:first, rdf:rest."),
							export(connection)
					);
				}));
	}

	@Test void testExecuteUpdateScriptOnResponseLocation() {

		final Tray tray=new Tray();
		final Graph graph=tray.get(Graph.Factory);

		tray

				.exec(() -> tool(Graph.Factory).update(connection -> {
					connection.add(decode("<test> rdf:value rdf:first."));
				}))

				.get(() -> new Processor()
						.update(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((Handler)request -> request.reply(response -> response
								.status(Response.OK)
								.header("Location", Base+"test"))))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/"))

				.accept(response -> graph.query(connection -> {
					assertIsomorphic("repository updated",
							decode("<test> rdf:value rdf:first, rdf:rest."),
							export(connection)
					);
				}));
	}

}
