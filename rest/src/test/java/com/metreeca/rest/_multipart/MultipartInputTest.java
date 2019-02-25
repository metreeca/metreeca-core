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

import com.metreeca.rest.Message;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.List;

import static com.metreeca.form.things.Codecs.input;
import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.bodies.TextBody.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.stream.Collectors.toList;


final class MultipartInputTest {

	private Collection<Message<?>> parts(final String content) throws IOException {
		return new MultipartInput(
				new Request(),
				input(new StringReader(content.replace("\n", "\r\n"))),
				"boundary"
		).parse().values();
	}


	@Nested final class Splitting {

		private List<String> parts(final String content) throws IOException {
			return MultipartInputTest.this.parts(content).stream()
					.map(message -> message.body(text()).value().orElse(""))
					.collect(toList());
		}


		@Test void testIgnoreFrame() throws IOException {

			assertThat(parts("")).isEmpty();
			assertThat(parts("--boundary--")).isEmpty();

			assertThat(parts("preamble\n--boundary--")).isEmpty();
			assertThat(parts("--boundary--\nepilogue")).isEmpty();

		}

		@Test void testSplitParts() throws IOException {

			assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("canonical")
					.containsExactly("content");

			assertThat(parts("--boundary\n\ncontent\n\n\n--boundary--"))
					.as("trailing newlines")
					.containsExactly("content\r\n\r\n");

			assertThat(parts("--boundary\n\ncontent"))
					.as("lenient missing closing boundary")
					.containsExactly("content");

			assertThat(parts("--boundary\n\none\n--boundary\n\ntwo\n--boundary--"))
					.as("multiple parts")
					.containsExactly("one", "two");

		}

		@Test void testIgnorePreamble() throws IOException {

			assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			assertThat(parts("preamble\n--boundary\n\ncontent\n--boundary--"))
					.as("ignored")
					.containsExactly("content");

		}

		@Test void testIgnoreEpilogue() throws IOException {

			assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			assertThat(parts("--boundary\n\ncontent\n--boundary-- trailing\ngarbage"))
					.as("ignored")
					.containsExactly("content");

		}

	}

	@Nested final class Headers {

		@Test void testParseHeaders() throws IOException {
			assertThat(parts("--boundary\nsingle: value\nmultiple: one\nmultiple: two\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> assertThat(message)
							.hasHeaders("single", "value")
							.hasHeaders("multiple", "one", "two")
							.hasBody(text(), text -> assertThat(text).isEqualTo("content"))
					);
		}


		@Test void testHandleEOFInHeader() {

			assertThatExceptionOfType(IllegalStateException.class)
					.as("in name")
					.isThrownBy(() -> parts("--boundary\nheader"));

			assertThatExceptionOfType(IllegalStateException.class)
					.as("in value")
					.isThrownBy(() -> parts("--boundary\nheader: value"));
		}

		@Test void testRejectMalformedHeaders() {
			assertThatExceptionOfType(IllegalStateException.class)
					.isThrownBy(() -> parts("--boundary\nheader : value\n\ncontent"));
		}

	}

	@Nested final class Limits {

		@Test void testEnforceSizeLimits() {

		}

		@Test void testEnforceCountLimits() {

		}

		@Test void testEnforceHeaderLimits() {

		}

	}

}
