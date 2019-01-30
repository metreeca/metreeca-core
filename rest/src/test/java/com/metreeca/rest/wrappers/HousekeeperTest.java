/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import static com.metreeca.form.things.ValuesTest.Base;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.things.ValuesTest.export;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.wrappers.Housekeeper.sparql;
import static com.metreeca.tray.Tray.tool;


final class HousekeeperTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	@Test void testExecuteTaskOnRequestFocus() {
		exec(() -> new Housekeeper(
				sparql("insert { ?this rdf:value rdf:first } where {}"),
				sparql("insert { ?this rdf:value rdf:rest } where {}")
		)

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
		exec(() -> new Housekeeper(
				sparql("insert { ?this rdf:value rdf:first } where {}"),
				sparql("insert { ?this rdf:value rdf:rest } where {}")
		)

				.wrap((Handler)request -> request.reply(response -> response
						.status(Response.OK)
						.header("Location", Base+"test"))
				)

				.handle(new Request()
						.method(Request.POST)
						.base(Base)
						.path("/")
				)

				.accept(response -> tool(Graph.Factory).query(connection -> {

					assertThat(decode("<test> rdf:value rdf:first, rdf:rest."))
							.as("repository updated")
							.isIsomorphicTo(export(connection));

				})));
	}

}
