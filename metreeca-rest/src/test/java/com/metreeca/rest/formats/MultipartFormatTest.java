/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.MultipartFormat.multipart;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


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
										.hasItem("file:example.txt")
										.hasHeader("Content-Disposition")
										.hasBody(text(), text -> assertThat(text)
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
							Assertions::assertThat
					);
		}

		@Test void testRejectMalformedPayloads() {
			new Request()

					.header("Content-Type", "multipart/data")
					.body(input(), content())

					.body(multipart())

					.fold(

							parts -> {
								fail("unexpected multipart body");


								return this;

							},

							error -> {

								new Request().reply(error).accept(response -> assertThat(response)
										.as("missing boundary parameter")
										.hasStatus(Response.BadRequest)
								);


								return this;
							}
					);
		}

	}

	@Nested final class Output {

		@SafeVarargs private final Map<String, Message<?>> map(final Map.Entry<String, Message<?>>... entries) {
			return Arrays.stream(entries).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
		}

		private Map.Entry<String, Message<?>> entry(final String item, final Message<?> part) {
			return new AbstractMap.SimpleImmutableEntry<>(item, part);
		}


		@Test void testGenerateRandomBoundary() {
			new Request().reply(response -> response

					.status(Response.OK)
					.body(multipart(), map(
							entry("one", response.link("one").body(text(), "one")),
							entry("two", response.link("two").body(text(), "two"))
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
