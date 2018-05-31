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

package com.metreeca.link.wrappers;

import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.spec.things.ValuesTest;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Test;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.link.wrappers.Processor.processor;
import static com.metreeca.spec.things.ValuesTest.assertIsomorphic;
import static com.metreeca.spec.things.ValuesTest.export;
import static com.metreeca.spec.things.ValuesTest.parse;
import static com.metreeca.spec.things.ValuesTest.sparql;
import static com.metreeca.tray.Tray.tool;


public final class ProcessorTest {

	@Test public void testExecuteUpdateScriptOnRequestFocus() {
		testbed()

				.dataset(parse("<test> rdf:value rdf:first."))

				.handler(() -> processor()
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((request, response) -> response
								.status(Response.OK)
								.done()
						))

				.request(writer -> writer
						.method(Request.POST)
						.base(ValuesTest.Base)
						.path("/test")
						.done())

				.response(reader -> {

					try (final RepositoryConnection connect=tool(Graph.Factory).connect()) {
						assertIsomorphic("repository updated",
								parse("<test> rdf:value rdf:first, rdf:rest."),
								export(connect)
						);
					}

				});
	}

	@Test public void testExecuteUpdateScriptOnResponseLocation() {
		testbed()

				.dataset(parse("<test> rdf:value rdf:first."))

				.handler(() -> processor()
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((request, response) -> response
								.status(Response.OK)
								.header("Location", ValuesTest.Base+"test")
								.done()
						))

				.request(writer -> writer
						.method(Request.POST)
						.base(ValuesTest.Base)
						.path("/")
						.done())

				.response(reader -> {

					try (final RepositoryConnection connect=tool(Graph.Factory).connect()) {
						assertIsomorphic("repository updated",
								parse("<test> rdf:value rdf:first, rdf:rest."),
								export(connect)
						);
					}

				});
	}

}
