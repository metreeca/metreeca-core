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

package com.metreeca.rest.bodies;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.Message;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.assertj.core.api.Condition;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.function.Supplier;

import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.rest.Body.Missing;
import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.MultipartBody.multipart;
import static com.metreeca.rest.bodies.ReaderBody.reader;
import static com.metreeca.rest.bodies.TextBody.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


final class MultipartBodyTest {

	@Nested final class Input {

		private static final String type="multipart/form-data; boundary=\"boundary\"";

		private Supplier<InputStream> content() {
			return content("\n"
					+"preamble\n"
					+"\n"
					+"--boundary\n"
					+"Content-Disposition: form-data; name=\"main\"\n"
					+"Content-Type: text/turtle\n"
					+"\n"
					+"@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
					+"\n"
					+"<> rdf:value rdf.nil.\n"
					+"\n"
					+"--boundary\t\t\n"
					+"Content-Disposition: form-data; name=\"file\"; filename=\"example.txt\"\n"
					+"\n"
					+"text\n"
					+"--boundary--\n"
					+"\n"
					+"\n"
					+"epilogue\n"
			);
		}

		private Supplier<InputStream> content(final String content) {
			return () -> new ByteArrayInputStream(
					content.replace("\n", "\r\n").getBytes(Codecs.UTF8)
			);
		}


		@Test void testParseMultipartBodies() {
			new Request()

					.header("Content-Type", type)

					.body(input(), content())

					.body(multipart(250, 1000))

					.fold(

							parts -> {

								assertThat(parts.size())
										.isEqualTo(2);

								assertThat(parts.keySet())
										.containsExactly("main", "file");

								assertThat(parts.get("file"))
										.as("part available by name")
										.hasItem(iri("file:example.txt"))
										.hasHeader("Content-Disposition")
										.hasBody(reader(), source -> assertThat(Codecs.text(source.get()))
												.isEqualTo("text")
										);

								return this;

							},

							error -> fail("unexpected failure {"+error+"}")

					);
		}

		@Test void testCacheIdempotentResults() {

			final Request request=new Request()
					.header("Content-Type", type)
					.body(input(), content());

			final Map<String, Message<?>> one=request
					.body(multipart(250, 1000))
					.value()
					.orElseGet(() -> fail("missing multipart body"));

			final Map<String, Message<?>> two=request
					.body(multipart())
					.value()
					.orElseGet(() -> fail("missing multipart body"));

			assertThat(one)
					.as("idempotent")
					.isSameAs(two);
		}


		@Test void testIgnoreUnrelatedContentTypes() {
			new Request()

					.header("Content-Type", "plain/test")
					.body(input(), content())

					.body(multipart())

					.fold(
							parts -> fail("unexpected multipart body"),
							error -> assertThat(error).isEqualTo(Missing)
					);
		}

		@Test void testRejectMalformedPayloads() {
			new Request()

					.header("Content-Type", "multipart/data")
					.body(input(), content())

					.body(multipart())

					.use(
							parts -> fail("unexpected multipart body"),
							error -> new Request().reply(error).accept(response -> assertThat(response)
									.as("missing boundary parameter")
									.hasStatus(Response.BadRequest)
							)
					);
		}

	}

	@Nested final class Output {

		@Test void testGenerateRandomBoundary() {
			new Request().reply(response -> response

					.status(Response.OK)
					.body(multipart(), map(
							entry("one", response.link(RDF.FIRST).body(text(), "one")),
							entry("two", response.link(RDF.FIRST).body(text(), "two"))
					))

			).accept(response -> assertThat(response)

					.has(new Condition<>(
							r -> r.header("Content-Type").filter(s -> s.contains("; boundary=")).isPresent(),
							"multipart boundary set"
					))


			);
		}

		@Test void testPreserveCustomBoundary() {
			new Request().reply(response -> response

					.status(Response.OK)
					.header("Content-Type", "multipart/form-data; boundary=1234567890")
					.body(multipart(), map())

			).accept(response -> assertThat(response)

					.hasHeader("Content-Type", "multipart/form-data; boundary=1234567890")

					.hasBody(text(), text -> assertThat(text)
							.contains("--1234567890--")
					)

			);
		}

	}

}
