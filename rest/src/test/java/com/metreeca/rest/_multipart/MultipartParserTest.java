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
import com.metreeca.rest.MessageTest.TestMessage;
import com.metreeca.rest.bodies.InputBody;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.bodies.TextBody.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;


final class MultipartParserTest {

	private Collection<Message<?>> parts(final String content) throws IOException, ParseException {

		final Map<String, Message<?>> parts=new LinkedHashMap<>();

		new MultipartParser(
				new ByteArrayInputStream(content.replace("\n", "\r\n").getBytes(UTF8)),
				"boundary",
				(headers, body) -> {

					final Map<String, List<String>> map=headers.stream().collect(groupingBy(
							Map.Entry::getKey,
							LinkedHashMap::new,
							mapping(Map.Entry::getValue, toList())
					));

					parts.put("part"+parts.size(), new TestMessage()
							.headers(map)
							.body(InputBody.input(), () -> body)
					);
				}
		).parse();

		return parts.values();
	}


	@Nested final class Splitting {

		private List<String> parts(final String content) throws IOException, ParseException {
			return MultipartParserTest.this.parts(content).stream()
					.map(message -> message.body(text()).value().orElse(""))
					.collect(toList());
		}

		@Test void testIgnoreFrame() throws IOException, ParseException {

			assertThat(parts("")).isEmpty();

			assertThat(parts("preamble\n--boundary--")).isEmpty();
			assertThat(parts("--boundary--\nepilogue")).isEmpty();

		}

		@Test void testSplitParts() throws IOException, ParseException {

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

		@Test void testIgnorePreamble()  throws IOException, ParseException {

			assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			assertThat(parts("preamble\n--boundary\n\ncontent\n--boundary--"))
					.as("ignored")
					.containsExactly("content");

		}

		@Test void testIgnoreEpilogue()  throws IOException, ParseException {

			assertThat(parts("--boundary\n\ncontent\n--boundary--"))
					.as("missing")
					.containsExactly("content");

			assertThat(parts("--boundary\n\ncontent\n--boundary--\n trailing\ngarbage"))
					.as("ignored")
					.containsExactly("content");

		}

	}

	@Nested final class Headers {

		@Test void testParseHeaders() throws IOException, ParseException {
			assertThat(parts("--boundary\nsingle: value\nmultiple: one\nmultiple: two\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> assertThat(message)
							.hasHeaders("single", "value")
							.hasHeaders("multiple", "one", "two")
							.hasBody(text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEmptyHeaders() throws IOException, ParseException {
			assertThat(parts("--boundary\nempty:\n\ncontent"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> assertThat(message)
							.hasHeaders("empty")
							.hasBody(text(), text -> assertThat(text).isEqualTo("content"))
					);
		}

		@Test void testHandleEOFInHeaders() throws IOException, ParseException {
			assertThat(parts("--boundary\nsingle: value"))
					.isNotEmpty()
					.hasOnlyOneElementSatisfying(message -> assertThat(message)
							.hasHeaders("single", "value")
					);
		}

		@Test void testRejectMalformedHeaders() {

			assertThatExceptionOfType(ParseException.class)
					.as("spaces before colon")
					.isThrownBy(() -> parts("--boundary\nheader : value\n\ncontent"));

			assertThatExceptionOfType(ParseException.class)
					.as("malformed name")
					.isThrownBy(() -> parts("--boundary\nhea der: value\n\ncontent"));

			assertThatExceptionOfType(ParseException.class)
					.as("malformed value")
					.isThrownBy(() -> parts("--boundary\nhea der: val\rue\n\ncontent"));

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
