/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Map;
import java.util.function.Supplier;

import static com.metreeca.rest.Xtream.entry;
import static com.metreeca.rest.Xtream.map;
import static com.metreeca.rest.formats.MultipartFormat.multipart;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;


final class MultipartFormatTest {


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
					content.replace("\n", "\r\n").getBytes(UTF_8)
			);
		}


		@Test void testParseMultipartBodies() {
			new Request()

					.header("Content-Type", type)

					.body(InputFormat.input(), content())

					.body(multipart(250, 1000))

					.fold(

							error -> Assertions.fail("unexpected failure {"+error+"}"), parts -> {

								assertThat(parts.size())
										.isEqualTo(2);

								assertThat(parts.keySet())
										.containsExactly("main", "file");

								MessageAssert.assertThat(parts.get("file"))
										.as("part available by name")
										.hasItem("file:example.txt")
										.hasHeader("Content-Disposition")
										.hasBody(text(), text -> assertThat(text)
												.isEqualTo("text")
										);

								return this;

							}

					);
		}

		@Test void testCacheIdempotentResults() {

			final Request request=new Request()
					.header("Content-Type", type)
					.body(InputFormat.input(), content());

			final Map<String, Message<?>> one=request
					.body(multipart(250, 1000))
					.fold(e -> Assertions.fail("missing multipart body"), identity());

			final Map<String, Message<?>> two=request
					.body(multipart())
					.fold(e -> Assertions.fail("missing multipart body"), identity());

			Assertions.assertThat(one)
					.as("idempotent")
					.isSameAs(two);
		}


		@Test void testIgnoreUnrelatedContentTypes() {
			new Request()

					.header("Content-Type", "plain/test")
					.body(InputFormat.input(), content())

					.body(multipart())

					.fold(
							Assertions::assertThat, parts -> Assertions.fail("unexpected multipart body")
					);
		}

		@Test void testRejectMalformedPayloads() {
			new Request()

					.header("Content-Type", "multipart/data")
					.body(InputFormat.input(), content())

					.body(multipart())

					.fold(

							error -> {

								new Request().reply(error).accept(response -> ResponseAssert.assertThat(response)
										.as("missing boundary parameter")
										.hasStatus(Response.BadRequest)
								);


								return this;
							}, parts -> {
								Assertions.fail("unexpected multipart body");


								return this;

							}

					);
		}

	}

	@Nested final class Output {

		@Test void testGenerateRandomBoundary() {
			new Request().reply(response -> response

					.status(Response.OK)
					.body(multipart(), map(
							entry("one", response.part("one").body(text(), "one")),
							entry("two", response.part("two").body(text(), "two"))
					))

			).accept(response -> MessageAssert.assertThat(response)

					.has(new Condition<>(
							r -> r.header("Content-Type").filter(s -> s.contains("; boundary=")).isPresent(),
							"multipart boundary set"
					))

			);
		}

		@Test void testPreserveCustomBoundary() {
			new Request()

					.reply(response -> response

							.status(Response.OK)
							.header("Content-Type", "multipart/form-data; boundary=1234567890")
							.body(multipart(), map())

					)

					.accept(response -> MessageAssert.assertThat(response)

							.hasHeader("Content-Type", "multipart/form-data; boundary=1234567890")

							.hasBody(output(), target -> {

								final ByteArrayOutputStream output=new ByteArrayOutputStream();

								target.accept(output);

								assertThat(new String(output.toByteArray(), UTF_8))
										.contains("--1234567890--");

							})

					);
		}

	}

}
