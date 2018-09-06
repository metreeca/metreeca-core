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

package com.metreeca.rest;

import com.metreeca.form.Shape;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.form.things._JSON;
import com.metreeca.tray.sys._Setup;

import org.eclipse.rdf4j.model.IRI;
import org.junit.Test;

import java.util.Properties;

import static com.metreeca.rest.LinkTest.json;
import static com.metreeca.rest.LinkTest.testbed;
import static com.metreeca.rest.RewriterTest.*;
import static com.metreeca.rest.Server.server;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.assertIsomorphic;
import static com.metreeca.tray._Tray.tool;

import static org.junit.Assert.assertEquals;

import static java.util.Collections.singleton;


public class ServerTest {

	private void setup() {
		tool(_Setup.Factory, () -> new _Setup(setup -> {

			final Properties properties=new Properties();

			properties.setProperty(_Setup.BaseProperty, Internal);

			return properties;

		}));
	}


	//// IRI Rewriting /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testHeadRewriting() {
		testbed()

				.toolkit(() -> setup())

				.request(request -> request

						.user(external("user"))
						.roles(external("role"))

						.method(Request.GET)

						.base(External)
						.path("/path")

						.query(external("request").toString())
						.parameter("request", external("request").toString())

						.header("request", "request="+external("request"))

						.done())

				.handler(() -> server((request, response) -> {

					assertEquals("user rewritten", internal("user"), request.user());
					assertEquals("roles rewritten", singleton(internal("role")), request.roles());

					assertEquals("base rewritten", Internal, request.base());
					assertEquals("focus rewritten", internal("path"), request.focus());

					assertEquals("query rewritten", internal("request").toString(), request.query());

					assertEquals("parameters rewritten",
							internal("request").toString(),
							request.parameter("request").orElse(""));

					assertEquals("request headers rewritten",
							"request="+internal("request"),
							request.header("request").orElse(""));

					response.status(Response.OK)
							.header("response", "response="+internal("response"))
							.done();

				}))

				.response(response -> {

					assertEquals("response headers rewritten",
							"response="+external("response"),
							response.header("response").orElse(""));

				});
	}

	@Test public void testRDFRewriting() {
		testbed()

				.toolkit(() -> setup())

				.request(request -> request

						.method(Request.PUT)

						.base(External)
						.path("/s")

						.text(ValuesTest.encode(singleton(external("s", "p", "o")))))

				.handler(() -> server((request, response) -> {

					assertIsomorphic("request rdf rewritten",
							singleton(internal("s", "p", "o")),
							request.rdf());

					response.status(Response.OK)
							.rdf(singleton(internal("s", "p", "o")));

				}))

				.response(response -> {

					assertIsomorphic("response rdf rewritten",
							singleton(external("s", "p", "o")),
							response.rdf());

				});
	}

	@Test public void testJSONRewriting() {
		testbed()

				.toolkit(() -> setup())

				.request(request -> request

						.method(Request.PUT)

						.base(External)
						.path("/s")

						.header("content-type", "application/json")
						.header("accept", "application/json")

						.text(json("{ 'p': 'o' }")))

				.handler(() -> server((request, response) -> {

					final Shape shape=trait(internal("p"), and(required(), datatype(Values.IRIType)));
					final IRI focus=internal("o");

					assertIsomorphic("request json rewritten",
							request.rdf(shape, internal("s")),
							singleton(statement(internal("s"), internal("p"), focus)));

					response.status(Response.OK)
							.rdf(singleton(internal("s", "p", "o")),
									shape);

				}))

				.response(response -> {

					assertEquals("response json rewritten",
							_JSON.decode(json("{ 'this': '"+external("s")+"', 'p': '"+external("o")+"' }")),
							response.json());

				});
	}

}
