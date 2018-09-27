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

import com.metreeca.form.Shape;
import com.metreeca.form.things.Transputs;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.json.Json;

import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Transputs.encode;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.assertIsomorphic;
import static com.metreeca.rest.formats.InputFormat.asInput;
import static com.metreeca.rest.formats.OutputFormat.asOutput;
import static com.metreeca.rest.formats.RDFFormat.asRDF;
import static com.metreeca.rest.formats.ShapeFormat.asShape;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static java.util.Collections.singleton;


final class RewriterTest {

	private static final String External=ValuesTest.Base;
	private static final String Internal="app://test/";

	private static final Shape TestShape=trait(internal("p"), and(required(), datatype(Values.IRIType)));


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

	@Test void testHeadRewriting() {
		new Tray()

				.get(() -> new Rewriter().base(Internal).wrap((Handler)request -> {

					assertThat(request.user()).named("rewritten user").isEqualTo(internal("user"));
					assertThat(request.roles()).named("rewritten roles").containsExactly(internal("role"));

					assertThat(request.base()).named("rewritten base").isEqualTo(Internal);
					assertThat(request.item()).named("rewritten item").isEqualTo(internal("path"));

					assertThat(request.query()).named("rewritten query").isEqualTo(internal("request").toString());

					assertThat(request.parameters("request"))
							.named("rewritten parameter")
							.containsExactly(internal("request").toString());

					assertThat(request.headers("request"))
							.named("rewritten header")
							.containsExactly("request="+internal("request"));

					return request.reply(response -> response.status(Response.OK)
							.header("response", "response="+internal("response")));

				}))

				.handle(new Request()

						.user(external("user"))
						.roles(external("role"))

						.method(Request.GET)

						.base(External)
						.path("/path")

						.query(external("request").toString())
						.parameter("request", external("request").toString())

						.header("request", "request="+external("request"))
				)

				.accept(response -> {

					assertThat(response.headers("response"))
							.named("rewritten response header")
							.containsExactly("response="+external("response"));

				});
	}

	@Test void testHeadEncodedRewriting() {
		new Tray()

				.get(() -> new Rewriter().base(Internal).wrap((Handler)request -> {

					assertThat(request.query())
							.named("rewritten encoded query")
							.isEqualTo(encode(internal("request").toString()));

					return request.reply(response -> response.status(Response.OK));

				}))

				.handle(new Request()

						.base(External)
						.query(encode(external("request").toString()))

				)

				.accept(response -> {});
	}

	@Test void testRDFRewriting() {
		new Tray()

				.get(() -> new Rewriter().base(Internal).wrap((Handler)request -> {

					request.body(asRDF).handle(
							model -> assertIsomorphic("request rdf rewritten",
									singleton(internal("s", "p", "o")), model),
							error -> fail("missing RDF payload")
					);

					return request.reply(response -> response.status(Response.OK)
							.body(asRDF, singleton(internal("s", "p", "o"))));
				}))

				.handle(new Request()

						.base(External)
						.path("/s")

						.body(asRDF, singleton(external("s", "p", "o"))))

				.accept(response -> {

					response.body(asRDF).handle(
							model -> assertIsomorphic("response rdf rewritten",
									singleton(external("s", "p", "o")), model),
							error -> fail("missing RDF payload")
					);

				});
	}

	@Test void testJSONRewriting() {
		new Tray()

				.get(() -> new Rewriter().base(Internal).wrap((Handler)request -> {

					request.body(asRDF).handle(
							model -> assertIsomorphic("request json rewritten",
									singleton(internal("s", "p", "o")), model),
							error -> fail("missing RDF payload")
					);

					return request.reply(response -> response.status(Response.OK)
							.body(asShape, TestShape)
							.body(asRDF, singleton(internal("s", "p", "o"))));
				}))

				.handle(new Request()

						.base(External)
						.path("/s")

						.header("content-type", "application/json")
						.header("accept", "application/json")

						.body(asShape, TestShape)

						.body(asInput, () -> new ByteArrayInputStream(
								Json.createObjectBuilder()
										.add("p", "o")
										.build()
										.toString()
										.getBytes(Transputs.UTF8))))


				.accept(response -> {

					response.body(asOutput).handle(
							value -> {

								final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

								value.accept(() -> buffer);

								assertThat(Json.createReader(new ByteArrayInputStream(buffer.toByteArray())).readObject())
										.named("rewritten response json")
										.isEqualTo(Json.createObjectBuilder()
												.add("this", External+"s")
												.add("p", External+"o")
												.build());

							},
							error -> fail("missing output body")
					);

				});
	}

}
