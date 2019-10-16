/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.wrappers;

import com.metreeca.rdf.Values;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.*;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Optional;

import javax.json.Json;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.MultipartFormat.multipart;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import static java.nio.charset.StandardCharsets.UTF_8;
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


	private String encode(final IRI iri) {
		return Codecs.encode(iri.toString());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRejectRelativeBase() {
		assertThrows(IllegalArgumentException.class, () -> new Rewriter("/example.org/"));
	}

	@Test void testHeadRewriting() {
		new Context()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.user()).as("rewritten user").contains(internal("user"));
					assertThat(request.roles()).as("rewritten roles").containsExactly(internal("role"));

					assertThat(request.base()).as("rewritten base").isEqualTo(Internal);
					assertThat(request.item()).as("rewritten item").isEqualTo(internal("path").stringValue());

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
		new Context()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.query())
							.as("rewritten encoded query")
							.isEqualTo(encode(internal("one"))+"&"+encode(internal("two")));

					return request.reply(response -> response.status(OK));

				}))

				.handle(new Request()

						.base(External)
						.query(encode(external("one"))+"&"+encode(external("two")))

				)

				.accept(response -> {});
	}

	@Test void testShapeRewriting() {
		new Context()

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					assertThat(request.shape())
							.isEqualTo(and(
									field(internal("p")),
									field(inverse(internal("p")))
							));

					return request.reply(response -> response
							.status(OK)
							.shape(request.shape()));

				}))

				.handle(new Request()

						.base(External)

						.shape(and(
								field(external("p")),
								field(inverse(external("p")))
						))

				)

				.accept(response -> assertThat(response.shape())
						.isEqualTo(and(
								field(external("p")),
								field(inverse(external("p")))
						)));

	}

	@Test void testRDFRewriting() {
		final Context context=new Context();

		context.exec(() -> context

				.get(() -> new Rewriter(Internal).wrap((Handler)request -> {

					request.body(rdf()).fold(

							model -> assertThat(model)
									.as("request rdf rewritten")
									.isIsomorphicTo(internal("s", "p", "o")),

							error -> fail("missing RDF payload")

					);

					return request.reply(response -> response.status(OK)
							.body(rdf(), singleton(internal("s", "p", "o"))));
				}))

				.handle(new Request()

						.base(External)
						.path("/s")

						.body(input(), () -> Codecs.input(new StringReader(ValuesTest.encode(singleton(
								external("s", "p", "o")
						))))))

				.accept(response -> response.body(output()).fold(

						consumer -> {

							try (final ByteArrayOutputStream stream=new ByteArrayOutputStream()) {

								consumer.accept(() -> stream);

								assertThat(decode(new String(stream.toByteArray(), UTF_8)))
										.as("response rdf rewritten")
										.isIsomorphicTo(singleton(external("s", "p", "o")));

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}

							return this;

						},

						error -> fail("missing RDF payload")

				))
		);
	}

	@Test void testJSONRewriting() {

		final Shape TestShape=field(internal("p"), and(required(), datatype(Values.IRIType)));

		final Context context=new Context();

		context.exec(() -> context

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

						.body(input(), () -> new ByteArrayInputStream(Json.createObjectBuilder()
								.add("p", "o")
								.build()
								.toString()
								.getBytes(UTF_8)
						))
				)

				.accept(response -> response.body(output()).fold(

						value -> {

							try (final ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {

								value.accept(() -> buffer);

								assertThat(Json.createReader(new ByteArrayInputStream(buffer.toByteArray())).readObject())
										.as("rewritten response json")
										.isEqualTo(Json.createObjectBuilder()
												.add("@id", "/s")
												.add("p", "/o")
												.build()
										);

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}

							return this;

						},

						error -> fail("missing output body")

				))
		);

	}


	@Test void testMergedMainPartRewriting() {
		final Context context=new Context();

		context.exec(() -> context

				.get(() -> new Rewriter(Internal)

						.wrap((Wrapper)handler -> request -> request.body(multipart(150, 300)).fold(

								parts -> Optional.ofNullable(parts.get("main"))

										.map(main -> {

											assertThat(parts.keySet())
													.containsExactly("main", "file");

											return handler.handle(request.lift(main));

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
						).replace("\n", "\r\n").getBytes(UTF_8))))

				.accept(response -> ResponseAssert.assertThat(response).hasStatus(OK))
		);
	}

}
