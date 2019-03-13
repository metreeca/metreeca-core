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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Field;
import com.metreeca.form.things.Codecs;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import javax.json.Json;

import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Codecs.encode;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.MultipartBody.multipart;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.OutputBody.output;
import static com.metreeca.rest.bodies.RDFBody.rdf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static java.util.Collections.singleton;


final class RewriterTest {

	private static final String External="app://external/";
	private static final String Internal="app://internal/";


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
		assertThrows(IllegalArgumentException.class, () -> new Rewriter("/example.org/"));
	}

	@Test void testHeadRewriting() {
		new Tray()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.user()).as("rewritten user").isEqualTo(internal("user"));
					assertThat(request.roles()).as("rewritten roles").containsExactly(internal("role"));

					assertThat(request.base()).as("rewritten base").isEqualTo(Internal);
					assertThat(request.item()).as("rewritten item").isEqualTo(internal("path"));

					assertThat(request.query()).as("rewritten query").isEqualTo(internal("request").toString());

					assertThat(request.parameters("request"))
							.as("rewritten parameter")
							.containsExactly(internal("request").toString());

					assertThat(request.headers("request"))
							.as("rewritten header")
							.containsExactly("request="+internal("request"));

					return request.reply(response -> response.status(OK)
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
							.as("rewritten response header")
							.containsExactly("response="+external("response"));

				});
	}

	@Test void testHeadEncodedRewriting() {
		new Tray()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.query())
							.as("rewritten encoded query")
							.isEqualTo(encode(internal("request").toString()));

					return request.reply(response -> response.status(OK));

				}))

				.handle(new Request()

						.base(External)
						.query(encode(external("request").toString()))

				)

				.accept(response -> {});
	}

	@Test void testShapeRewriting() {
		new Tray()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.shape()).isEqualTo(and(
							Field.field(internal("p")),
							Field.field(inverse(internal("p")))
					));

					return request.reply(response -> response
							.status(OK)
							.shape(request.shape()));

				}))

				.handle(new Request()

						.base(External)

						.shape(and(
								Field.field(external("p")),
								Field.field(inverse(external("p")))
						))

				)

				.accept(response -> {

					assertThat(response.shape()).isEqualTo(and(
							Field.field(external("p")),
							Field.field(inverse(external("p")))
					));

				});

	}

	@Test void testRDFRewriting() {
		new Tray()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					request.body(rdf()).use(
							model -> ModelAssert.assertThat(internal("s", "p", "o"))
									.as("request rdf rewritten")
									.isIsomorphicTo(model),
							error -> fail("missing RDF payload")
					);

					return request.reply(response -> response.status(OK)
							.body(rdf(), singleton(internal("s", "p", "o"))));
				}))

				.handle(new Request()

						.base(External)
						.path("/s")

						.body(rdf(), singleton(external("s", "p", "o"))))

				.accept(response -> {

					response.body(rdf()).use(
							model -> assertThat(singleton(external("s", "p", "o")))
									.as("response rdf rewritten")
									.isIsomorphicTo(model),
							error -> fail("missing RDF payload")
					);

				});
	}

	@Test void testJSONRewriting() {

		final Shape TestShape=field(internal("p"), and(required(), datatype(Form.IRIType)));

		new Tray()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request).hasBody(rdf(), rdf -> assertThat(rdf)
							.as("request json rewritten")
							.isIsomorphicTo(singleton(internal("s", "p", "o"))));

					return request.reply(response -> response.
							status(OK)
							.shape(TestShape)
							.body(rdf(), singleton(internal("s", "p", "o"))));
				}))

				.handle(new Request()

						.base(External)
						.path("/s")

						.header("content-type", "application/json")
						.header("accept", "application/json")

						.shape(TestShape)

						.body(input(), () -> new ByteArrayInputStream(
								object(entry("p", "o"))
										.toString()
										.getBytes(Codecs.UTF8)
						))
				)


				.accept(response -> {

					response.body(output()).use(
							value -> {

								final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

								value.accept(() -> buffer);

								assertThat(Json.createReader(new ByteArrayInputStream(buffer.toByteArray())).readObject())
										.as("rewritten response json")
										.isEqualTo(object(
												entry("this", "/s"),
												entry("p", "/o")
										));

							},
							error -> fail("missing output body")
					);

				});
	}


	@Test void testMergedMainPartRewriting() {
		new Tray()

				.get(() -> new Rewriter(Internal)

						.wrap((Wrapper)handler -> request -> request.body(multipart(150, 300)).fold(

								parts -> Optional.ofNullable(parts.get("main"))

										.map(main -> {

											assertThat(parts.keySet())
													.containsExactly("main", "file");

											return handler.handle(request.merge(main));

										})

										.orElseGet(() -> request.reply(new Failure()
												.status(BadRequest)
												.cause("missing main body part")
										)),

								request::reply

						))

						.wrap((Handler)request -> {

							assertThat(request).hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(internal("s", "p", "o"))
							);

							return request.reply(response -> response.status(OK));

						})
				)

				.handle(new Request()

						.base(External)
						.path("/s")

						.header("Content-Type", "multipart/form-data; boundary=\"boundary\"")

						.body(input(), () -> new ByteArrayInputStream(("--boundary\n"
								+"Content-Disposition: form-data; name=\"main\"\n"
								+"Content-Type: text/turtle\n"
								+"\n"
								+"<> <app://external/p> <app://external/o> .\n"
								+"\n"
								+"--boundary\t\t\n"
								+"Content-Disposition: form-data; name=\"file\"; filename=\"example.txt\"\n"
								+"\n"
								+"text\n"
								+"--boundary--\n"
						).replace("\n", "\r\n").getBytes(Codecs.UTF8))))

				.accept(response -> assertThat(response).hasStatus(OK));
	}

}
