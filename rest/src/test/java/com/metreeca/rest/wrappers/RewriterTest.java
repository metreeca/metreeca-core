/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats._RDF;
import com.metreeca.rest.formats.ReaderFormat;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.assertIsomorphic;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static java.util.Collections.singleton;


final class RewriterTest {

	private static final String External=ValuesTest.Base;
	private static final String Internal="app://test/";


	private static IRI external(final String name) {
		return iri(External, name);
	}

	private static IRI internal(final String name) {
		return iri(Internal, name);
	}


	private static Statement internal(final String subject, final String predicate, final String object) {
		return statement(internal(subject), internal(predicate), internal(object));
	}

	private static Statement external(final String subject, final String predicate, final String object) {
		return statement(external(subject), external(predicate), external(object));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRejectRelativeBase() {
		assertThrows(IllegalArgumentException.class, () -> new Rewriter().base("/example.org/"));
	}


	//// IRI Rewriting /////////////////////////////////////////////////////////////////////////////////////////////////

	//@Test public void testHeadRewriting() {
	//	LinkTest.testbed()
	//
	//			.toolkit(() -> setup())
	//
	//			.request(request -> request
	//
	//					.user(external("user"))
	//					.roles(external("role"))
	//
	//					.method(Request.GET)
	//
	//					.base(External)
	//					.path("/path")
	//
	//					.query(external("request").toString())
	//					.parameter("request", external("request").toString())
	//
	//					.header("request", "request="+external("request"))
	//
	//					.done())
	//
	//			.handler(() -> server((request, response) -> {
	//
	//				assertEquals("user rewritten", internal("user"), request.user());
	//				assertEquals("roles rewritten", singleton(internal("role")), request.roles());
	//
	//				assertEquals("base rewritten", Internal, request.base());
	//				assertEquals("focus rewritten", internal("path"), request.focus());
	//
	//				assertEquals("query rewritten", internal("request").toString(), request.query());
	//
	//				assertEquals("parameters rewritten",
	//						internal("request").toString(),
	//						request.parameter("request").orElse(""));
	//
	//				assertEquals("request headers rewritten",
	//						"request="+internal("request"),
	//						request.header("request").orElse(""));
	//
	//				response.status(Response.OK)
	//						.header("response", "response="+internal("response"))
	//						.done();
	//
	//			}))
	//
	//			.response(response -> {
	//
	//				Assert.assertEquals("response headers rewritten",
	//						"response="+external("response"),
	//						response.header("response").orElse(""));
	//
	//			});
	//}

	@Test void testReaderRewriting() {

		new Tray()

				.get(() -> (Handler)request -> {

					request.body(_RDF.Format).handle(
							model -> assertIsomorphic("request rdf rewritten",
									singleton(internal("s", "p", "o")), model),
							error -> fail("missing RDF payload")
					);

					return request.reply(response -> response.status(Response.OK)
							.body(_RDF.Format, singleton(internal("s", "p", "o"))));
				})

				.handle(new Request()

						.method(Request.PUT)

						.base(External)
						.path("/s")

						.body(ReaderFormat.asReader, () -> new StringReader(ValuesTest.encode(singleton(external("s", "p", "o"))))))

				.accept(response -> {

					response.body(_RDF.Format).handle(
							model -> assertIsomorphic("response rdf rewritten",
									singleton(external("s", "p", "o")), model),
							error -> fail("missing RDF payload")
					);

				});
	}

	@Test void testRDFRewriting() {

	}

	//@Test public void testJSONRewriting() {
	//	LinkTest.testbed()
	//
	//			.toolkit(() -> setup())
	//
	//			.request(request -> request
	//
	//					.method(Request.PUT)
	//
	//					.base(External)
	//					.path("/s")
	//
	//					.header("content-type", "application/json")
	//					.header("accept", "application/json")
	//
	//					.text(LinkTest.json("{ 'p': 'o' }")))
	//
	//			.handler(() -> server((request, response) -> {
	//
	//				final Shape shape=trait(internal("p"), and(required(), datatype(Values.IRIType)));
	//				final IRI focus=internal("o");
	//
	//				assertIsomorphic("request json rewritten",
	//						request.rdf(shape, internal("s")),
	//						singleton(statement(internal("s"), internal("p"), focus)));
	//
	//				response.status(Response.OK)
	//						.rdf(singleton(internal("s", "p", "o")),
	//								shape);
	//
	//			}))
	//
	//			.response(response -> {
	//
	//				Assert.assertEquals("response json rewritten",
	//						JSON.decode(LinkTest.json("{ 'this': '"+external("s")+"', 'p': '"+external("o")+"' }")),
	//						response.json());
	//
	//			});
	//}


	//@org.junit.Test public void testRewriteReader() throws IOException {
	//
	//	final Reader external=new StringReader("<"+external("test")+">");
	//	final Reader internal=rewriter(External, Internal).internal(external);
	//
	//	assertEquals("reader rewritten", "<"+internal("test")+">", Transputs.text(internal));
	//}

	//@org.junit.Test public void testRewriteWriter() throws IOException {
	//
	//	final StringWriter external=new StringWriter();
	//
	//	try (final Writer internal=rewriter(External, Internal).external(external)) {
	//		internal.write("<"+internal("test")+">");
	//	}
	//
	//	assertEquals("writer rewritten", "<"+external("test")+">", external.toString());
	//}

}
