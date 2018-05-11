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

package com.metreeca.link.handlers;

import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.jeep.rdf.ValuesTest.assertIsomorphic;
import static com.metreeca.jeep.rdf.ValuesTest.parse;
import static com.metreeca.link.HandlerTest.model;
import static com.metreeca.link.HandlerTest.response;
import static com.metreeca.link.HandlerTest.tools;


public class UpdaterTest {

	@Test public void testExecuteHandlerUnconditionally() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			response(tools,

					new Updater(tools,

							(_tools, request, response, sink) -> {

								model(tools.get(Graph.Tool), parse("rdf:nil rdf:value rdf:first."));

								sink.accept(request, response.setStatus(Response.OK));
							},

							map(entry(Request.PUT, "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
									+"insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))),

					new Request()

							.setMethod(Request.POST)
							.setBase(RDF.NAMESPACE)
							.setTarget(RDF.NIL.toString()),

					(request, response) -> assertIsomorphic(
							parse("rdf:nil rdf:value rdf:first."),
							model(graph)
					)

			);
		});
	}

	@Test public void testExecuteUpdateScriptOnTarget() {
		tools(tools -> {

			final Graph graph=model(tools.get(Graph.Tool), parse("rdf:nil rdf:value rdf:first."));

			response(tools,

					new Updater(tools,

							(_tools, request, response, sink) -> sink.accept(request, response.setStatus(Response.OK)),

							map(entry(Request.POST, "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
									+"insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))),

					new Request()

							.setMethod(Request.POST)
							.setBase(RDF.NAMESPACE)
							.setTarget(RDF.NIL.toString()),

					(request, response) -> assertIsomorphic("repository updated",
							parse("rdf:nil rdf:value rdf:first, rdf:rest."),
							model(graph)
					)

			);
		});
	}

	@Test public void testExecuteUpdateScriptOnLocation() {
		tools(tools -> {

			final Graph graph=model(tools.get(Graph.Tool), parse("rdf:nil rdf:value rdf:first."));

			response(tools,

					new Updater(tools,

							(_tools, request, response, sink) -> sink.accept(request, response
									.setStatus(Response.OK)
									.setHeader("Location", RDF.NIL.stringValue())),

							map(entry(Request.POST, "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
									+"insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))),

					new Request()

							.setMethod(Request.POST)
							.setBase(RDF.NAMESPACE)
							.setTarget(""),

					(request, response) -> assertIsomorphic("repository updated",
							parse("rdf:nil rdf:value rdf:first, rdf:rest."),
							model(graph))

			);
		});
	}

	@Test public void testExecuteWildcardScript() {
		tools(tools -> {

			final Graph graph=model(tools.get(Graph.Tool), parse("rdf:nil rdf:value rdf:first."));

			response(tools,

					new Updater(tools,

							(_tools, request, response, sink) -> sink.accept(request, response.setStatus(Response.OK)),

							map(entry(Request.ANY, "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
									+"insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))),

					new Request()

							.setMethod(Request.POST)
							.setBase(RDF.NAMESPACE)
							.setTarget(RDF.NIL.toString()),

					(request, response) -> assertIsomorphic("repository updated",
							parse("rdf:nil rdf:value rdf:first, rdf:rest."),
							model(graph)
					)

			);
		});
	}

}
