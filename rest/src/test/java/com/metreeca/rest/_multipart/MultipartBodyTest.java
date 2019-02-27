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

package com.metreeca.rest._multipart;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.function.Supplier;

import static com.metreeca.form.things.Values.iri;
import static com.metreeca.rest.Body.Missing;
import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest._multipart.MultipartBody.multipart;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.ReaderBody.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


final class MultipartBodyTest {

	@Nested final class Input {

		private final String type="multipart/form-data; boundary=\"boundary\"";

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


		// !!! main merging
		// !!! main part rewriting

	}

	@Nested final class Output {

	}

}
