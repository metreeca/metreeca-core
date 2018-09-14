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

package com.metreeca.next.wrappers;

import com.metreeca.next.Handler;
import com.metreeca.next.Request;
import com.metreeca.next.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.tray.Tray.tool;


final class ProcessorTest {

	@Test void testExecuteUpdateScriptOnRequestFocus() {

		final Tray tray=new Tray();

		tray

				.run(() -> tool(Graph.Factory).update(connection -> {
					connection.add(decode("<test> rdf:value rdf:first."));
				}))

				.get(() -> new Processor()
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((Handler)request -> request.response().status(Response.OK)))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/test"))

				.accept(response -> tray.run(() -> tool(Graph.Factory).browse(connection -> {
					assertIsomorphic("repository updated",
							decode("<test> rdf:value rdf:first, rdf:rest."),
							export(connection)
					);
				})));
	}

	@Test void testExecuteUpdateScriptOnResponseLocation() {

		final Tray tray=new Tray();

		tray

				.run(() -> tool(Graph.Factory).update(connection -> {
					connection.add(decode("<test> rdf:value rdf:first."));
				}))

				.get(() -> new Processor()
						.script(sparql("insert { ?this rdf:value rdf:rest } where { ?this rdf:value rdf:first }"))
						.wrap((Handler)request -> request.response()
								.status(Response.OK)
								.header("Location", Base+"test")))

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/"))

				.accept(response -> tray.run(() -> tool(Graph.Factory).browse(connection -> {
					assertIsomorphic("repository updated",
							decode("<test> rdf:value rdf:first, rdf:rest."),
							export(connection)
					);
				})));
	}

}
