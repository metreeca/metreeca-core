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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.TextFormat.text;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class MultipartParserTest {

	private InputStream content(final String content) {
		return new ByteArrayInputStream(content.replace("\n", "\r\n").getBytes(UTF_8));
	}

	private Collection<Message<?>> parts(final String content) throws IOException {

		final Map<String, Message<?>> parts=new LinkedHashMap<>();

		new MultipartParser(1000, 1000, content(content), "boundary", (headers, body) -> {

			final Map<String, List<String>> map=headers.stream().collect(groupingBy(
					Entry::getKey,
					LinkedHashMap::new,
					mapping(Entry::getValue, toList())
			));

			parts.put("part"+parts.size(), new MessageMock()
					.headers(map)
					.body(input(), () -> body)
			);

		}).parse();

		return parts.values();
	}


	@Nested final class Splitting {

		private List<String> parts(final String content) throws IOException {
			return MultipartParserTest.this.parts(content).stream()
					.map(message -> message.body(text()).fold(e -> "", identity()))
					.collect(toList());
		}

		@Test void testIgnoreFrame() throws IOException {

			assertThat(parts("")).isEmpty();

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

			assertThat(parts("--boundary\n\ncontent\n--boundary--\n trailing\ngarbage"))
					.as("ignored")
					.containsExactly("content");

		}

	}

	@Nested final class Headers {

		@Test void testParseHeaders() throws IOException {
			assertThat(parts("--boundary\nsingle: value\nmultiple: one\nmultiple: two\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("single", "value")
							.hasHeaders("multiple", "one", "two")
							.hasBody(text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEmptyHeaders() throws IOException {
			assertThat(parts("--boundary\nempty:\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("empty")
							.hasBody(text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEOFInHeaders() throws IOException {
			assertThat(parts("--boundary\nsingle: value"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> MessageAssert.assertThat(message)
							.hasHeaders("single", "value")
					);
		}

		@Test void testRejectMalformedHeaders() {

			assertThatExceptionOfType(MessageException.class)
					.as("spaces before colon")
					.isThrownBy(() -> parts("--boundary\nheader : value\n\ncontent"));

			assertThatExceptionOfType(MessageException.class)
					.as("malformed name")
					.isThrownBy(() -> parts("--boundary\nhea der: value\n\ncontent"));

			assertThatExceptionOfType(MessageException.class)
					.as("malformed value")
					.isThrownBy(() -> parts("--boundary\nhea der: val\rue\n\ncontent"));

		}

	}

	@Nested final class Limits {

		private MultipartParser parser(final int part, final int body, final String content) {
			return new MultipartParser(part, body, content(content), "boundary", (headers, _body) -> {});
		}


		@Test void testEnforceBodySizeLimits() {

			assertThatExceptionOfType(MessageException.class)
					.as("body size exceeded")
					.isThrownBy(() -> parser(5, 25,
							"--boundary\n\none\n--boundary\n\ntwo\n--boundary--"
					).parse());

		}

		@Test void testEnforcePartSizeLimits() {

			assertThatExceptionOfType(MessageException.class)
					.as("parts size exceeded")
					.isThrownBy(() -> parser(10, 1000,
							"--boundary\n\nshort\n--boundary\n\nlong content\n--boundary--"
					).parse());

		}

	}

}
