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

import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.tray.Tray.tool;


final class ProcessorTest {

	//@Test void testShapedRelatePiped() {
	//	testbed()
	//
	//			.handler(() -> relator().shape(ValuesTest.Employee)
	//
	//					.pipe((request, model) -> {
	//
	//						model.add(statement(request.focus(), RDF.VALUE, RDF.FIRST));
	//
	//						return model;
	//
	//					})
	//
	//					.pipe((request, model) -> {
	//
	//						model.add(statement(request.focus(), RDF.VALUE, RDF.REST));
	//
	//						return model;
	//
	//					}))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(LinkTest.Manager)
	//					.done())
	//
	//			.response(response -> {
	//
	//				ValuesTest.assertSubset("items retrieved", asList(
	//
	//						statement(response.focus(), RDF.VALUE, RDF.FIRST),
	//						statement(response.focus(), RDF.VALUE, RDF.REST)
	//
	//				), response.rdf());
	//
	//			});
	//
	//}


	@Test void testExecuteUpdateScriptOnRequestFocus() {

		final Tray tray=new Tray();
		final Graph graph=tray.get(Graph.Factory);

		tray

				.exec(() -> tool(Graph.Factory).update(connection -> {
					connection.add(decode("<test> rdf:value rdf:first."));
				}))

				.get(() -> new Processor()
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
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
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
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
